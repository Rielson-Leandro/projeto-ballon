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


public class Socket extends DatagramSocket{
	//Socket do lado client
	private int server_port; //Usado tambem do lado server;
	private InetAddress server_adress;

	//Socket do lado servidor;
	private int client_port; //Usado tambem do lado cliente;
	private InetAddress client_adress;

	private boolean is_server = false;	
	private boolean is_connected = false;

	//dados para controle de envio
	private AtomicInteger send_base = new AtomicInteger(0); //base da janela de congestionamento
	private AtomicInteger nextseqnum = new AtomicInteger(0); //proximo numero de sequencia
	private AtomicInteger cwin = new AtomicInteger(1); //janela de congestionamento
	private AtomicInteger ssthresh = new AtomicInteger(128); //limiar de partidade lenta
	private AtomicInteger quantos_zerou = new AtomicInteger(0);
	private AtomicInteger restam_prox_cwin = new AtomicInteger(1);	
	private AtomicLong timeout = new AtomicLong(1000);
	private AtomicBoolean estimateRTT_process = new AtomicBoolean(); //variavel para detectar que se esta estimando um RTT
	private AtomicInteger estimateRTT_for_packet = new AtomicInteger(0); //numero do pacote para o qual se esta estimando o RTT

	private int max_win = 256;

	AtomicLong temp_SampleRTT = new AtomicLong(0);
	AtomicLong last_send = new AtomicLong(0); //valor do ultimo byte que se tem certeza que foi recebido pelo cliente

	private long min_timeout = 750;
	private long EstimatedRTT = 1000;
	private long DevRTT = 20;

	private AtomicInteger send_packets_cont = new AtomicInteger();

	AtomicBoolean timer_run = new AtomicBoolean(false);

	//dados para controle de recep��o
	AtomicInteger rcv_base = new AtomicInteger(0); //base inicial de recep��o
	AtomicInteger rwin = new AtomicInteger(0); //janela de recep��o

	AtomicBoolean close = new AtomicBoolean(false); //booleano com condi��o de parada das threads

	AtomicLong velocidade = new AtomicLong(0);

	//Objetos para sincroniza��o
	Object buffer_cliente = new Object();
	Object buffer_server = new Object();
	Object sinc_send_buffer = new Object(); //objeto para sincroniza de escrita e leitura do buffer de pacotes enviados
	Object sinc_send_socket = new Object();
	Object sinc_var_timeout = new Object(); //mutex para opera��es que envolvem mechar com v�riavies que est�o no calculo de timeout
	//Objetos para sincroniza��o

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
					send(packet);
				}
			} catch (IOException e) {
				System.out.println("Problema com socket interno");
				System.out.println("Fechando conex�o...");
				close.set(true);
			}
		}
		close.set(true);
		super.close();
	}

	protected void internal_close(){
		super.close();
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
	 * Retorna a quantidade de bytes de foi enviada ao destin�rio com sucesso, use reset para limpar esse valor
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

	//Contrutores reais
	public Socket(InetAddress address, int port) throws IOException{
		super();
		this.is_server = false;
		boolean done = false;
		do{
			//Enviar solicita��o ao ServerSocket
			byte[] to_conect = new byte[Pacote.head_payload];
			OperacoesBinarias.inserirCabecalho(to_conect, 0, 0, false, false, true, false, 0, 0);
			DatagramPacket packet = new DatagramPacket(to_conect, to_conect.length,address,port);
			this.send(packet); //envia pacote para o servidor

			//receber mensagem do socket especial de servidor criado para comunical�ao
			DatagramPacket packet2  = new DatagramPacket(new byte[Pacote.default_size], Pacote.default_size);
			this.receive(packet2);
			byte[] temp = packet2.getData();

			if(OperacoesBinarias.extrairSYN(temp) && OperacoesBinarias.extrairACK(temp)){
				done  = true;
				this.server_adress = packet2.getAddress();
				this.server_port = packet2.getPort();
				this.client_adress = this.getLocalAddress();
				this.client_port = this.getLocalPort();

				//resposta para o novo servidor
				OperacoesBinarias.inserirCabecalho(to_conect, 0, 0, true, false, true, false, 0, 0);
				DatagramPacket packet3 = new DatagramPacket(to_conect, to_conect.length,server_adress,server_port);
				this.send(packet3);
				//resposta para o novo servidor a partir de agora
				new Thread(new Receiver()).start();
			}
		}while(!done);
	}

	public Socket(String host, int port) throws IOException{
		super();
		this.is_server = false;
		boolean done = false;
		do{
			//Enviar solicita��o ao ServerSocket
			byte[] to_conect = new byte[Pacote.head_payload];
			OperacoesBinarias.inserirCabecalho(to_conect, 0, 0, false, false, true, false, 0, 0);
			DatagramPacket packet = new DatagramPacket(to_conect, to_conect.length,InetAddress.getByName(host),port);
			this.send(packet); //envia pacote para o servidor

			//receber mensagem do socket especial de servidor criado para comunical�ao
			DatagramPacket packet2  = new DatagramPacket(new byte[Pacote.default_size], Pacote.default_size);
			this.receive(packet2);
			byte[] temp = packet2.getData();
			if(OperacoesBinarias.extrairSYN(temp) && OperacoesBinarias.extrairACK(temp)){
				done  = true;
				this.server_adress = packet2.getAddress();
				this.server_port = packet2.getPort();
				this.client_adress = this.getLocalAddress();
				this.client_port = this.getLocalPort();

				//resposta para o novo servidor
				OperacoesBinarias.inserirCabecalho(to_conect, 0, 0, true, false, true, false, 0, 0);
				DatagramPacket packet3 = new DatagramPacket(to_conect, to_conect.length,server_adress,server_port);
				this.send(packet3);
				//resposta para o novo servidor a partir de agora
				new Thread(new Receiver()).start();
			}
		}while(!done);
	}
	//fim dos construtores reais

	//Soclet especial criado por um ServerSocket
	public Socket(InetAddress client_adress, int client_port, boolean isServer) throws IOException{
		boolean done = false;

		do{
			this.is_server = true; //Sou um Socket do servidor
			byte[] to_cliente = new byte[Pacote.head_payload];
			OperacoesBinarias.inserirCabecalho(to_cliente, 0, 0, true, false, true, false, 0, 0);
			DatagramPacket packet = new DatagramPacket(to_cliente, to_cliente.length, client_adress, client_port);
			this.send(packet); //envio mensagem de apresenta��o ao meu novo cliente

			//Seto meus endere�os
			this.server_adress = this.getLocalAddress();
			this.server_port = this.getLocalPort();

			//Recebo confirma��o do cliente
			DatagramPacket temp  = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
			this.receive(temp);
			if(OperacoesBinarias.extrairACK(temp.getData()) && OperacoesBinarias.extrairSYN(temp.getData())){
				done = true;
				this.client_adress = temp.getAddress();
				this.client_port = temp.getPort();

				//inicio o processo de inicializa��o da classe Socket sendo um server
				this.send_base.set(0);
				this.nextseqnum.set(0);

				new Thread(new Receiver()).start(); //Cria um receptor de ACKs
				new Thread(new Sender()).start(); //Cria uma thread respons�vel pelo envio de pacotes
				
				is_connected = true;
			}
		}while(!done);

	}

	//Server
	public Socket(int port) throws IOException{
		super(port);
		this.server_adress = InetAddress.getByName("localhost");
		this.server_port = port;
		DatagramPacket temp  = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		this.receive(temp);
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
	}

	//Cliente
	public Socket(int server_port, InetAddress server) throws IOException{
		super();
		this.server_adress = server;
		this.server_port = server_port;
		DatagramPacket temp  = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload,this.server_adress,this.server_port);
		this.send(temp);
		this.client_adress = this.getLocalAddress();
		this.client_port = this.getLocalPort();

		new Thread(new Receiver()).start();
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
	}

	//classes internas
	private class Sender extends Thread{

		@Override
		public void run() {
			while(!close.get()){

				if(nextseqnum.get()<=(send_base.get()+cwin.get())){//verifico se existe espa�o dentro da janela para criar um pacote

					byte[] to_packet = new byte[Pacote.default_size]; //array de bytes com dados lidos
					int as_read = 0; //inteiro para ver quantos bytes foram lidos

					try {
						as_read = read_internal(to_packet, Pacote.head_payload, Pacote.util_load);							
					} catch (IOException e) {
						System.out.println("Problemas com buffers internos");
						System.out.println("Encerrando conex�o...");
						close.set(true);
					}

					if(as_read>0){

						OperacoesBinarias.inserirCabecalho(to_packet, nextseqnum.get(), 0, false, false, false, false, as_read, 0); //inser��o de cabe�alh
						DatagramPacket packet = new DatagramPacket(to_packet, Pacote.default_size,client_adress,client_port);
						Pacote pacote = new Pacote(packet,System.currentTimeMillis(),as_read); //Cria pacote de buffer

						synchronized (sinc_send_buffer) {
							send_packet_buffer.add(pacote);
							if(!estimateRTT_process.get()){ //Se nenhum estimados de RTT está em progresso inicia um
								estimateRTT_for_packet.set(nextseqnum.get()); //estimamos o valor para 
							}
						}

						nextseqnum.incrementAndGet();

						synchronized (sinc_send_socket) { //adquire reserva do socket para enviar pacote
							try {
								send(packet);
								if(!timer_run.get()){//se nenhum temporizador esta ativo ativa um
									timer_run.set(true);
									new Thread(new Timeout()).start();
								}
							} catch (IOException e) {
								System.out.println("N�o foi poss�vel enviar um pacote, problema no Socket\nEncerrando conex�o");
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
					receive(packet); //recebe um pacote

					int key = OperacoesBinarias.extrairNumeroReconhecimento(buffer);
					int seqNum = OperacoesBinarias.extrairNumeroSequencia(buffer);
					int dataLength = OperacoesBinarias.extrairComprimentoDados(buffer);

					if(OperacoesBinarias.extrairFIN(buffer)){
						close.set(true);
						Socket.this.close();
					}else if(OperacoesBinarias.extrairSYN(buffer)){

					}else if(OperacoesBinarias.extrairACK(buffer)){

						if(packet.getAddress().equals(client_adress) && packet.getPort()==client_port){ //tenho um ack de quem me comunico

							Pacote temp = null;

							synchronized (sinc_send_buffer) {

								int remap = key-send_packets_cont.get();

								if((remap)<send_packet_buffer.size() && !send_packet_buffer.isEmpty() && remap>=0){ //verifica se o pacote esta dentro das possibilidades do buffer

									temp = send_packet_buffer.get(remap);


									temp_SampleRTT.set(System.currentTimeMillis()-temp.send_time); //atualiza variavel com sampleRTT tempor�rio


									if(!temp.isEnviado()){
										temp.setEnviado(true);
										cwin.set(Math.min(cwin.get()+1, max_win));
									}
								}
							}

							if(key>estimateRTT_for_packet.get() && estimateRTT_process.get() && temp!=null){
								long temp_SampleRTT = System.currentTimeMillis() - temp.send_time;//Calculo sampleRTT
								EstimatedRTT = (long) ((EstimatedRTT*0.875)+(0.125*temp_SampleRTT)); //Use para Estimar o proximoRTT
								DevRTT = (long) ((0.75*DevRTT)+(0.25*Math.abs(temp_SampleRTT-EstimatedRTT))); //E também o DevRTT
								timeout.set(EstimatedRTT+(4*DevRTT));//E altere o timeout
								estimateRTT_process.set(false);
							}

						}else{
							System.out.println("Recebi um ACK de um host estranho. Cuidado a rede pode estar sendo invadida! ^~^");
						}

					}else if(OperacoesBinarias.extrairRST(buffer)){

					}else{ //temos um pacote com dados

						if(packet.getAddress().equals(server_adress) && packet.getPort()==server_port){ //tenho um ack de quem me comunico
							//cria ack em resposta ao dado recebido
							byte[] to_ack = new byte[Pacote.head_payload];
							OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, rcv_base.get()+rwin.get());
							DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,server_adress,server_port);
							synchronized (sinc_send_socket) { //envia ack
								send(ack);
							}

							if(seqNum==rcv_base.get()){ //se temos o proximo pacote esperado
								write_internal(buffer, Pacote.head_payload, dataLength); //escreve para camada de cima
								rcv_base.incrementAndGet();//incrementa a base da janela
								velocidade.getAndAdd(dataLength);

								//tenta pegar mais pacotes que possa estar no buffer de recep��o
								while(rec_packet_buffer.get(rcv_base.get())!=null){
									byte[] dados = rec_packet_buffer.get(rcv_base.get());
									dataLength = OperacoesBinarias.extrairComprimentoDados(dados);
									seqNum = OperacoesBinarias.extrairNumeroSequencia(dados);
									velocidade.getAndAdd(dataLength);
									rcv_base.incrementAndGet(); //incrementa a base de recep��o para o proximo pacote
									write_internal(dados, Pacote.head_payload,dataLength);
								}

							}else if(seqNum>rcv_base.get()){ //se n�o temos o proximo pacote esperado
								//coloca ele no buffer
								rec_packet_buffer.put(seqNum, buffer);
							}else{
								System.out.println("Retransmissao");
							}
						}
					}
				} catch (IOException e) {
					System.out.println("Deu merda no Socket do receiver fechando conex�o...");
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
				} catch (InterruptedException e) {
					System.out.println("Erro no timeout");
				}


				if(!send_packet_buffer.isEmpty()){
					if(System.currentTimeMillis()-send_packet_buffer.get(0).send_time>(min_timeout/2)){

						if(!send_packet_buffer.get(0).isEnviado()){

							if(timeouts%3==0){
								ssthresh.set((cwin.get()/2)+10); // limiar de envio e setado pela metade
								cwin.set(1); //janela de congestionamento e setada para 1MSS
								quantos_zerou.set(0);
								restam_prox_cwin.set(1);
							}
							timeouts++;

							synchronized (sinc_send_socket) {
								try {
									int indice = 0;
									while(indice<send_packet_buffer.size() && !send_packet_buffer.isEmpty() && !send_packet_buffer.get(indice).isEnviado()){
										send(send_packet_buffer.get(indice).setSend_time_and_getme(System.currentTimeMillis()).pkt);
										indice++;
									}
								} catch (IOException e) {
									System.out.println("Problema com o socket de envio");
									System.out.println("Fechando conex�o");
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