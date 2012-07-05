package rede;

import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class newSocket extends DatagramSocket {

	//variaveis
	InetAddress endereco_cliente;
	InetAddress endereco_servidor;
	int porta_cliente;
	int porta_servidor;
	//variaveis
	
	

	//usado pelo cliente para indicar criar um socket para o servidor
	public newSocket(int porta_servidor, InetAddress endereco_Servidor) throws SocketException {
		super();
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
