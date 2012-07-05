package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ServerSocket extends DatagramSocket {
	int listerningPort;
	boolean close;
	public ServerSocket(int listerningPort) throws SocketException{
		super(listerningPort);
	}

	public Socket accept() throws IOException{
		boolean done = false;
		Socket socket = null;
		do{
			byte[] arra_rec = new byte[Pacote.default_size];
			DatagramPacket packet = new DatagramPacket(arra_rec, arra_rec.length);
			this.receive(packet);
			if(OperacoesBinarias.extrairSYN(arra_rec)){
				socket = new Socket(packet.getAddress(), packet.getPort(), true);
				done = socket.isConnected();
			}
		}while(!done);
		return socket;
	}

	@Override
	public void close(){
		super.close();
	}
}
