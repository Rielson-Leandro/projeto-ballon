package newRede.teste;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import newRede.ServerSocket;
import newRede.Socket;

public class Server {
	ServerSocket socket;
	Socket socket2;

	public Server() throws FileNotFoundException, IOException {
		socket = new ServerSocket(3000, new FileInputStream("rac2011.iso"));
		socket2 = socket.accept();
	}

}
