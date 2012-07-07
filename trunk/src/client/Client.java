package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import entidades.Arquivo;
import entidades.Usuario;

public class Client{

	private String enderecoIp;
	private String mainDir;
	private String remainingDir;
	private String filesDir;
	private BufferedReader inFromServer;
	private DataOutputStream outToServer;
	private Socket socketToServer;
	private boolean connected;
	private Usuario usuario;
	private Sincronizador sync;

	public Client(){
		System.out.println("Inicializando cliente ... ");

		this.mainDir = "C:\\Users\\Pedro Neves\\Desktop\\Balloon\\";
		this.remainingDir = this.mainDir + "remaining" + File.separatorChar;
		this.filesDir = this.mainDir + "arquivos" + File.separatorChar;

		System.out.print("Inicializando pastas ... ");
		File temp = new File(this.remainingDir);
		temp.mkdirs();
		temp = new File(this.filesDir);
		temp.mkdirs();
		System.out.println("OK!");

		this.enderecoIp = "0.0.0.0";
		this.connected = false;
		System.out.println("Cliente inicializado.");
	}

	public Client(String end){
		System.out.println("Inicializando cliente ... ");

		this.mainDir = "C:\\Users\\Pedro Neves\\Desktop\\Balloon\\";
		this.remainingDir = this.mainDir + "remaining" + File.separatorChar;
		this.filesDir = this.mainDir + "arquivos" + File.separatorChar;

		System.out.print("Inicializando pastas ... ");
		File temp = new File(this.remainingDir);
		temp.mkdirs();
		temp = new File(this.filesDir);
		temp.mkdirs();
		System.out.println("OK!");

		this.enderecoIp = end;
		this.connected = false;
		this.connect();
		this.inicializadorCanaisMensagem();
		System.out.println("Cliente inicializado.");
	}

	private void inicializadorCanaisMensagem(){
		System.out.print("Iniciando canais ... ");
		try{
			this.outToServer = new DataOutputStream(this.socketToServer.getOutputStream());
			System.out.print("Saida OK ... ");

			try{
				this.inFromServer = new BufferedReader(new InputStreamReader(this.socketToServer.getInputStream()));
				System.out.println("Entrada OK!");

			}catch(IOException e1){
				System.out.println("Entrada FAIL!");
			}
		}catch(IOException e){
			System.out.print("Saida FAIL ... ");
		}
	}

	public synchronized boolean sendMensagem(String msg){
		boolean retorno = false;
		try{
			this.outToServer.writeBytes(msg + "\n");
			System.out.println("Mensagem enviada com sucesso.");

			try{
				this.outToServer.flush();
				retorno = true;
			}catch(IOException e1){
				System.out.println("Falha ao enviar dados do canal de saida.");
			}
		}catch(IOException e){
			System.out.println("Falha ao exscrever dados do canal de saida.");
		}

		return retorno;

	}

	public void setEnderecoIp(String novoEnd){
		this.enderecoIp = novoEnd;
		System.out.println("Tentando conectar em " + this.enderecoIp);
		this.connect();
	}

	public String getMainDir(){
		return this.mainDir;
	}

	public void setMainDir(String newMainDir){
		this.mainDir = newMainDir;
	}

	public String getRemainigDir(){
		return this.remainingDir;
	}

	public void setRemainingDir(String newRemainingDir){
		this.remainingDir = this.mainDir + newRemainingDir;
	}

	public String getFilesDir(){
		return this.filesDir;
	}

	public void setFilesDir(String newFilesDir){
		this.filesDir = this.mainDir + newFilesDir;
	}

	public boolean isConnected(){
		return this.connected;
	}

	public Usuario getUser(){
		return this.usuario;
	}

	public void connect(){
		if(!this.enderecoIp.equals("0.0.0.0")){
			try{
				this.socketToServer = new Socket(this.enderecoIp, 20000);
				this.connected = true;
				System.out.println("Conectado!");
			}catch(UnknownHostException e){
				System.out.println("Host nao encontrado.");
			}catch(IOException e){
				System.out.println("Falha na conexao com servidor.");
			}
		}else{
			System.out.println("Conexao falhou.\nEndereco de IP nao definido.");
		}
	}

	public void login(String login, String senha){
		String msg = "LOGINRQST#" + login + "#" + senha;

		System.out.println("Enviando requisicao de login...");
		try{
			this.outToServer.writeBytes(msg + "\n");
			System.out.println("Mensagem enviada com sucesso.");

			try{
				this.outToServer.flush();
			}catch(IOException e3){
				System.out.println("Falha ao enviar dados do canal de saida.");
			}

			boolean gotResponse = false;
			while(!gotResponse){
				if(this.inFromServer.ready()){
					try{
						String[] resposta = this.inFromServer.readLine().split("#");
						gotResponse = true;

						if(resposta[0].equals("LOGINRQST")){

							if(resposta[1].equals("successful")){
								this.connected = true;
								System.out.println("Login ACEITO!");

								System.out.println("Carregando informacoes do usuario ... ");
								try{
									ObjectInputStream userReader = new ObjectInputStream(this.socketToServer.getInputStream());

									try{
										this.usuario = (Usuario) userReader.readObject();
										this.usuario.getListaArquivos().setAllNotSynced();
										System.out.println(this.usuario.getListaArquivos().listagem());
									}catch(ClassNotFoundException e){
										System.out.println("Falha de classe nao encontrada!");
									}
									System.out.println("Informacoes do usuario carregadas.");

									System.out.println("Iniciando sincronizacao ... ");
									this.sync = new Sincronizador(this, this.socketToServer);
									this.sync.start();
								}catch (IOException e) {
									System.out.println("FALHA ao inicializar o leitor de usuarios.");
								}

							}else{
								System.out.println("Login INVALIDO!");
							}
						}
					}catch(IOException e4){
						System.out.println("Nenhuma mensagem para ser lida.");
					}
				}
			}
		}catch(IOException e2){
			System.out.println("Falha ao enviar mensagem de requisicao de login.");
		}
	}

	public void signUp(String login, String senha){
		String msg = "SIGNUP#" + login + "#" + senha;

		System.out.println("Enviando requisicao de cadastro...");
		try{
			this.outToServer.writeBytes(msg + "\n");
			System.out.println("Mensagem enviada com sucesso.");

			try{
				this.outToServer.flush();
			}catch(IOException e3){
				System.out.println("Falha ao enviar dados do canal de saida.");
			}

			boolean gotResponse = false;
			while(!gotResponse){
				if(this.inFromServer.ready()){
					try{
						String[] resposta = this.inFromServer.readLine().split("#");
						gotResponse = true;

						if(resposta[0].equals("SIGNUP")){

							if(resposta[1].equals("successful")){
								System.out.println("cadastro ACEITO!");
							}else{
								System.out.println("cadastro INVALIDO!");
							}
						}
					}catch(IOException e4){
						System.out.println("Nenhuma mensagem para ser lida.");
					}
				}
			}
		}catch(IOException e2){
			System.out.println("Falha ao enviar mensagem de requisicao de cadastro.");
		}
	}

	public void adicionarArquivo(String caminho){
		//o parametro caminho indica todo o path ate o arquivo incluindo o nome do arquivo, juntamente com a extensao.
		
		String[] temp = caminho.replace(File.separatorChar, '#').split("#"); 
		
		//transfere o arquivo para a pasta de compartilhamento
		Arquivo file = new Arquivo(this.filesDir + temp[temp.length - 1], this.usuario.getLogin());
		TransferidorArquivo transfer = new TransferidorArquivo(file, caminho);
		System.out.println("Transferindo arquivo para pasta ... ");
		//adiciona o arquivo na lista do usuario
		this.usuario.getListaArquivos().addArquivo(file);
		transfer.start();
	}

	public void disconnect(){
		System.out.print("Desconectando ... ");
		this.connected = false;
		try{
			this.outToServer.close();
			this.inFromServer.close();
			System.out.println("DESCONECTADO!");
		}catch(IOException e){
			System.out.println("FAIL!");
		}
	}
}

class TransferidorArquivo extends Thread{

	private Arquivo original;
	private String caminhoFonte;
	private File fonte;
	private File destino;

	TransferidorArquivo(Arquivo destino, String fonte){
		this.original = destino;
		this.caminhoFonte = fonte;
		this.fonte = new File(fonte);
		this.destino = new File(destino.getCaminho());

		this.destino.setReadable(false);
		this.destino.setWritable(true);

		try{
			this.destino.createNewFile();
		}catch(IOException e){
			System.out.println("Falha ao criar o arquivo de destino na transferencia.");
		}

	}

	public void run(){

		try{
			FileInputStream leitor = new FileInputStream(this.fonte);
			FileOutputStream escritor = new FileOutputStream(this.destino);
			int bytesLidos = 0;
			byte[] buffer = new byte[10485760];
			while(bytesLidos != -1){
				try{

					bytesLidos = leitor.read(buffer);
					if(bytesLidos != -1){

						try{
							escritor.write(buffer, 0, bytesLidos);
						}catch(IOException e2){
							System.out.println("Falha ao escrever o arquivo de destino para copia.");
						}

					}

				}catch(IOException e1){
					System.out.println("Falha ao ler o arquivo de fonte para copia.");
				}
			}

			try{
				escritor.close();
				leitor.close();
			}catch(IOException e){
				System.out.println("Falha ao fechar os arquivos.");
			}

		}catch(FileNotFoundException e){
			System.out.println("Arquivos nao encontrados.");
		}

		this.original.setUltimaModificacao(this.destino.lastModified());
		this.destino.setReadable(true);
		this.original.setReadyStatus(true);
		this.original.setSyncStatus(false);

		System.out.println("Arquivo " + this.destino.getPath() + " adicionado.");

	}
}