package rede.testes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import rede.Pacote;
import rede.Socket;

public class Server {
	public static void main(String[] args) throws IOException, InterruptedException {
		Socket socket = new Socket(3000);
		File file = new File("rac2011.iso");
		FileInputStream in = new FileInputStream(file);
		boolean continua = true;

		while(in.available()>0){
			if(socket.buffer_avaliable()>0){
				int quantos_vai_ler = Math.max(socket.buffer_avaliable(), Pacote.util_load);
				byte[] buffer = new byte[quantos_vai_ler];
				int quantos_leu = in.read(buffer);
				socket.write(buffer, 0, quantos_leu);
			}else{
				Thread.sleep(1000);
			}
		}
		
		while(socket.get_last_send()<file.length()){
			Thread.sleep(1000);
		}
		socket.close();
		System.out.println("Encerrando...");
	}
}