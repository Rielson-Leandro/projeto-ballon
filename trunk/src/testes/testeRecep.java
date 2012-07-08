package testes;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import network.ReceberArquivo;

public class testeRecep {
	public static void main(String[] args) throws UnknownHostException, IOException {
//		System.out.println(new File("Bastion.rar").length());
		ReceberArquivo arquivo = new ReceberArquivo("Bastion.rar", 3000, InetAddress.getByName("172.20.4.99"), 951127027, false);
	}
}
