package rede;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


public class Socket{
	//Socket do lado client
	private int server_port; //Usado tambem do lado server;
	private InetAddress server_adress;

	//Socket do lado servidor;
	private int client_port; //Usado tambem do lado cliente;
	private InetAddress client_adress;
	
	private int local_port;
	private InetAddress local_adress;
	
	protected DatagramSocket real_socket;

	private boolean is_server = false;	
	private boolean is_connected = false;

	//dados para controle de envio
	private AtomicInteger send_base = new AtomicInteger(0); //base da janela de congestionamento
	private AtomicInteger nextseqnum = new AtomicInteger(0); //proximo numero de sequencia
	private AtomicInteger cwin = new AtomicInteger(10); //janela de congestionamento
	private AtomicInteger ssthresh = new AtomicInteger(128); //limiar de partidade lenta
	private AtomicInteger quantos_zerou = new AtomicInteger(0);
	private AtomicInteger restam_prox_cwin = new AtomicInteger(1);	
	private AtomicLong timeout = new AtomicLong(1000);
	private AtomicBoolean estimateRTT_process = new AtomicBoolean(); //variavel para detectar que se esta estimando um RTT
	private AtomicInteger estimateRTT_for_packet = new AtomicInteger(0); //numero do pacote para o qual se esta estimando o RTT

	private int max_win = 256;

	AtomicLong temp_SampleRTT = new AtomicLong(0);
	AtomicLong last_send = new AtomicLong(0); //valor do ultimo byte que se tem certeza que foi recebido pelo cliente

	private long min_timeout = 400;
	private long EstimatedRTT = 500;
	private long DevRTT = 20;

	private AtomicInteger send_packets_cont = new AtomicInteger();

	AtomicBoolean timer_run = new AtomicBoolean(false);

	//dados para controle de recepção
	AtomicInteger rcv_base = new AtomicInteger(0); //base inicial de recepção
	AtomicInteger rwin = new AtomicInteger(0); //janela de recepção

	AtomicBoolean close = new AtomicBoolean(false); //booleano com condição de parada das threads
	AtomicBoolean connect = new AtomicBoolean(false);

	AtomicLong velocidade = new AtomicLong(0);

	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};

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

	public void close(){
		synchronized (sinc_send_socket) {
			byte[] to_fin = new byte[Pacote.head_payload];
			OperacoesBinarias.inserirCabecalho(to_fin, 0, 0, false, false, false, true, 0, 0);
			DatagramPacket packet;
			if(is_server){
				packet = new DatagramPacket(to_fin, Pacote.head_payload,client_adress,client_port);
			}else{
				packet = new DatagramPacket(to_fin, Pacote.head_payload,server_adress,server_port);	
			}

			try {
				for (int i = 0; i < max_win; i++) {
					real_socket.send(packet);
				}
			} catch (IOException e) {
				System.out.println("Problema com socket interno");
				System.out.println("Fechando conexão...");
				close.set(true);
			}
		}
		close.set(true);
		real_socket.close();
	}

	public boolean isConnected(){
		return is_connected;
	}

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

	//usado pelo cliente para indicar criar um socket para o servidor
	public Socket(int porta_servidor, InetAddress endereco_servidor) throws IOException {
		real_socket = new DatagramSocket();
		this.client_port = this.server_port = porta_servidor;
		this.client_adress = this.server_adress = endereco_servidor;
		this.local_adress = real_socket.getLocalAddress();
		this.local_port = real_socket.getLocalPort();
		DatagramPacket receiver = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		while(!connect.get()){
			real_socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			real_socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			real_socket.receive(receiver);
			if(receiver.getAddress().equals(endereco_servidor) && receiver.getPort()==porta_servidor){
				connect.set(true);
			}
		}

		new Thread(new Receiver()).start();
		new Thread(new Sender()).start();
//		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
	}

	//usado pelo servidor para ficar escutando na porta especifica
	public Socket(int port) throws IOException {
		real_socket = new DatagramSocket(port);
	}

	public void setCliente(int portaCliente, InetAddress enderecoCliente){
		this.server_adress = this.client_adress = enderecoCliente;
		this.server_port = this.client_port = portaCliente;
		this.local_adress = real_socket.getLocalAddress();
		this.local_port = real_socket.getLocalPort();
		new Thread(new Receiver()).start();
		new Thread(new Sender()).start();
//		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
		
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

						OperacoesBinarias.inserirCabecalho(to_packet, nextseqnum.get(), 0, false, false, false, false, as_read, 0); //inserção de cabeçalh
						DatagramPacket packet = new DatagramPacket(to_packet, Pacote.default_size,client_adress,client_port);
						Pacote pacote = new Pacote(packet,System.currentTimeMillis(),as_read); //Cria pacote de buffer

						synchronized (sinc_send_buffer) {
							send_packet_buffer.add(pacote);
							if(!estimateRTT_process.get()){ //Se nenhum estimados de RTT estÃ¡ em progresso inicia um
								estimateRTT_for_packet.set(nextseqnum.get()); //estimamos o valor para 
							}
						}

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

				synchronized (sinc_send_buffer) {

					while(!send_packet_buffer.isEmpty() && send_packet_buffer.get(0).isEnviado()){
						last_send.addAndGet(send_packet_buffer.get(0).dataLenth);
						send_base.incrementAndGet();//incrementa o valor send_base
						send_packet_buffer.remove(0); //remove o pacore do buffer
						send_packets_cont.incrementAndGet();
					}
				}
			}
			System.out.println("Sender encerrando...");
		}
	}

	private class Receiver extends Thread{

		@Override
		public void run() {
			while(!close.get()){ 
				byte[] buffer = new byte[Pacote.default_size];
				DatagramPacket packet = new DatagramPacket(buffer, Pacote.default_size);

				try {
					real_socket.receive(packet); //recebe um pacote

					int key = OperacoesBinarias.extrairNumeroReconhecimento(buffer);
					int seqNum = OperacoesBinarias.extrairNumeroSequencia(buffer);
					int dataLength = OperacoesBinarias.extrairComprimentoDados(buffer);

					if(OperacoesBinarias.extrairFIN(buffer)){
						close.set(true);
						Socket.this.close();
					}else if(OperacoesBinarias.extrairSYN(buffer)){

					}else if(OperacoesBinarias.extrairACK(buffer)){

						if(packet.getAddress().equals(local_adress) && packet.getPort()==local_port){ //tenho um ack de quem me comunico

							Pacote temp = null;

							synchronized (sinc_send_buffer) {

								int remap = key-send_packets_cont.get();

								if((remap)<send_packet_buffer.size() && !send_packet_buffer.isEmpty() && remap>=0){ //verifica se o pacote esta dentro das possibilidades do buffer

									temp = send_packet_buffer.get(remap);


									temp_SampleRTT.set(System.currentTimeMillis()-temp.send_time); //atualiza variavel com sampleRTT temporário


									if(!temp.isEnviado()){
										temp.setEnviado(true);
										cwin.set(Math.min(cwin.get()+10, max_win));
									}
								}
							}

							if(key>estimateRTT_for_packet.get() && estimateRTT_process.get() && temp!=null){
								long temp_SampleRTT = System.currentTimeMillis() - temp.send_time;//Calculo sampleRTT
								EstimatedRTT = (long) ((EstimatedRTT*0.875)+(0.125*temp_SampleRTT)); //Use para Estimar o proximoRTT
								DevRTT = (long) ((0.75*DevRTT)+(0.25*Math.abs(temp_SampleRTT-EstimatedRTT))); //E tambÃ©m o DevRTT
								timeout.set(EstimatedRTT+(4*DevRTT));//E altere o timeout
								estimateRTT_process.set(false);
							}

						}else{
							System.out.println("Recebi um ACK de um host estranho. Cuidado a rede pode estar sendo invadida! ^~^");
						}

					}else if(OperacoesBinarias.extrairRST(buffer)){

					}else{ //temos um pacote com dados

						if(packet.getAddress().equals(local_adress) && packet.getPort()==local_port){ //tenho um ack de quem me comunico
							//cria ack em resposta ao dado recebido
							byte[] to_ack = new byte[Pacote.head_payload];
							OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, rcv_base.get()+rwin.get());
							DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,server_adress,server_port);
							synchronized (sinc_send_socket) { //envia ack
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
									seqNum = OperacoesBinarias.extrairNumeroSequencia(dados);
									velocidade.getAndAdd(dataLength);
									rcv_base.incrementAndGet(); //incrementa a base de recepção para o proximo pacote
									write_internal(dados, Pacote.head_payload,dataLength);
								}

							}else if(seqNum>rcv_base.get()){ //se não temos o proximo pacote esperado
								//coloca ele no buffer
								rec_packet_buffer.put(seqNum, buffer);
							}else{
								System.out.println("Retransmissao");
							}
						}else{
							System.out.println("MERDAAAAAAAAAAAAA");
							System.out.println(packet.getAddress());
							System.out.println(packet.getPort());
							System.out.println(local_adress);
							System.out.println(local_port);
						}
					}
				} catch (IOException e) {
					System.out.println("Deu merda no Socket do receiver fechando conexão...");
					close.set(true);
				}
			}
			System.out.println("Receiver Encerrado...");
		}

	}

	private class Timeout implements Runnable{
		protected boolean continua = true;
		protected int timeouts;

		@Override
		public void run() {

			while(continua && !close.get()){

				try {
					Thread.sleep(Math.max(timeout.get(),min_timeout)); //thread dorme para simular timeout
					//					Thread.sleep(200);
				} catch (InterruptedException e) {
					System.out.println("Erro no timeout");
				}


				if(!send_packet_buffer.isEmpty()){
					if(System.currentTimeMillis()-send_packet_buffer.get(0).send_time>(min_timeout/2)){

						if(!send_packet_buffer.get(0).isEnviado()){

							if(timeouts%3==0){
								ssthresh.set((cwin.get()/2)+10); // limiar de envio e setado pela metade
								cwin.set(cwin.get()/2); //janela de congestionamento e setada para 1MSS
								quantos_zerou.set(0);
								restam_prox_cwin.set(1);
							}
							timeouts++;

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

					}
				}else{
					continua=false;
				}
			}

			timer_run.set(false);
			System.out.println("Encerrado temporizado...");
		}
	}

	private class Bandwidth extends TimerTask{

		double repVelo = 0;

		@Override
		public void run() {
			repVelo = (repVelo * 0.825) + ((velocidade.getAndSet(0) / 1024)*0.175);

			System.out.println((int) repVelo + " Kb/s");

			if(close.get()){
				this.cancel();
			}
		}
	}

	private class Transfered extends TimerTask{
		@Override
		public void run() {
			System.out.println("Bytes transferidos com sucesso: "+ last_send.get());

			if(close.get()){
				this.cancel();
			}
		}
	}
}
