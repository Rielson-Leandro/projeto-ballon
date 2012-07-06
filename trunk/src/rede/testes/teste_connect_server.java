package rede.testes;

import java.io.IOException;

import rede.ServerSocket;

public class teste_connect_server {
	public static void main(String[] args) throws IOException {
		ServerSocket serverSocket = new ServerSocket(3000);
		serverSocket.accept();
		System.out.println("Conectado");
	}
}
