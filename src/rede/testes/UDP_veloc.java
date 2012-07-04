package rede.testes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import rede.Pacote;

public class UDP_veloc {
	AtomicLong velocidade = new AtomicLong(0);

	public void mede() throws IOException{
		DatagramSocket tempSocket = new DatagramSocket(50000);
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
		while(true){
			byte[] dados = new byte[Pacote.default_size];
			DatagramPacket rec = new DatagramPacket(dados, dados.length);
			tempSocket.receive(rec);
			System.out.println(rec);
			velocidade.addAndGet(rec.getLength());
		}

	}

	private class Bandwidth extends TimerTask{

		double repVelo = 0;

		@Override
		public void run() {
			repVelo = (repVelo * 0.825) + ((velocidade.getAndSet(0) / 1024)*0.175);

			System.out.println((int) repVelo + " Kb/s");

		}

	}
	
	public static void main(String[] args) throws IOException {
		new UDP_veloc().mede();
		
	}

}
