package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends newSocket{
	DatagramSocket real_socket;
	boolean close;
	boolean conectado;

	public ServerSocket(int listerningPort) throws IOException{
		super(listerningPort);
	}

	public newSocket accept() throws IOException{
		boolean done = false;

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		real_socket.receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			this.setCliente(packet.getPort(), packet.getAddress());
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("`Porta do cliente "+ packet.getPort());
			send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload));
			send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload));
		}
		return this;
	}

	public void fechar(){
		this.close();
	}
}
