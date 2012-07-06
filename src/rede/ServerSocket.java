package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends newSocket{
	boolean close;
	boolean conectado;

	public ServerSocket(int listerningPort) throws IOException{
		super(listerningPort);
	}

	public newSocket accept() throws IOException{

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			this.setCliente(packet.getPort(), packet.getAddress());
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("`Porta do cliente "+ packet.getPort());
			send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
		}
		return this;
	}

	public void fechar(){
		this.close();
	}
}
