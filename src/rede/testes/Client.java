package rede.testes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import rede.Pacote;
import rede.Socket;

public class Client {
	public static void main(String[] args) throws IOException {
		Socket socket = new Socket(3000, InetAddress.getByName("172.20.4.99"));

		FileOutputStream stream = new FileOutputStream("rac2011.iso");
		while(true){
			byte[] buffer = new byte[Pacote.default_size];
			int as_read = socket.read(buffer,0,buffer.length);
			if(as_read!=-1){
				if(as_read<Pacote.default_size){
					stream.write(buffer, 0, as_read);
					stream.flush();
				}else{
					stream.write(buffer);
					stream.flush();
				}
			}

		}
	}
}
