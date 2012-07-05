package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends DatagramSocket {
	int listerningPort;
	boolean close;
	ServerSocket(int listerningPort) throws SocketException{
		super(listerningPort);
		this.close = false;
	}

	public Socket accept() throws IOException{
		boolean concluido = false;
		do{
			byte[] arra_rec = new byte[Pacote.default_size];
			DatagramPacket packet = new DatagramPacket(arra_rec, arra_rec.length);
			this.receive(packet);
			if(OperacoesBinarias.extrairSYN(arra_rec)){
				
			}
		}while(!concluido);
		
	}

	@Override
	public void close(){
		super.close();
	}
}
