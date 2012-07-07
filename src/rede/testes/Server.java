package rede.testes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import newRede.ServerSocket2;

import rede.Pacote;
import rede.ServerSocket;
import rede.Socket;
import rede.Socket2;
import rede.newSocket;

public class Server {
	public static void main(String[] args) throws IOException, InterruptedException {
		FileInputStream in = new FileInputStream("FF.zip");
		ServerSocket2 serverSocket = new ServerSocket2(3000,in);
		newRede.Socket2 socket = serverSocket.accept();
		
				
		System.out.println("Encerrando...");
	}
}