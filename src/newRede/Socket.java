package newRede;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

	HashMap<Integer, Pacote> enviados= new HashMap<Integer, Pacote>();
	ConcurrentHashMap<Integer,byte[]> recebidos = new ConcurrentHashMap<Integer,byte[]>();
	
	Object syncEnviados = new Object();
	
	AtomicBoolean close = new AtomicBoolean(false);
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

		new Timer().scheduleAtFixedRate(new ReceiverPackets(), 0, 50);
		new Timer().scheduleAtFixedRate(new Armazena(), 100, 100);
	}

	class Sender extends TimerTask{
		byte[] dados = new byte[Pacote.default_size];
		int bytes_lidos = 0;

		@Override
		public void run() {
			if(nextseqnum<base_envio.get()+cwin.get()){
				int k = (base_envio.get()+cwin.get())-nextseqnum;
//				System.out.println("K= "+k);
				while(k-->0){
					try {
						bytes_lidos = arquivo_envio.read(dados,Pacote.head_payload,Pacote.util_load);
						OperacoesBinarias.inserirCabecalho(dados, nextseqnum, 0, false, false, false, false, bytes_lidos, 0);
						DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco_cliente, porta_cliente); 
						socket.send(pacote);
//						System.out.println(nextseqnum+" enviado");
						synchronized (syncEnviados) {
							enviados.put(nextseqnum, new Pacote(pacote,System.currentTimeMillis(),bytes_lidos));
						}
						socket.send(pacote);
						nextseqnum++;
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					if(!timeout_rodando.get()){
						new Timer().scheduleAtFixedRate(new Timeout(), 100, 100);
					}
				}
		}else{
			System.out.println("N�o da");
			if(!timeout_rodando.get()){
				new Timer().scheduleAtFixedRate(new Timeout(), 100, 100);
			}
		}
			
		synchronized (syncEnviados) {
			while(!enviados.isEmpty() && enviados.get(base_envio.get())!=null){
				ultimoEnviado += enviados.get(base_envio.get()).getDataLentgh();
				enviados.remove(base_envio.getAndIncrement());
			}
		}
		
		
		if(close.get()){
			this.cancel();
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
				int numeroSequencia = OperacoesBinarias.extrairNumeroSequencia(buffer);
				enviarACK(packet.getAddress(), packet.getPort(), OperacoesBinarias.extrairNumeroSequencia(buffer));  //envia ack
				if(OperacoesBinarias.extrairNumeroSequencia(buffer)>=base_recepcao.get()){
					recebidos.put(numeroSequencia,buffer);
					if(numeroSequencia==base_recepcao.get()){
						System.out.println("Pacote em ordem "+numeroSequencia);
					}else{
						System.out.println("Pacote fora de ordem "+ numeroSequencia);
					}
				}else{
					System.out.println("Retrasmissao");
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(close.get()){
			this.cancel();
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
					cwin.getAndAdd(100);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(close.get()){
			this.cancel();
		}
	}
}

class Timeout extends TimerTask{

	@Override
	public void run() {
		try {
			synchronized (syncEnviados) {
				if(!enviados.isEmpty()){
					if(enviados.get(base_envio.get())!=null){
						if((System.currentTimeMillis()-enviados.get(base_envio.get()).send_time)>500){
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
			}
		} catch (IOException e) {
			e.printStackTrace();
			timeout_rodando.set(false);
		} catch (Exception e) {
			e.printStackTrace();
			timeout_rodando.set(false);
			close.set(true);
		}
		
		if(close.get()){
			this.cancel();
		}
	}
}

private class Armazena extends TimerTask{

	@Override
	public void run() {
		while(recebidos.get(base_recepcao.get())!=null){
			try {
				arquivo_recebido.write(recebidos.get(base_recepcao.get()), Pacote.head_payload, OperacoesBinarias.extrairComprimentoDados(recebidos.get(base_recepcao.get())));
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
