package rede.testes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import rede.Pacote;

public class UDP_send {
	
	public void send() throws IOException{
		@SuppressWarnings("resource")
		FileInputStream stream = new FileInputStream("rac2011.iso");
		@SuppressWarnings("resource")
		DatagramSocket socket = new DatagramSocket(50000);
		DatagramPacket temp = new DatagramPacket(new byte[16], 16);
		socket.receive(temp);
		InetAddress adrr = temp.getAddress();
		int port = temp.getPort();
		
		while(stream.available()>0){
			byte[] data = new byte[Pacote.default_size];
			int as_read = stream.read(data);
			DatagramPacket packet = new DatagramPacket(data, as_read, adrr, port);
			socket.send(packet);
		}
	}
	
	public static void main(String[] args) throws IOException {
		new UDP_send().send();
	}
}
