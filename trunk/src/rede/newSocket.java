package rede;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.acl.LastOwnerException;
import java.util.Hashtable;

public class newSocket extends DatagramSocket {

	//variaveis
	InetAddress endereco_cliente;
	InetAddress endereco_servidor;

	int porta_cliente;
	int porta_servidor;
	
	int base_envio = 0;
	int numero_seq = 0;
	int cwin = 1;
	int janela_maxima = 64;
	int numero_pacote_paraRTT;
	int base_recepcao;
	
	long ultimo_byte_enviado;
	long DevRTT;
	long EstimatedRTT;
	long timeout;
	long velocidade;
	
	boolean conectado = false;
	boolean parar = false;
	boolean estimando_RTT = false;
	boolean temporizado_rodando = false;

	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};

	PipedOutputStream server_escreve;
	PipedInputStream leitor_interno;
	PipedOutputStream escritor_interno;
	PipedInputStream cliente_le;

	Hashtable<Integer, Pacote> buffer_envio;
	Hashtable<Integer, DatagramPacket> buffer_recepcao;

	Object locksender;
	//variaveis

	//usado pelo cliente para indicar criar um socket para o servidor
	public newSocket(int porta_servidor, InetAddress endereco_servidor) throws IOException {
		super();
		this.porta_servidor = porta_servidor;
		this.endereco_servidor = endereco_servidor;
		DatagramPacket receiver = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		while(!conectado){
			send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			receive(receiver);
			if(receiver.getAddress().equals(endereco_servidor) && receiver.getPort()==porta_servidor){
				conectado=true;
			}
		}
		escritor_interno = new PipedOutputStream();
		cliente_le = new PipedInputStream(escritor_interno);
		buffer_recepcao = new Hashtable<Integer, DatagramPacket>();
		ultimo_byte_enviado = 0;
	}

	//usado pelo servidor para ficar escutando na porta especifica
	public newSocket(int port) throws IOException {
		super(port);
		server_escreve = new PipedOutputStream();
		leitor_interno = new PipedInputStream(server_escreve);
		buffer_envio = new Hashtable<Integer, Pacote>();
		locksender = new Object();
	}

	public void setCliente(int portaCliente, InetAddress enderecoCliente){
		this.endereco_cliente = enderecoCliente;
		this.porta_cliente = portaCliente;
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
						int bytes_lidos = leitor_interno.read(dados, Pacote.head_payload, Pacote.util_load);
						if(bytes_lidos>0){
							OperacoesBinarias.inserirCabecalho(dados, numero_seq, 0, false, false, false, false, bytes_lidos, 0);
							DatagramPacket packet = new DatagramPacket(dados, Pacote.default_size,endereco_cliente,porta_cliente);
							Pacote pacote = new Pacote(packet, System.currentTimeMillis(), bytes_lidos);
							buffer_envio.put(numero_seq, pacote);
							if(!estimando_RTT){
								numero_pacote_paraRTT = numero_seq;
							}
							numero_seq++;

							synchronized (locksender) {
								send(packet);
							}

							if(!temporizado_rodando){
								temporizado_rodando = true;
								new Thread(new Timeout()).start();
							}

						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}else{
					if(!temporizado_rodando){//se nenhum temporizador esta ativo ativa um
						temporizado_rodando = true;
						new Thread(new Timeout()).start();
					}
				}

				while(buffer_envio.get(base_envio)!=null){
					ultimo_byte_enviado += buffer_envio.get(base_envio).dataLenth;
					buffer_envio.remove(base_envio);
					base_envio++;
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
					receive(packet);

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
		}
	}

	private class ReceiverCliente implements Runnable{

		@Override
		public void run() {
			byte[] dados = new byte[Pacote.default_size];
			DatagramPacket packet = new DatagramPacket(dados, dados.length);
			try {
				receive(packet);
				if(packet.getAddress().equals(endereco_servidor)&& packet.getPort()==porta_servidor){
					int seqNum = OperacoesBinarias.extrairNumeroSequencia(packet.getData());
					int Qbytes = OperacoesBinarias.extrairComprimentoDados(packet.getData());
					if(OperacoesBinarias.extrairFIN(packet.getData())){
						
					}else{//dados
						byte[] to_ack = new byte[Pacote.head_payload];
						OperacoesBinarias.inserirCabecalho(to_ack, 0, seqNum, true, false, false, false, 0, 0);
						DatagramPacket ack = new DatagramPacket(to_ack, Pacote.head_payload,endereco_servidor,porta_servidor);
						synchronized (locksender) { //envia ack
							send(ack);
						}
						
						if(seqNum==base_recepcao){
							escritor_interno.write(packet.getData(), Pacote.head_payload, Qbytes);
							base_recepcao++;
							velocidade += Qbytes;
							
							while(buffer_recepcao.get(base_recepcao)!=null){
								Qbytes = OperacoesBinarias.extrairComprimentoDados(buffer_recepcao.get(base_recepcao).getData());
								escritor_interno.write(buffer_recepcao.get(base_recepcao).getData(), Pacote.head_payload, Qbytes);
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

	private class Timeout implements Runnable{

		@Override
		public void run() {
			
		}

	}
	//fim classes internas

}
