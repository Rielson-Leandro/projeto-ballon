package rede.testes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Scanner;

import rede.Pacote;
import rede.ServerSocket;


public class teste {

	public class Writer implements Runnable {
		rede.Socket socket;
		Scanner in;
		public Writer(rede.Socket socket){
			this.socket = socket;
			in = new Scanner(System.in);
		}

		@Override
		public void run() {
			while(true){
				while(true){
					
					try {
						FileInputStream stream = new FileInputStream("rac2011.iso");
						byte[] data = new byte[Math.max(socket.buffer_avaliable(), Pacote.default_size)];
						int leu = stream.read(data);
						if(leu>0 && socket.buffer_avaliable()>0){
						socket.write(data, 0, leu);
						}else{
							Thread.sleep(2000);
						}
					} catch (IOException e1) {
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

	}

	public class Receiver implements Runnable{
		rede.Socket socket;
		
		public Receiver(rede.Socket socket){
			this.socket = socket;
		}
		
		@Override
		public void run() {
			try {
				FileOutputStream stream = new FileOutputStream("bytes.rar");
				while(true){

					byte[] to_string = new byte[Pacote.default_size];
					int leu = socket.read(to_string, 0, to_string.length);
					if(leu!=-1){
						stream.write(to_string, 0, leu);
					}
				}

			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}



	private void kk() throws IOException{
		FileInputStream arquivo_enviar = new FileInputStream("rac2011.iso");
		FileOutputStream arquivo_receber = new FileOutputStream("bytes.rar");
		rede.ServerSocket serverSocket = new ServerSocket(3001);
		rede.Socket socket = serverSocket.accept();
		//		Receiver receiver = new Receiver(socket);
//		new Thread(new Receiver(socket)).start();
//		new Thread(new Writer(socket)).start();
	}

	public static void main(String[] args) throws IOException {
		new teste().kk();
	}
}
