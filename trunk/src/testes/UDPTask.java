package testes;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import rede.Pacote;

public class UDPTask {

	private class UDP extends TimerTask{
		FileInputStream stream;
		DatagramSocket socket;
		byte[] data = new byte[Pacote.default_size];
		
		public UDP(FileInputStream arquivo, DatagramSocket socket){
			this.stream = arquivo;
			this.socket = socket;
		}

		@Override
		public void run(){
			try {
				
				if(stream.available()>0){
					boolean parar = false;
					for (int i = 0; i < 64 && !parar; i++) {
						int leu = stream.read(data);
						if(leu>0){
							DatagramPacket packet = new DatagramPacket(data, leu, InetAddress.getByName("172.20.4.85"), 3000);
							socket.send(packet);
						}else{
							parar = true;
						}
					}
					
				}else{
					stream.close();
					this.cancel();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void start() throws FileNotFoundException, SocketException{
		new Timer().scheduleAtFixedRate(new UDP(new FileInputStream("rac2011.iso"), new DatagramSocket()), 0, 50);
	}
	public static void main(String[] args) throws FileNotFoundException, SocketException {
		new UDPTask().start();
	}
}
