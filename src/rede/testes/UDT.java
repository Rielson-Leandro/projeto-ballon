package rede.testes;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;



public class UDT {
	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket(3000);
		DatagramPacket packet = new DatagramPacket(new byte[500], 500);
		socket.receive(packet);
		System.out.println(new String(packet.getData()));
	}
}
