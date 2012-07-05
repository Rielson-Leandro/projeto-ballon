package rede;

import java.net.DatagramSocket;
import java.net.DatagramSocketImpl;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;

public class newSocket extends DatagramSocket {

	public newSocket(SocketAddress bindaddr) throws SocketException {
		super(bindaddr);
		// TODO Auto-generated constructor stub
	}

	public newSocket(int port, InetAddress laddr) throws SocketException {
		super(port, laddr);
		// TODO Auto-generated constructor stub
	}

	public newSocket(int port) throws SocketException {
		super(port);
		// TODO Auto-generated constructor stub
	}

	protected newSocket(DatagramSocketImpl impl) {
		super(impl);
		// TODO Auto-generated constructor stub
	}

	public newSocket() throws SocketException {
		super();
		// TODO Auto-generated constructor stub
	}

}
