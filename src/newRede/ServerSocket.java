package newRede;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Timer;

import rede.OperacoesBinarias;
import rede.Pacote;

public class ServerSocket extends Socket{
	boolean close;
	boolean conectado;
	DatagramSocket socket;
	
	
	public ServerSocket(int listerningPort,FileInputStream arquivo_enviar) throws IOException,FileNotFoundException{
		super();
		super.socket = new DatagramSocket(listerningPort){
			@Override
			synchronized public void send(DatagramPacket packet) throws IOException{ 
				super.send(packet);
			}
		};
		super.arquivo_envio = arquivo_enviar;
	}

	public Socket accept() throws IOException{

		DatagramPacket packet = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		super.socket.receive(packet);
		if(OperacoesBinarias.extrairSYN(packet.getData())){
			super.endereco_cliente = packet.getAddress();
			super.porta_cliente = packet.getPort();
			System.out.println("Nova solicitação de conexão");
			System.out.println("Endereço do cliente "+ packet.getAddress());
			System.out.println("Porta do cliente "+ packet.getPort());
			super.socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			super.socket.send(new DatagramPacket(SYN_ACK_BYTE, Pacote.head_payload,packet.getAddress(),packet.getPort()));
			new Timer().scheduleAtFixedRate(new Sender(), 0, 1);
			new Timer().scheduleAtFixedRate(new ReceiverAcks(), 0, 1);
			new Timer().scheduleAtFixedRate(new Timeout(), 100, 100);
		}
		System.out.println("Solicitação concluida");
		return this;
	}

	public void fechar(){

	}
}
