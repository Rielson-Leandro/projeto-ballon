package testes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.TimerTask;

import rede.Pacote;

public class UPDTaskCliente {
	long velocidade;
	
	
	private class UPDCL extends TimerTask{
		DatagramSocket socket;
		byte[] data = new byte[Pacote.default_size];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		@Override
		public void run() {
			try {
				socket.receive(packet);
				velocidade += packet.getLength();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	private class Bandwidth extends TimerTask{

		double repVelo = 0;

		@Override
		public void run() {
			repVelo = (repVelo * 0.825) + ((velocidade / 1024)*0.175);
			velocidade = 0;
			System.out.println((int) repVelo + " Kb/s");
		}
	
	}
	
}
