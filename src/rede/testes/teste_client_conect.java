package rede.testes;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import rede.newSocket;

public class teste_client_conect {
	public static void main(String[] args) throws UnknownHostException, IOException {
		newSocket socket = new newSocket(3000, InetAddress.getByName("172.20.4.99"));
	}
}
