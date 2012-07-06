package rede;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends Socket{
	boolean close;
	boolean conectado;
	DatagramSocket socket;
	
	public ServerSocket(int listerningPort) throws IOException{
		super(listerningPort);
	}
	
	public ServerSocket(int listerningPort,FileInputStream arquivo_enviar,FileOutputStream arquivo_recebe) throws IOException{
		super(listerningPort,arquivo_enviar,arquivo_recebe);
	}

	public Socket accept() throws IOException{

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		super.real_socket.receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			super.address_comparable = packet.getSocketAddress();
			this.setCliente(packet.getPort(), packet.getAddress());
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("Porta do cliente "+ packet.getPort());
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
		}
		System.out.println("Solicitação concluida");
		return this;
	}

	public void fechar(){

	}
}
