package newRede;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;

import rede.OperacoesBinarias;
import rede.Pacote;

public class ServerSocket extends Socket2{
	boolean close;
	boolean conectado;
	DatagramSocket socket;
	
	
	public ServerSocket(int listerningPort,FileInputStream arquivo_enviar) throws IOException,FileNotFoundException{
		super();
		super.real_socket = new DatagramSocket(listerningPort){
			@Override
			synchronized public void send(DatagramPacket packet) throws IOException{ 
				super.send(packet);
			}
		};
		super.setArquivo_enviado(arquivo_enviar);
	}

	public Socket2 accept() throws IOException{

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		super.real_socket.receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			super.client_adress = packet.getAddress();
			super.client_port = packet.getPort();
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("Porta do cliente "+ packet.getPort());
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			super.real_socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			new Timer().scheduleAtFixedRate(new AndarJanela(), 50, 50);
			new Thread(new Receiver()).start();
			new Thread(new Sender()).start();
		}
		System.out.println("Solicitação concluida");
		return this;
	}

	public void fechar(){

	}
}
