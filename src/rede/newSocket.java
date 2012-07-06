package rede;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class newSocket{

	//variaveis

	DatagramSocket socket;

	InetAddress endereco_cliente;
	InetAddress endereco_servidor;

	int porta_cliente;
	int porta_servidor;

	int base_envio = 0;
	int numero_seq = 0;
	int cwin = 1;
	int ssthresh = 64;
	int janela_maxima = 128;
	int numero_pacote_paraRTT = 0;
	int base_recepcao = 0;
	final int minTimeout = 500;
	long bytesTransferidos = 0;
	long DevRTT = 20;
	long EstimatedRTT = 1000;
	long timeout = 1000;
	long velocidade = 0;

	boolean conectado = false;
	boolean parar = false;
	boolean estimando_RTT = false;
	boolean temporizado_rodando = false;
	boolean eServidor = false;

	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};

	Hashtable<Integer, Pacote> buffer_envio;
	Hashtable<Integer, DatagramPacket> buffer_recepcao;

	Object lockSend;
	Object lockJanela;
	Object buffer_cliente = new Object();
	Object buffer_server = new Object();

	private int buffer_size = 20971520;	
	ByteArrayOutputStream server = new ByteArrayOutputStream();
	ByteArrayOutputStream cliente = new ByteArrayOutputStream(buffer_size);
	ByteArrayInputStream internal;
	ByteArrayInputStream to_cliente;
	//variaveis

	public long bytesTransferidos(){
		return bytesTransferidos;
	}

	public long resetTransferencias(){
		return this.bytesTransferidos = 0;
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

	//usado pelo cliente para indicar criar um socket para o servidor
	public newSocket(int porta_servidor, InetAddress endereco_servidor) throws IOException {
		socket = new DatagramSocket();
		this.porta_servidor = porta_servidor;
		this.endereco_servidor = endereco_servidor;
		DatagramPacket receiver = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		while(!conectado){
			socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			socket.receive(receiver);
			if(receiver.getAddress().equals(endereco_servidor) && receiver.getPort()==porta_servidor){
				conectado=true;
			}
		}
		buffer_recepcao = new Hashtable<Integer, DatagramPacket>();
		bytesTransferidos = 0;
		new Thread(new ReceiverCliente()).start();
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
	}

	//usado pelo servidor para ficar escutando na porta especifica
	public newSocket(int port) throws IOException {
		socket = new DatagramSocket(port);
		buffer_envio = new Hashtable<Integer, Pacote>();
		lockSend = new Object();
		lockJanela = new Object();
	}

	public void setCliente(int portaCliente, InetAddress enderecoCliente){
		this.endereco_cliente = enderecoCliente;
		this.porta_cliente = portaCliente;
		new Thread(new ReceiverServer()).start();
		new Thread(new Sender()).start();
	}


	public boolean getEstado(){
		return parar;
	}

	//classes internas
	private class Sender implements Runnable{

		@Override
		public void run() {
			while(!parar){
				if(numero_seq<base_envio+cwin){	
					try {
						byte[] dados = new byte[Pacote.default_size]; //array de bytes com dados lidos
						int bytes_lidos = read_internal(dados, Pacote.head_payload, Pacote.util_load);
						if(bytes_lidos>0){
							OperacoesBinarias.inserirCabecalho(dados, numero_seq, 0, false, false, false, false, bytes_lidos, 0);
							DatagramPacket packet = new DatagramPacket(dados, Pacote.default_size,endereco_cliente,porta_cliente);
							Pacote pacote = new Pacote(packet, System.currentTimeMillis(), bytes_lidos);
							buffer_envio.put(numero_seq, pacote);
							if(!estimando_RTT){
								numero_pacote_paraRTT = numero_seq;
							}
							numero_seq++;

							synchronized (lockSend) {
								socket.send(packet);
							}

							if(!temporizado_rodando){
								temporizado_rodando = true;
								new Thread(new Timeout()).start();
							}

						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}else{
					if(!temporizado_rodando){//se nenhum temporizador esta ativo ativa um
						temporizado_rodando = true;
						new Thread(new Timeout()).start();
					}
				}

				synchronized (lockJanela) {
					while(buffer_envio.get(base_envio)!=null){
						bytesTransferidos += buffer_envio.get(base_envio).dataLenth;
						buffer_envio.remove(base_envio);
						base_envio++;
					}
				}

			}
			System.out.println("Sender encerrando...");
		}
	}

	private class ReceiverServer implements Runnable{

		@Override
		public void run() {
			while(!parar){

				try {
					byte[] dados = new byte[Pacote.head_payload];
					DatagramPacket packet = new DatagramPacket(dados, dados.length);
					socket.receive(packet);

					if(packet.getAddress().equals(endereco_cliente) && packet.getPort()==porta_cliente){
						if(OperacoesBinarias.extrairACK(packet.getData())){
							int chave = OperacoesBinarias.extrairNumeroReconhecimento(packet.getData());
							if(buffer_envio.get(chave)!=null){
								if(!buffer_envio.get(chave).is_send){
									buffer_envio.get(chave).setEnviado(true);
									cwin = Math.min(janela_maxima, cwin+1);
								}

								if(chave>numero_pacote_paraRTT && estimando_RTT){
									long temp_SampleRTT = System.currentTimeMillis() - buffer_envio.get(chave).send_time;//Calculo sampleRTT
									EstimatedRTT = (long) ((EstimatedRTT*0.875)+(0.125*temp_SampleRTT)); //Use para Estimar o proximoRTT
									DevRTT = (long) ((0.75*DevRTT)+(0.25*Math.abs(temp_SampleRTT-EstimatedRTT))); //E também o DevRTT
									timeout = (EstimatedRTT+(4*DevRTT));//E altere o timeout
									estimando_RTT = false;
								}
							}
						}else if(OperacoesBinarias.extrairFIN(packet.getData())){

						}
					}else{
						System.out.println("Recebendo dados de local estranho");
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Receiver encerrando...");
		}
	}

	private class ReceiverCliente implements Runnable{

		@Override
		public void run() {

			while(!parar){

				try {
					byte[] dados = new byte[Pacote.default_size];
					DatagramPacket packet = new DatagramPacket(dados, dados.length);
					socket.receive(packet);
					if(packet.getAddress().equals(endereco_servidor)&& packet.getPort()==porta_servidor){
						int seqNum = OperacoesBinarias.extrairNumeroSequencia(packet.getData());
						int Qbytes = OperacoesBinarias.extrairComprimentoDados(packet.getData());
						if(OperacoesBinarias.extrairFIN(packet.getData())){

						}else{//dados
							byte[] to_ack = new byte[Pacote.head_payload];
							OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, 0);
							DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,endereco_servidor,porta_servidor);
							socket.send(ack);

							if(seqNum==base_recepcao){
								write_internal(packet.getData(), Pacote.head_payload, Qbytes);
								base_recepcao++;
								velocidade += Qbytes;

								while(buffer_recepcao.get(base_recepcao)!=null){
									Qbytes = OperacoesBinarias.extrairComprimentoDados(buffer_recepcao.get(base_recepcao).getData());
									write_internal(buffer_recepcao.get(base_recepcao).getData(), Pacote.head_payload, Qbytes);
									velocidade += Qbytes;
									base_recepcao++;
								}

							}else if(seqNum>base_recepcao){
								buffer_recepcao.put(seqNum, packet);
							}else{
								System.out.println("Retransmissao");
							}
						}
					}else{
						System.out.println("Recebendo dados de local estranho");
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}

	private class Timeout implements Runnable{
		protected boolean continua = true;
		protected int timeouts;

		@Override
		public void run() {
			while(!parar && continua){
				try {
					Thread.sleep(Math.max(minTimeout, timeout));

					if(!buffer_envio.isEmpty()){
						synchronized (lockJanela) {
							if(System.currentTimeMillis()-buffer_envio.get(base_envio).send_time>minTimeout/2){
								if(!buffer_envio.get(base_envio).is_send){
									if(timeouts%3==0){
										ssthresh = (cwin/2)+3;
										cwin = 1;
									}
									timeouts++;

									int indice = base_envio;
									while(buffer_envio.get(base_envio)!=null && !buffer_envio.get(base_envio).is_send){
										synchronized (lockSend) {
											socket.send(buffer_envio.get(indice).pkt);
											indice++;
										}
									}
								}
							}
						}
					}else{
						continua = false;
					}

				} catch (InterruptedException e) {
					e.printStackTrace();
					continua = false;
				} catch (IOException e) {
					e.printStackTrace();
					continua = false;
				}
			}

			temporizado_rodando = false;
			System.out.println("Encerrando temporizador");
		}

	}

	private class Bandwidth extends TimerTask{

		double repVelo = 0;

		@Override
		public void run() {
			repVelo = (repVelo * 0.825) + ((velocidade / 1024)*0.175);
			velocidade = 0;

			System.out.println((int) repVelo + " Kb/s");

			if(parar){
				this.cancel();
			}
		}
	}
	//fim classes internas

}
