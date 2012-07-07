package newRede;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jfree.ui.ArrowPanel;

import rede.OperacoesBinarias;
import rede.Pacote;

public class Socket {

	DatagramSocket socket;
	InetAddress endereco_cliente;
	int porta_cliente;

	int nextseqnum;
	AtomicInteger base_recepcao = new AtomicInteger(0);
	AtomicInteger base_envio = new AtomicInteger(0);
	AtomicInteger cwin = new AtomicInteger(1);

	long ultimoEnviado = 0;
	long bytes_recebidos = 0;

	AtomicBoolean timeout_rodando = new AtomicBoolean(false);
	boolean parar = false;

	FileInputStream arquivo_envio;
	FileOutputStream arquivo_recebido;

	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};

	ConcurrentHashMap<Integer, Pacote> enviados= new ConcurrentHashMap<Integer, Pacote>();
	ConcurrentHashMap<Integer,byte[]> recebidos = new ConcurrentHashMap<Integer,byte[]>();

	public Socket(){

	}

	public Socket(int porta_servidor, InetAddress endereco_servidor,FileOutputStream arquivo_recebido) throws IOException {
		this.arquivo_recebido = arquivo_recebido;
		this.socket = new DatagramSocket();
		DatagramPacket receiver = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		boolean connectado = false;
		while(!connectado){
			socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			socket.send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			socket.receive(receiver);
			if(receiver.getAddress().equals(endereco_servidor) && receiver.getPort()==porta_servidor){
				connectado=true;
			}
		}

		new Timer().scheduleAtFixedRate(new ReceiverPackets(), 0, 1);
		new Timer().scheduleAtFixedRate(new Armazena(), 100, 100);
	}

	class Sender extends TimerTask{
		byte[] dados = new byte[Pacote.default_size];
		int bytes_lidos = 0;

		@Override
		public void run() {
			if(nextseqnum<=base_envio.get()+cwin.get()){
				int numero_pacotes_enviar = (base_envio.get()+cwin.get())-nextseqnum;
				System.out.println("Peguei permissao para enviar" + numero_pacotes_enviar);
				try {
					for (int i = 0; i < numero_pacotes_enviar && arquivo_envio.available()>0; i++) {
						bytes_lidos = arquivo_envio.read(dados,Pacote.head_payload,Pacote.util_load);
						OperacoesBinarias.inserirCabecalho(dados, nextseqnum, 0, false, false, false, false, bytes_lidos, 0);
						DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco_cliente, porta_cliente); 
						socket.send(pacote);
						enviados.put(nextseqnum, new Pacote(pacote,System.currentTimeMillis(),bytes_lidos));
						socket.send(pacote);
						nextseqnum++;
					}
					if(!timeout_rodando.get()){
						new Timer().scheduleAtFixedRate(new Timeout(), 100, 100);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}else{
				System.out.println("Não da");
				if(!timeout_rodando.get()){
					new Timer().scheduleAtFixedRate(new Timeout(), 100, 100);
				}
			}
			
			while(!enviados.isEmpty() && enviados.get(base_envio.get())!=null){
				ultimoEnviado += enviados.get(base_envio.get()).getDataLentgh();
				enviados.remove(base_envio.getAndIncrement());
			}
		}
	}

	private class ReceiverPackets extends TimerTask{

		public void enviarACK(InetAddress server_adress, int server_port,int seqNum) throws IOException{
			DatagramPacket packet = new DatagramPacket(OperacoesBinarias.inserirCabecalho(new byte[Pacote.head_payload], 0, seqNum, true, false, false, false, 0, 0), Pacote.head_payload, server_adress, server_port);
			socket.send(packet);
		}

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[Pacote.default_size];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				if(OperacoesBinarias.extrairFIN(buffer)){

				}else{
					System.out.println("Pacote recebido"+OperacoesBinarias.extrairNumeroSequencia(buffer));
					enviarACK(packet.getAddress(), packet.getPort(), OperacoesBinarias.extrairNumeroSequencia(buffer));  //envia ack
					if(OperacoesBinarias.extrairNumeroSequencia(buffer)>=base_recepcao.get()){
						recebidos.put(OperacoesBinarias.extrairNumeroSequencia(buffer),buffer);
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class ReceiverAcks extends TimerTask{

		@Override
		public void run() {
			try {
				byte[] buffer = new byte[Pacote.default_size];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);
				System.out.println("ACK "+OperacoesBinarias.extrairNumeroReconhecimento(buffer));
				if(OperacoesBinarias.extrairFIN(buffer)){

				}else if(OperacoesBinarias.extrairACK(buffer)){
					if(enviados.get(OperacoesBinarias.extrairNumeroReconhecimento(buffer))!=null){
						enviados.get(OperacoesBinarias.extrairNumeroReconhecimento(buffer)).setEnviado(true);
						cwin.getAndAdd(10);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	class Timeout extends TimerTask{

		@Override
		public void run() {
			try {
				if(!enviados.isEmpty()){
					if(enviados.get(base_envio.get())!=null){
						if((System.currentTimeMillis()-enviados.get(base_envio.get()).send_time)>200){
							System.out.println("TimeoutEvent");
							int indice = base_envio.get();
							while(enviados.get(indice)!=null){
								socket.send(enviados.get(indice).pkt);
								cwin.set(cwin.get()/2);
								indice++;
							}
							

						}
					}
				}else{
					timeout_rodando.set(false);
				}
			} catch (IOException e) {
				e.printStackTrace();
				timeout_rodando.set(false);
			} catch (Exception e) {
				e.printStackTrace();
				timeout_rodando.set(false);
			}
		}
	}

	private class Armazena extends TimerTask{

		@Override
		public void run() {
			while(recebidos.get(base_recepcao.get())!=null){
				try {
					arquivo_recebido.write(recebidos.get(base_recepcao.get()), Pacote.head_payload, OperacoesBinarias.extrairComprimentoDados(recebidos.get(base_recepcao.getAndIncrement())));
					bytes_recebidos+= OperacoesBinarias.extrairComprimentoDados(recebidos.get(base_envio.get()));
					recebidos.remove(base_envio.getAndIncrement());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
