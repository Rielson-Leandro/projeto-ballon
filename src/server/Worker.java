package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import entidades.Arquivo;
import entidades.Transfer;
import entidades.Usuario;

public class Worker extends Thread{

	private boolean isConnected;
	private Socket socketClient;
	private Server servidor;
	private BufferedReader fromClient;
	private DataOutputStream toCLient;
	private Usuario user;


	public Worker(Socket skt, Server srv){
		super();
		System.out.println("Inicializando worker para " + skt.getInetAddress().getHostAddress() + " ... ");

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
			System.out.println("DESCONECTADO");
			isConnected = false;
		}else{
			//System.out.println("CONECTADO");
			isConnected = true;
		}
	}

	private void interpretador(String mensagem){

		//segmentacao no formato estabelecido
		String[] msg = mensagem.split("#");
		System.out.println("Interpretando mensagem: " + mensagem);

		switch(msg[0]){
			case "LOGINRQST":
			case "SIGNUP":
				System.out.println("Mensagem encaminhada para o gerenciador de usuarios...");
				this.gerenciamentoUsuarios(msg);
				break;
			case "GETREADY":
			case "SENDFILE":
				System.out.println("Mensagem encaminhada para o gerenciador de transferencias...");
				this.gerenciamentoTransferencias(msg);
				break;
			case "REMOVEFILE":
				this.gerenciamentoArquivos(msg);
				break;
			case "CONCHEK":
				System.out.println("Mensagem encaminhada para o gerenciador de conexao...");
				this.gerenciamentoConexao(msg);
				break;
			default:
				System.out.println("Mensagem invalida!");
		}

	}

	private void gerenciamentoUsuarios(String[] msg){
		if(msg[0].equals("LOGINRQST")){
			System.out.println("Processando requisicao de login:\nLogin: " + msg[1] +"\nSenha: " + msg[2]);
			Usuario rqstUser = this.servidor.executeLogin(msg[1], msg[2]);
			if( rqstUser != null ){
				System.out.println(msg[1] + " logado com SUCESSO!\nEnviando mensagem de resposta.");

				this.user = rqstUser;
				
				System.out.println(this.user.getListaArquivos().listagem());
				try{
					this.toCLient.writeBytes("LOGINRQST#successful\n");

					ObjectOutputStream out = new ObjectOutputStream(this.socketClient.getOutputStream());
					System.out.println("Enviando usuario ... ");
					out.writeObject(rqstUser);
					System.out.println("Enviado ... ");
					out.flush();
					System.out.println("Transferencia concluida.");


				}catch(IOException e){
					System.out.println("Falha ao enviar a mensagem de login bem sucedido para o cliente.");
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
			System.out.println("Processando requisicao de cadastro:\nLogin: " + msg[1] + "\nSenha: " + msg[2]);
			this.servidor.executeSignUp(msg[1], msg[2]);
			System.out.println("Cadastro realizado ... Enviando resposta ...");

			try{ 
				this.toCLient.writeBytes("SIGNUP#successful\n");
			}catch(IOException e){
				System.out.println("Falha ao enviar a msg de cadastro bem sucedido para o cliente.");
			}
		}
	}

	private void gerenciamentoArquivos(String[] msg){
		if(msg[0].equals("REMOVEFILE")){
			this.user.getListaArquivos().removerByHash(msg[1]);
			File temp = new File(this.servidor.getFilesDir() + msg[1]);
			temp.delete();
		}
	}

	private void gerenciamentoTransferencias(String[] msg){

		//GETREADY#user#caminhoOriginal
		if(msg[0].equals("GETREADY")){
			boolean userCorreto = false;

			ServerSocket socketFiles = this.getServerDisponivel();
			if(socketFiles != null){
				try{

					do{
						System.out.println("Aguardando conexao de socket de transferencia na porta " + socketFiles.getLocalPort() + " ... ");

						Arquivo novoArquivo = new Arquivo(msg[2], msg[1]);

						//envia a porta pra onde o cliente deve enviar o arquivo
						// SENDONPORT#user#porta#hash
						this.toCLient.writeBytes("SENDONPORT#" + msg[1] + "#" + socketFiles.getLocalPort() + "#" + novoArquivo.getHash() + "\n" );

						Socket transferSocket = socketFiles.accept();

						String[] endClientOriginal =  this.socketClient.getRemoteSocketAddress().toString().split(":");
						String[] endClientNovo = transferSocket.getRemoteSocketAddress().toString().split(":");

						System.out.println("Comparando " + endClientOriginal[0] + " <-> " + endClientNovo[0]);

						if( endClientOriginal[0].equals(endClientNovo[0]) ){
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
			}
		}

		if(msg[0].equals("SENDFILE")){
			boolean userCorreto = false;

			try{

				do{
					System.out.println("Aguardando conexao de socket de transferencia ... ");

					String[] endClientOriginal =  this.socketClient.getRemoteSocketAddress().toString().split(":");

					Socket transferSocket = new Socket( endClientOriginal[0].substring(1) , Integer.parseInt(msg[3]) );

					String[] endClientNovo = transferSocket.getRemoteSocketAddress().toString().split(":");

					System.out.println("Comparando " + endClientOriginal[0].substring(1) + " <-> " + endClientNovo[0].substring(1));

					if( endClientOriginal[0].equals(endClientNovo[0]) && transferSocket.isConnected() ){

						userCorreto = true;

						Arquivo arquivoParaSerEnviado = this.user.getListaArquivos().getByHash(msg[2]);
						
						System.out.println("\nIniciando transferencia do arquivo:\nHash: " + arquivoParaSerEnviado.getHash() + "\nNome: " + arquivoParaSerEnviado.getNomeOriginal() + "\nCaminho: " + arquivoParaSerEnviado.getCaminho() + "\nUploader: " + arquivoParaSerEnviado.getLoginUploader() + "\n");

						Transfer transferidor = new Transfer();
						transferidor.setSender(arquivoParaSerEnviado, this.servidor.getFilesDir() + arquivoParaSerEnviado.getHash(), transferSocket, this.servidor.getRemainingDir());
						transferidor.start();
					}
				}while(!userCorreto);

			}catch(IOException e){
				System.out.println("Falha ao capturar o socket de transferencia.");
			}
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

