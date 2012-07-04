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
		FileInputStream stream = new FileInputStream("Ubuntu.iso");
		DatagramSocket socket = new DatagramSocket();
//		InetAddress adress = InetAddress.getByName("172.20.4.80");
		InetAddress adress = InetAddress.getByName("localhost");
		int port = 3000;
		while(stream.available()>0){
			byte[] data = new byte[Pacote.default_size];
			int as_read = stream.read(data);
			DatagramPacket packet = new DatagramPacket(data, as_read, adress, port);
			socket.send(packet);
		}
	}
	
	public static void main(String[] args) throws IOException {
		new UDP_send().send();
	}
}
