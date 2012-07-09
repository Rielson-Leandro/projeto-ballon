package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import network.ReceberArquivo;

import entidades.Arquivo;
import entidades.Transfer;
import entidades.TransferMini;
import entidades.Usuario;

public class Worker extends Thread{

	private boolean isConnected;
	private Socket socketClient;
	private ServerSocket loginSocket;
	private Server servidor;
	private BufferedReader fromClient;
	private DataOutputStream toCLient;
	private Usuario user;


	public Worker(Socket skt, Server srv, ServerSocket loginSkt){
		super();
		System.out.println("Inicializando worker para " + skt.getInetAddress().getHostAddress() + " ... ");
		
		this.loginSocket = loginSkt;
		this.isConnected = true;
		this.socketClient = skt;
		this.servidor = srv;

		try{
			this.fromClient = new BufferedReader(new InputStreamReader(this.socketClient.getInputStream()));
			this.toCLient = new DataOutputStream(this.socketClient.getOutputStream());
		}catch(IOException e){
			System.out.println("Falha ao criar os canais de comunicacao com o cliente " + this.socketClient.getInetAddress().getHostAddress());
		}

		System.out.println("Iniciazado com sucesso.");
	}

	private void checkConnection(){
		//System.out.print("Checando estado da conexao com " + this.socketClient.getInetAddress().getHostAddress() + " ... ");
		if(this.socketClient.isClosed()){
			isConnected = false;
		}else{
			isConnected = true;
		}
	}

	private void interpretador(String mensagem){

		//segmentacao no formato estabelecido
		String[] msg = mensagem.split("#");
		System.out.println("Interpretando mensagem: " + mensagem);

		if(msg[0].equals("LOGINRQST") || msg[0].equals("SIGNUP") || msg[0].equals("LOGOUT") ){
			System.out.println("Mensagem encaminhada para o gerenciador de usuarios...");
			this.gerenciamentoUsuarios(msg);
		}

		if(msg[0].equals("GETREADY") || msg[0].equals("SENDFILE") ){
			System.out.println("Mensagem encaminhada para o gerenciador de transferencias...");
			this.gerenciamentoTransferencias(msg);
		}

		if(msg[0].equals("REMOVEFILE") ){
			this.gerenciamentoArquivos(msg);
		}

		if(msg[0].equals("CONCHEK") ){
			System.out.println("Mensagem encaminhada para o gerenciador de conexao...");
			this.gerenciamentoConexao(msg);
		}

	}

	private void gerenciamentoUsuarios(String[] msg){
		if(msg[0].equals("LOGINRQST")){

			Usuario rqstUser = this.servidor.executeLogin2(msg[1], msg[2]);

			if( rqstUser != null ){
				System.out.println(msg[1] + " logado com SUCESSO!");

				this.user = rqstUser;

				System.out.println(this.user.getListaArquivos().listagem());
				try{

					this.toCLient.writeBytes("LOGINRQST#successful#" + this.loginSocket.getLocalPort() + "\n");

					System.out.println("Preparando pra enviar dados do usuario.");
					
					Socket lgnSkt = this.loginSocket.accept();
					DataOutputStream loginWriter = new DataOutputStream(lgnSkt.getOutputStream());
					
					File temp = new File(this.servidor.getUsersDir() + msg[1] + ".login");
					System.out.print("Carregando usuario ... ");
					FileInputStream fis = new FileInputStream(temp);
					int lidos = 0;
					byte[] buffer = new byte[262144];
					while(lidos != -1){
						lidos = fis.read(buffer);
						System.out.print("Dados user lidos: " + lidos + " ... ");
						if(lidos != -1){
							loginWriter.write(buffer, 0, lidos);
							System.out.print("Escritos ... ");
							loginWriter.flush();
							loginWriter.close();
							System.out.println("Enviados!");
						}
					}
					fis.close();
					this.loginSocket.close();
					System.out.println("Transferencia de usuario finalizada.");


				}catch(IOException e){
					System.out.println("FALHA ao enviar a mensagem de login bem sucedido para o cliente.");
				}

			}else{
				System.out.println("Requisicao de login invalida.");

				try{
					this.toCLient.writeBytes("LOGINRQST#failed\n");
				}catch(IOException e){
					System.out.println("Falha ao enviar a mensagem de login mal sucedida para o cliente.");
				}
			}
		}

		if(msg[0].equals("SIGNUP")){

			this.servidor.executeSignUp2(msg[1], msg[2]);

			try{ 
				this.toCLient.writeBytes("SIGNUP#successful\n");
			}catch(IOException e){
				System.out.println("Falha ao enviar a msg de cadastro bem sucedido para o cliente.");
			}
		}

		if(msg[0].equals("LOGOUT")){

			this.isConnected = false;
			this.servidor.saveUser(this.user);
		}
	}

	private void gerenciamentoArquivos(String[] msg){
		if(msg[0].equals("REMOVEFILE")){
			this.user.getListaArquivos().removerByHash(msg[1]);
			this.servidor.saveUserList();
			File temp = new File(this.servidor.getFilesDir() + msg[1] + ".file");
			temp.delete();
		}
	}

	private void gerenciamentoTransferencias(String[] msg){

		//GETREADY#user#caminhoOriginal#tamanhoArquivo
		if(msg[0].equals("GETREADY")){
			
			int portaDisponivel = this.getPortaDisponivel();
			Arquivo novoArquivo = new Arquivo(msg[2], msg[1]);
			
			try {
				System.out.println("SENDONPORT#" + msg[1] + "#" + portaDisponivel + "#" + novoArquivo.getHash());
				this.toCLient.writeBytes("SENDONPORT#" + msg[1] + "#" + portaDisponivel + "#" + novoArquivo.getHash() + "\n" );
				
				TransferMini transfer = new TransferMini(this.servidor.getRemainingDir());
				System.out.println("Recebendo : " + this.servidor.getFilesDir() + novoArquivo.getHash() + ".file");
				transfer.setReciever(this.servidor.getFilesDir() + novoArquivo.getHash() + ".file", portaDisponivel, this.socketClient.getInetAddress(), Integer.parseInt(msg[3]), false);
				System.out.println("Iniciando transferencia ... ");
				transfer.start();
				this.user.getListaArquivos().addArquivo(novoArquivo);
				
			} catch (IOException e) {
				System.out.println("FALHA ao enviar a msg de confirmacao de porta para o cliente.");
			}
			
			/*boolean userCorreto = false;

			ServerSocket socketFiles = this.getServerDisponivel();
			if(socketFiles != null){
				try{

					do{
						System.out.println("Aguardando conexao de socket de transferencia na porta " + socketFiles.getLocalPort() + " ... ");

						Arquivo novoArquivo = new Arquivo(msg[2], msg[1]);

						//envia a porta pra onde o cliente deve enviar o arquivo
						// SENDONPORT#user#porta#hash
						int portaDisponivel = this.getPortaDisponivel();
						this.toCLient.writeBytes("SENDONPORT#" + msg[1] + "#" + socketFiles.getLocalPort() + "#" + novoArquivo.getHash() + "\n" );
						
						Socket transferSocket = socketFiles.accept();

						if( this.socketClient.getInetAddress().getHostAddress().equals(transferSocket.getInetAddress().getHostAddress()) ){

							System.out.println("Conexao de transferencia estabelecida para " + transferSocket.getRemoteSocketAddress().toString());

							userCorreto = true;

							Transfer transferidor = new Transfer();
							transferidor.setReciever(transferSocket, novoArquivo, this.servidor.getFilesDir(), socketFiles, false);
							this.user.getListaArquivos().addArquivo(novoArquivo);
							this.servidor.saveUserList();
							transferidor.start();
						}
					}while(!userCorreto);

				}catch(IOException e){
					System.out.println("Falha ao capturar o socket de transferencia.");
				}
			}*/
		}

		if(msg[0].equals("SENDFILE")){
			
			TransferMini transfer = new TransferMini(this.servidor.getRemainingDir());
			transfer.setSender(this.servidor.getFilesDir() + this.user.getListaArquivos().getByHash(msg[2]).getHash() + ".file", Integer.parseInt(msg[3]), 0);
			transfer.start();
			
			/*boolean userCorreto = false;

			try{

				do{
					System.out.println("Aguardando conexao de socket de transferencia ... ");

					Socket transferSocket = new Socket( this.socketClient.getInetAddress().getHostAddress() , Integer.parseInt(msg[3]) );

					System.out.println("Comparando " + this.socketClient.getInetAddress().getHostAddress() + " <-> " + transferSocket.getInetAddress().getHostAddress());

					if( this.socketClient.getInetAddress().getHostAddress().equals(transferSocket.getInetAddress().getHostAddress()) && transferSocket.isConnected() ){

						userCorreto = true;

						Arquivo arquivoParaSerEnviado = this.user.getListaArquivos().getByHash(msg[2]);

						System.out.println("\nIniciando transferencia do arquivo:\nHash: " + arquivoParaSerEnviado.getHash() + "\nNome: " + arquivoParaSerEnviado.getNomeOriginal() + "\nCaminho: " + arquivoParaSerEnviado.getCaminho() + "\nUploader: " + arquivoParaSerEnviado.getLoginUploader() + "\n");

						Transfer transferidor = new Transfer();
						transferidor.setSender(arquivoParaSerEnviado, this.servidor.getFilesDir() + arquivoParaSerEnviado.getHash() + ".file", transferSocket, this.servidor.getRemainingDir());
						transferidor.start();
					}
				}while(!userCorreto);

			}catch(IOException e){
				System.out.println("Falha ao capturar o socket de transferencia.");
			}*/
		}
	}

	private void gerenciamentoConexao(String[] msg){}

	private ServerSocket getServerDisponivel(){
		int base = 30000;
		int limite = 30100;
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
	
	private int getPortaDisponivel(){
		return this.getServerDisponivel().getLocalPort();
	}

	public void run(){
		System.out.println("Worker para " + this.socketClient.getInetAddress().getHostAddress() + " em execucao.");

		String msgFromClient = "";

		while(isConnected){

			try{
				if(fromClient.ready()){

					try{
						msgFromClient = fromClient.readLine();

						this.interpretador(msgFromClient);
					}catch(IOException e1){
						System.out.println("Falha ao ler mensagens vindas do cliente " + this.socketClient.getInetAddress().getHostAddress());
					}

				}

				this.checkConnection();
				if(isConnected){
					this.sleep(100);
				}

			}catch(IOException e){
				System.out.println("Falha na verificacao de entrada de mensagens do cliente.");
			}catch(InterruptedException e){
				System.out.println("Falha ao enviar o worker de " + this.socketClient.getInetAddress().getHostAddress() + " para o aguardo temporario.");
			}

		}
	}
}

