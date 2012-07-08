package rede.testes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import newRede.Socket2;

import rede.Pacote;
import rede.Socket;
import rede.newSocket;

public class Client {
	public static void main(String[] args) throws IOException {
//		Socket socket = new Socket(3000,InetAddress.getByName("172.20.4.99"));

		FileOutputStream stream = new FileOutputStream("rac2011.iso");
		Socket2 socket2 = new Socket2(3000, InetAddress.getByName("172.20.4.99"), stream);
	}
}
