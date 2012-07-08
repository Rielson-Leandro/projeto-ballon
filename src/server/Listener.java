package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener extends Thread{

	private ServerSocket socket;
	private Server servidor;

	public Listener(Server srv){
		super();
		System.out.print("Inicializando Listener ... ");

		this.servidor = srv;

		try{
			this.socket = new ServerSocket(20000);
		}catch(IOException e){
			System.out.println("Falha ao criar socket na porta 20000");
		}

		System.out.println("Sucesso!");
	}

	private ServerSocket getServerDisponivel(){
		int base = 20100;
		int limite = 20200;
		ServerSocket retorno = null;
		boolean pronto = false;

		do{
			try{
				retorno = new ServerSocket(base);
				pronto = true;
			}catch(IOException e){
				base++;
				if(base > limite){
					pronto = true; // retorna null nesse caso
				}
			}
		}while(!pronto);

		return retorno;
	}
	
	public void run(){

		System.out.println("Listener em execucao.");

		while(true){

			try{

				System.out.println("Aguardando conexao ... ");
				Socket skt = this.socket.accept();
				Worker novoWorker = new Worker(skt, this.servidor, this.getServerDisponivel());
				novoWorker.start();

			}catch(IOException e){
				System.out.println("Falha ao caputrar o socket de conexão.");
			}

		}

	}

}