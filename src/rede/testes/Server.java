package rede.testes;

import java.io.FileInputStream;
import java.io.IOException;

import newRede.ServerSocket2;

public class Server {
	public static void main(String[] args) throws IOException, InterruptedException {
//		FileInputStream in = new FileInputStream("rac2011.iso");
//		ServerSocket2 serverSocket = new ServerSocket2(3000,in);
//		newRede.Socket2 socket = serverSocket.accept();
//		FileInputStream in2 = new FileInputStream("Geeks.wmv");
//		ServerSocket2 serverSocket2 = new ServerSocket2(3001,in2);
//		newRede.Socket2 socket2 = serverSocket2.accept();
		FileInputStream in3 = new FileInputStream("Bastion.rar");
		ServerSocket2 serverSocket3 = new ServerSocket2(3002,in3);
		newRede.Socket2 socket3 = serverSocket3.accept();
		System.out.println("Encerrando...");
	}
}