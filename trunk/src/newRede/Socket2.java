package newRede;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import rede.OperacoesBinarias;
import rede.Pacote;


public class Socket2{
	//Socket do lado client
	protected int server_port; //Usado tambem do lado server;
	protected InetAddress server_adress;

	//Socket do lado servidor;
	protected int client_port; //Usado tambem do lado cliente;
	protected InetAddress client_adress;

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
	AtomicBoolean connect = new AtomicBoolean(false);

	AtomicLong velocidade = new AtomicLong(0);

	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};

	//Objetos para sincroniza��o
	Object buffer_cliente = new Object();
	Object buffer_server = new Object();
	Object sinc_send_buffer = new Object(); //objeto para sincroniza de escrita e leitura do buffer de pacotes enviados
	Object sinc_send_socket = new Object();
	Object sinc_var_timeout = new Object(); //mutex para opera��es que envolvem mechar com v�riavies que est�o no calculo de timeout
	//Objetos para sincroniza��o

	//Buffers internos para Pacotes
	ConcurrentHashMap<Integer,Pacote> send_packet_buffer = new ConcurrentHashMap<Integer,Pacote>();
	ConcurrentHashMap<Integer, byte[]> rec_packet_buffer = new ConcurrentHashMap<Integer,byte[]>(); //chave vai ser numero de sequencia
	//Buffers internos para Pacotes

	//Buffers internos para Dados
	private int buffer_size = 20971520;	
	ByteArrayOutputStream server = new ByteArrayOutputStream();
	ByteArrayOutputStream cliente = new ByteArrayOutputStream(buffer_size);
	ByteArrayInputStream internal;
	ByteArrayInputStream to_cliente;
	//buffers internos para Dados

	FileInputStream arquivo_enviado;
	FileOutputStream arquivo_recebido;

	public FileInputStream getArquivo_enviado() {
		return arquivo_enviado;
	}

	public void setArquivo_enviado(FileInputStream arquivo_enviado) {
		this.arquivo_enviado = arquivo_enviado;
	}

	public FileOutputStream getArquivo_recebido() {
		return arquivo_recebido;
	}

	public void setArquivo_recebido(FileOutputStream arquivo_recebido) {
		this.arquivo_recebido = arquivo_recebido;
	}

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
				System.out.println("Fechando conex�o...");
				close.set(true);
			}
		}
		close.set(true);
		real_socket.close();
	}

	public boolean isConnected(){
		return is_connected;
	}

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

	//usado por quem vai receber um arquivo
	public Socket2(int porta_servidor, InetAddress endereco_servidor,FileOutputStream arquivo) throws IOException {
		this.setArquivo_recebido(arquivo);
		real_socket = new DatagramSocket();
		this.server_port = porta_servidor;
		this.server_adress = endereco_servidor;
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
		new Timer().scheduleAtFixedRate(new Armazena(), 100, 100);
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
	}

	public Socket2(){
	}
	
	public void setCliente(int portaCliente, InetAddress enderecoCliente){
		
	}

	//classes internas
	class Sender extends Thread{
		byte[] to_packet = new byte[Pacote.default_size]; //array de bytes com dados lidos
		int as_read = 0; //inteiro para ver quantos bytes foram lidos

		@Override
		public void run() {
			while(!close.get()){

				if(nextseqnum.get()<=(send_base.get()+cwin.get())){//verifico se existe espa�o dentro da janela para criar um pacote

					try {
						as_read = arquivo_enviado.read(to_packet, Pacote.head_payload, Pacote.util_load);	

						if(as_read>0){

							OperacoesBinarias.inserirCabecalho(to_packet, nextseqnum.get(), 0, false, false, false, false, as_read, 0); //inser��o de cabe�alh
							DatagramPacket packet = new DatagramPacket(to_packet, Pacote.default_size,client_adress,client_port);
							Pacote pacote = new Pacote(packet,System.currentTimeMillis(),as_read); //Cria pacote de buffer

							synchronized (sinc_send_buffer) {
								send_packet_buffer.put(nextseqnum.get(),pacote);
								if(!estimateRTT_process.get()){ //Se nenhum estimados de RTT está em progresso inicia um
									estimateRTT_for_packet.set(nextseqnum.get()); //estimamos o valor para 
								}
							}
							
							nextseqnum.incrementAndGet();

							synchronized (sinc_send_socket) { //adquire reserva do socket para enviar pacote
								real_socket.send(packet);
								if(!timer_run.get()){//se nenhum temporizador esta ativo ativa um
									timer_run.set(true);
									new Thread(new Timeout()).start();
								}
							}
						}

					} catch (IOException e) {
						System.out.println("Problemas com buffers internos");
						System.out.println("Encerrando conex�o...");
						close.set(true);
						e.printStackTrace();
					}


				}else{
					if(!timer_run.get()){//se nenhum temporizador esta ativo ativa um
						timer_run.set(true);
						new Thread(new Timeout()).start();
					}
				}
			}
			System.out.println("Sender encerrando...");
		}
	}

	class Receiver extends Thread{

		@Override
		public void run() {
			while(!close.get()){ 
				byte[] buffer = new byte[Pacote.default_size];
				DatagramPacket packet = new DatagramPacket(buffer, Pacote.default_size);

				try {
					real_socket.receive(packet); //recebe um pacote

					int key = OperacoesBinarias.extrairNumeroReconhecimento(buffer);
					int seqNum = OperacoesBinarias.extrairNumeroSequencia(buffer);
				
					if(OperacoesBinarias.extrairFIN(buffer)){
						close.set(true);
						Socket2.this.close();
					}else if(OperacoesBinarias.extrairACK(buffer)){

						if(packet.getAddress().equals(client_adress) && packet.getPort()==client_port){ //tenho um ack de quem me comunico

							Pacote temp = null;

							synchronized (sinc_send_buffer) {

								
								if(!send_packet_buffer.isEmpty() && send_packet_buffer.get(key)!=null){ //verifica se o pacote esta dentro das possibilidades do buffer

									temp = send_packet_buffer.get(key);
								
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

					}else{ //temos um pacote com dados

						if(packet.getAddress().equals(server_adress) && packet.getPort()==server_port){ //tenho um ack de quem me comunico
							//cria ack em resposta ao dado recebido
							byte[] to_ack = new byte[Pacote.head_payload];
							OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, rcv_base.get()+rwin.get());
							DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,server_adress,server_port);
							synchronized (sinc_send_socket) { //envia ack
								real_socket.send(ack);
							}

							if(seqNum>=rcv_base.get()){ //se temos o proximo pacote esperado
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

	class Timeout implements Runnable{
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

	class Armazena extends TimerTask{
		int dataLength;
		@Override
		public void run() {
			
			while(rec_packet_buffer.get(rcv_base.get())!=null){
				byte[] dados = rec_packet_buffer.get(rcv_base.get());
				dataLength= OperacoesBinarias.extrairComprimentoDados(dados);
				velocidade.getAndAdd(dataLength);
				rcv_base.incrementAndGet(); //incrementa a base de recep��o para o proximo pacote
				try {
					arquivo_recebido.write(dados, Pacote.head_payload,dataLength);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}
	
	class AndarJanela extends TimerTask{

		@Override
		public void run() {	
			synchronized (sinc_send_buffer) {
				while(!send_packet_buffer.isEmpty() && send_packet_buffer.get(0).isEnviado()){
					last_send.addAndGet(send_packet_buffer.get(0).dataLenth);
					send_base.incrementAndGet();//incrementa o valor send_base
					send_packet_buffer.remove(0); //remove o pacore do buffer
					send_packets_cont.incrementAndGet();
				}
			}
		}
		
	}
	
	class Bandwidth extends TimerTask{

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

	class Transfered extends TimerTask{
		@Override
		public void run() {
			System.out.println("Bytes transferidos com sucesso: "+ last_send.get());

			if(close.get()){
				this.cancel();
			}
		}
	}
}