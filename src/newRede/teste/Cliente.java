package newRede.teste;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

import newRede.Socket;

public class Cliente {
		
	public static void main(String[] args) throws UnknownHostException, FileNotFoundException, IOException {
		Socket socket = new Socket(3000, InetAddress.getByName("172.20.4.99"), new FileOutputStream("rac2011.iso"));
		
	}
}
