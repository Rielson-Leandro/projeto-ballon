package network;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import network.util.OperacoesBinarias;
import network.util.Pacote;

public class miniServerSocket extends miniSocket{
	boolean close;
	boolean conectado;
	DatagramSocket socket;
	
	
	public miniServerSocket(int listerningPort,RandomAccessFile arquivo_enviar) throws IOException,FileNotFoundException{
		super(listerningPort);
		super.arquivo_enviar = arquivo_enviar;
	}

	public miniSocket accept() throws IOException{

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		super.real_socket.receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			super.client_adress = packet.getAddress();
			super.client_port = packet.getPort();
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("Porta do cliente "+ packet.getPort());
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			new Thread(new Receiver()).start();
			new Thread(new Sender()).start();
		}
		System.out.println("Solicitação concluida");
		return this;
	}

	public void fechar(){

	}
}
