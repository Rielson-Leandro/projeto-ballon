package rede;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class newSocket extends DatagramSocket {

	//variaveis
	InetAddress endereco_cliente;
	InetAddress endereco_servidor;
	
	int porta_cliente;
	int porta_servidor;
	
	boolean conectado;
	
	byte[] SYN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0};
	byte[] SYN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,0};
	byte[] FIN_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1};
	byte[] FIN_ACK_BYTE = {0,0,0,0,0,0,0,0,0,0,0,0,1,0,0,1};
	//variaveis
		
	
	//usado pelo cliente para indicar criar um socket para o servidor
	public newSocket(int porta_servidor, InetAddress endereco_servidor) throws IOException {
		super();
		this.porta_servidor = porta_servidor;
		this.endereco_servidor = endereco_servidor;
		DatagramPacket receiver = new DatagramPacket(new byte[Pacote.head_payload], Pacote.head_payload);
		while(!conectado){
			send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			send(new DatagramPacket(SYN_BYTE, Pacote.head_payload, endereco_servidor, porta_servidor));
			receive(receiver);
			if(receiver.getAddress().equals(endereco_servidor) && receiver.getPort()==porta_servidor){
				conectado=true;
			}
		}
		
	}

	//usado pelo servidor para ficar escutando na porta especifica
	public newSocket(int port) throws SocketException {
		super(port);
	}
		
	public void setCliente(int portaCliente, InetAddress enderecoCliente){
		this.endereco_cliente = enderecoCliente;
		this.porta_cliente = portaCliente;
	}
}
