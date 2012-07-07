package testes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import rede.Pacote;

public class UPDTaskCliente {
	long velocidade;
	
	
	private class UPDCL extends TimerTask{
		DatagramSocket socket;
		byte[] data = new byte[Pacote.default_size];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		public UPDCL(DatagramSocket socket){
			this.socket = socket;
		}
		
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
	
	public void start() throws SocketException{
		new Timer().scheduleAtFixedRate(new UPDCL(new DatagramSocket(3000)), 0, 50);
		new Timer().schedule(new Bandwidth(), 1000, 1000);
	}
	
	public static void main(String[] args) throws SocketException {
		new UPDTaskCliente().start();
	}	
}
