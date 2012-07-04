package rede;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class Socket {
	//Socket do lado client
	private int server_port; //Usado tambem do lado server;
	private InetAddress server_adress;

	//Socket do lado servidor;
	private int client_port; //Usado tambem do lado cliente;
	private InetAddress client_adress;

	//Socket por onde as informações iram passar
	private DatagramSocket real_socket;

	//dados para controle de envio
	private AtomicInteger send_base = new AtomicInteger(0); //base da janela de congestionamento
	private AtomicInteger nextseqnum = new AtomicInteger(0); //proximo numero de sequencia
	private AtomicInteger cwin = new AtomicInteger(1); //janela de congestionamento
	private AtomicInteger ssthresh = new AtomicInteger(64); //limiar de partidade lenta
	private AtomicInteger quantos_zerou = new AtomicInteger(0);
	private AtomicInteger restam_prox_cwin = new AtomicInteger(1);	
	private AtomicLong timeout = new AtomicLong(1000);

	private int max_win = 16;

	AtomicLong temp_SampleRTT = new AtomicLong(0);
	AtomicLong last_send = new AtomicLong(0); //valor do ultimo byte que se tem certeza que foi recebido pelo cliente

	private long min_timeout = 1000;
	private long EstimatedRTT = 1000;
	private long DevRTT = 20;

	private long update_timers_rate = 250;

	private AtomicInteger send_packets_cont = new AtomicInteger();

	AtomicBoolean timer_run = new AtomicBoolean(false);

	//dados para controle de recepção
	AtomicInteger rcv_base = new AtomicInteger(0); //base inicial de recepção
	AtomicInteger rwin = new AtomicInteger(0); //janela de recepção

	AtomicBoolean close = new AtomicBoolean(false); //booleano com condição de parada das threads

	AtomicLong velocidade = new AtomicLong(0);

	//Objetos para sincronização
	Object buffer_cliente = new Object();
	Object buffer_server = new Object();
	Object sinc_send_buffer = new Object(); //objeto para sincroniza de escrita e leitura do buffer de pacotes enviados
	Object sinc_send_socket = new Object();
	Object sinc_var_timeout = new Object(); //mutex para operações que envolvem mechar com váriavies que estão no calculo de timeout
	//Objetos para sincronização

	//Buffers internos para Pacotes
	ArrayList<Pacote> send_packet_buffer = new ArrayList<Pacote>();
	HashMap<Integer, byte[]> rec_packet_buffer = new HashMap<Integer,byte[]>(); //chave vai ser numero de sequencia
	//Buffers internos para Pacotes

	//Buffers internos para Dados
	private int buffer_size = 20971520;	
	ByteArrayOutputStream server = new ByteArrayOutputStream();
	ByteArrayOutputStream cliente = new ByteArrayOutputStream(buffer_size);
	ByteArrayInputStream internal;
	ByteArrayInputStream to_cliente;
	//buffers internos para Dados

	//Metodos para leitura e escrita nos buffers

	AtomicInteger remain_buffer_space = new AtomicInteger(buffer_size);

	public void write(byte[] data,int off, int len) throws IOException{
		synchronized (buffer_server) {
			server.write(data,off,len);
			remain_buffer_space.set(remain_buffer_space.get()-len);
		}
	}

	protected int read_internal(byte[] data,int off, int len) throws IOException{
		if(internal==null || internal.available()<=0){
			synchronized (buffer_server) {
				internal = new ByteArrayInputStream(server.toByteArray());
				server.reset();
				remain_buffer_space.set(buffer_size);
			}
		}
		return internal.read(data,off,len);
	}

	protected void write_internal(byte[] data,int off,int len) throws IOException{
		synchronized (buffer_cliente) {
			cliente.write(data,off,len);
		}
	}

	public int read(byte[] data,int off, int len) throws IOException{
		if(to_cliente==null || to_cliente.available()<=0){
			synchronized (buffer_cliente) {
				to_cliente = new ByteArrayInputStream(cliente.toByteArray());
				cliente.reset();
			}
		}
		return to_cliente.read(data,off,len);
	}

	public int buffer_avaliable(){
		return remain_buffer_space.get();
	}
	//Metodos para leitura e escrita nos buffers

	/**
	 * Retorna a quantidade de bytes de foi enviada ao destinário com sucesso, use reset para limpar esse valor
	 * @return long - byes entregues com sucesso
	 * **/
	public long get_last_send(){
		return this.last_send.get();
	}


	/**
	 * Reseta o valor de bytes enviados para zero
	 *
	 */
	public void reset(){
		this.last_send.set(0);
	}


	//Server
	public Socket(int port) throws IOException{
		this.server_adress = InetAddress.getByName("localhost");
		this.server_port = port;
		this.real_socket = new DatagramSocket(port);
		DatagramPacket temp  = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		real_socket.receive(temp);
		this.client_adress = temp.getAddress();
		this.client_port = temp.getPort();

		this.send_base.set(0);
		this.nextseqnum.set(0);

		Sender sender = new Sender();
		Receiver receiver = new Receiver();
		Thread sender_t = new Thread(sender);
		sender_t.setName("Sender");
		Thread receiver_t = new Thread(receiver);
		receiver_t.setName("Receiver");
		sender_t.start();
		receiver_t.start();
		new Timer().scheduleAtFixedRate(new UpdateTimers(),0, update_timers_rate);

	}

	//Cliente
	public Socket(int server_port, InetAddress server) throws IOException{
		this.server_adress = server;
		this.server_port = server_port;
		this.real_socket = new DatagramSocket();
		DatagramPacket temp  = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload,this.server_adress,this.server_port);
		real_socket.send(temp);
		this.client_adress = real_socket.getLocalAddress();
		this.client_port = real_socket.getLocalPort();

		new Thread(new Receiver()).start();
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
	}

	//classes internas
	private class Sender extends Thread{

		@Override
		public void run() {
			while(!close.get()){
				if(nextseqnum.get()<=(send_base.get()+cwin.get())){//verifico se existe espaço dentro da janela para criar um pacote
					byte[] to_packet = new byte[Pacote.default_size]; //array de bytes com dados lidos
					int as_read = 0; //inteiro para ver quantos bytes foram lidos
					try {
						as_read = read_internal(to_packet, Pacote.head_payload, Pacote.util_load);							
					} catch (IOException e) {
						System.out.println("Problemas com buffers internos");
						System.out.println("Encerrando conexão...");
						close.set(true);
					}

					if(as_read>0){
						//verificar a possibilidade de ter de colocar a janela de recepção caso ative full duplex
						OperacoesBinarias.inserirCabecalho(to_packet, nextseqnum.get(), 0, false, false, false, false, as_read, 0); //inserção de cabeçalh
						DatagramPacket packet = new DatagramPacket(to_packet, Pacote.default_size,client_adress,client_port);
						Pacote pacote = new Pacote(packet,System.currentTimeMillis()); //Cria pacote de buffer

						synchronized (sinc_send_buffer) {
							send_packet_buffer.add(pacote);
						}

						//						System.out.println("New seq "+nextseqnum.incrementAndGet());//incrementa o número de sequencia
						nextseqnum.incrementAndGet();

						synchronized (sinc_send_socket) { //adquire reserva do socket para enviar pacote
							try {
								real_socket.send(packet);
								if(!timer_run.get()){//se nenhum temporizador esta ativo ativa um
									timer_run.set(true);
									new Thread(new Timeout()).start();
								}
							} catch (IOException e) {
								System.out.println("Não foi possível enviar um pacote, problema no Socket\nEncerrando conexão");
								close.set(true);
							} //envia pacote
						}
					}
				}else{
					if(!timer_run.get()){//se nenhum temporizador esta ativo ativa um
						timer_run.set(true);
						new Thread(new Timeout()).start();
					}
				}
			}
		}
	}

	private class Receiver extends Thread{

		@Override
		public void run() {
			while(!close.get()){ 
				byte[] buffer = new byte[Pacote.default_size];
				DatagramPacket packet = new DatagramPacket(buffer, Pacote.default_size);

				synchronized (sinc_send_buffer) {

					while(!send_packet_buffer.isEmpty() && send_packet_buffer.get(0).isEnviado()){
						send_base.incrementAndGet();//incrementa o valor send_base
						send_packet_buffer.remove(0); //remove o pacore do buffer
						send_packets_cont.incrementAndGet();
					}
				}

				try {
					real_socket.receive(packet); //recebe um pacote

					int key = OperacoesBinarias.extrairNumeroReconhecimento(buffer);
					int seqNum = OperacoesBinarias.extrairNumeroSequencia(buffer);
					int dataLength = OperacoesBinarias.extrairComprimentoDados(buffer);

					if(OperacoesBinarias.extrairFIN(buffer)){

					}else if(OperacoesBinarias.extrairSYN(buffer)){

					}else if(OperacoesBinarias.extrairACK(buffer)){

						if(packet.getAddress().equals(client_adress) && packet.getPort()==client_port){ //tenho um ack de quem me comunico

							Pacote temp;

							synchronized (sinc_send_buffer) {

								int remap = key-send_packets_cont.get();

								if((remap)<send_packet_buffer.size() && !send_packet_buffer.isEmpty() && remap>=0){ //verifica se o pacote esta dentro das possibilidades do buffer

									temp = send_packet_buffer.get(remap);

									temp_SampleRTT.set(System.currentTimeMillis()-temp.send_time); //atualiza variavel com sampleRTT temporário

									if(!temp.isEnviado()){
										temp.setEnviado(true);
										cwin.set(Math.min(cwin.get()+1, max_win));
									}
								}
							}
						}else{
							System.out.println("Recebi um ACK de um host estranho. Cuidado a rede pode estar sendo invadida! ^~^");
						}

					}else if(OperacoesBinarias.extrairRST(buffer)){

					}else{ //temos um pacote com dados

						if(packet.getAddress().equals(server_adress) && packet.getPort()==server_port){ //tenho um ack de quem me comunico
							byte[] to_ack = new byte[Pacote.head_payload];
							OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, rcv_base.get()+rwin.get());
							DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,server_adress,server_port);
							synchronized (sinc_send_socket) {
								real_socket.send(ack);
							}

							if(seqNum==rcv_base.get()){ //se temos o proximo pacote esperado
								write_internal(buffer, Pacote.head_payload, dataLength); //escreve para camada de cima
								rcv_base.incrementAndGet();//incrementa a base da janela
								velocidade.getAndAdd(dataLength);

								//tenta pegar mais pacotes que possa estar no buffer de recepção
								while(rec_packet_buffer.get(rcv_base.get())!=null){
									byte[] dados = rec_packet_buffer.get(rcv_base.get());
									dataLength = OperacoesBinarias.extrairComprimentoDados(dados);
									velocidade.getAndAdd(dataLength);
									seqNum = OperacoesBinarias.extrairNumeroSequencia(dados);
									rcv_base.incrementAndGet(); //incrementa a base de recepção para o proximo pacote
									write_internal(dados, Pacote.head_payload,dataLength);
								}
								
								//Teste importante para ver se o SVN esta rodando direito
							}else if(seqNum>rcv_base.get()){ //se não temos o proximo pacote esperado
								//coloca ele no buffer
								rec_packet_buffer.put(seqNum, buffer);
							}else{
								System.out.println("Retransmissao");
							}
						}
					}
				} catch (IOException e) {
					System.out.println("Deu merda no Socket do receiver fechando conexão...");
					close.set(true);
				}
			}
		}

	}

	private class Timeout implements Runnable{
		protected boolean continua = true;
		protected int timeouts;

		@Override
		public void run() {

			while(continua){
				try {
					Thread.sleep(Math.max(timeout.get(),min_timeout)); //thread dorme para simular timeout
				} catch (InterruptedException e) {
					System.out.println("Erro no timeout");
				}

				if(System.currentTimeMillis()-send_packet_buffer.get(0).send_time>(min_timeout/2)){
					if(!send_packet_buffer.isEmpty()){
						if(!send_packet_buffer.get(0).isEnviado()){

							if(timeouts%2==0){
								ssthresh.set((cwin.get()/2)+10); // limiar de envio e setado pela metade
								cwin.set(1); //janela de congestionamento e setada para 1MSS
								quantos_zerou.set(0);
								restam_prox_cwin.set(1);
							}

							synchronized (sinc_send_socket) {
								try {
									int indice = 0;
									while(indice<send_packet_buffer.size() && !send_packet_buffer.isEmpty() && !send_packet_buffer.get(indice).isEnviado()){
										real_socket.send(send_packet_buffer.get(indice).setSend_time_and_getme(System.currentTimeMillis()).pkt);
										indice++;
									}
								} catch (IOException e) {
									System.out.println("Problema com o socket de envio");
									System.out.println("Fechando conexão");
									close.set(true);
									continua = false;
								}
							}
						}

					}else{
						continua=false;
					}
				}
			}

			timer_run.set(false);
			System.out.println("Encerrado temporizado...");
		}
	}

	private class UpdateTimers extends TimerTask{

		@Override
		public void run() {
			EstimatedRTT = (long) ((EstimatedRTT*0.875)+(0.125*temp_SampleRTT.get()));
			DevRTT = (long) ((0.75*DevRTT)+(0.25*Math.abs(temp_SampleRTT.get()-EstimatedRTT)));
			timeout.set(EstimatedRTT+(4*DevRTT));
		}

	}

	private class Bandwidth extends TimerTask{

		double repVelo = 0;

		@Override
		public void run() {
			repVelo = (repVelo * 0.825) + ((velocidade.getAndSet(0) / 1024)*0.175);

			System.out.println((int) repVelo + " Kb/s");

		}

	}
}
