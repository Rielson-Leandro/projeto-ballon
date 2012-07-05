package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends Socket{
	DatagramSocket real_socket;
	boolean close;
	
	public ServerSocket(int listerningPort) throws IOException{
		super(listerningPort);
	}

	public Socket accept() throws IOException{
		boolean done = false;
		
		do{
			byte[] arra_rec = new byte[Pacote.default_size];
			DatagramPacket packet = new DatagramPacket(arra_rec, arra_rec.length);
			real_socket.receive(packet);
			if(OperacoesBinarias.extrairSYN(arra_rec)){
				System.out.println("Nova solicitação de conexão");
				System.out.println("Endereço do cliente "+ packet.getAddress());
				System.out.println("`Porta do cliente "+ packet.getPort());
			}
		}while(!done);
		return this;
	}

	public void close(){
		real_socket.close();
		close = true;
	}
}
