package rede.testes;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;


public class UDP {
	public static void main(String[] args) throws IOException {
		DatagramSocket socket = new DatagramSocket();
		String luiz_diz = "Bruno seu porra eu quero dormir";
		DatagramPacket packet = new DatagramPacket(luiz_diz.getBytes(), luiz_diz.length(),InetAddress.getByName("172.20.4.99"),3000);
		socket.send(packet);
	}
}
