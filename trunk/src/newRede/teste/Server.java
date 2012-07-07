package newRede.teste;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import rede.Socket2;

import newRede.ServerSocket;
import newRede.Socket;

public class Server {
	ServerSocket socket;
	Socket socket2;
	
	class temp extends TimerTask{
		public temp(){
			
		}
		@Override
		public void run() {
			System.out.println("Threads rodando"+Thread.activeCount());
		}
	}
	
	public void tt(){
		new Timer().scheduleAtFixedRate(new temp(), 50, 10);
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ServerSocket server = new ServerSocket(3000, new FileInputStream("rac2011.iso"));
		newRede.Socket2 so = server.accept();
	}
}
