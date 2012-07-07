package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import entidades.ListaArquivos;
import entidades.ListaUsuarios;
import entidades.Usuario;

public class Server{

	private String mainDir;
	private String fileDir;
	private String usersDir;
	private String remainingDir;
	private boolean isNewUserList;
	private boolean isNewFileList;
	private ListaUsuarios userL;
	private ListaArquivos fileL;
	private ObjectInputStream fromUsersList;
	private ObjectInputStream fromFileList;
	private ObjectOutputStream toUserList;
	private ObjectOutputStream toFileList;
	private Listener listener;

	public Server(){
		System.out.println("");
		System.out.println("Inicializando servidor.");

		this.mainDir = "C:\\Users\\Pedro Neves\\Desktop\\BalloonServer\\";
		this.usersDir = this.mainDir + "users" + File.separatorChar;
		this.fileDir = this.mainDir + "arquivos" + File.separatorChar;
		this.remainingDir = this.fileDir + "remaining" + File.separatorChar;
		System.out.println("Servidor configurado.");

		System.out.println("Criando pastas ... ");
		File temp;
		temp = new File(this.usersDir);
		temp.mkdirs();
		temp = new File(this.fileDir);
		temp.mkdirs();
		temp = new File(this.remainingDir);
		temp.mkdirs();
		System.out.println("Pastas criadas.");

		System.out.println("Setando arquivos de configuracao ... ");
		//inicializando flags de novas listas como falsa por default
		this.isNewFileList = false;
		this.isNewUserList = false;
		temp = new File(this.usersDir + "list.userl");
		if(temp.exists()){
			System.out.println("Arquivo de lista de usuarios ja criado. Ignorando ... ");
			this.getUserList();
		}else{
			System.out.print("Criando arquivo de lista de usuarios ... ");
			try{
				temp.createNewFile();
				this.isNewUserList = true;
				this.userL = new ListaUsuarios();
				this.saveUserList();
				System.out.println("Sucesso!");
			}catch(IOException e){
				System.out.println("Falha!");
			}
		}

		temp = new File(this.fileDir + "list.filel");
		if(temp.exists()){
			System.out.println("Arquivo de lista de arquivos ja criada. Ignorando ... ");
			this.getFileList();
		}else{
			System.out.print("Criando arquivo de lista de arquivos ... ");
			try{
				temp.createNewFile();
				this.isNewFileList = true;
				this.fileL = new ListaArquivos();
				this.saveFileList();
				System.out.println("Sucesso!");
			}catch(IOException e){
				System.out.println("Falha!");
			}
		}
		temp = null;
		System.out.println("Arquivos de configuracao setados.");

		this.listener = new Listener(this);
		this.listener.start();
	}

	public synchronized void saveUserList(){
		try{
			FileOutputStream fos = new FileOutputStream(this.usersDir + "list.userl");

			try{

				this.toUserList = new ObjectOutputStream(fos);
				try{
					this.toUserList.writeObject(this.userL);
					this.toUserList.close();
				}catch(IOException e2){
					System.out.println("Falha ao salvar lista de usuarios.");
				}

			}catch(IOException e1){
				System.out.println("Falha ao estabelcer conexao para OBJETOS para atualizar lista de usuarios.");
			}

		}catch(IOException e){
			System.out.println("Falha ao estabelcer conexao para atualizar lista de usuarios.");
		}
	}

	public synchronized void getUserList(){
		try{
			FileInputStream fos = new FileInputStream(this.usersDir + "list.userl");

			try{

				this.fromUsersList = new ObjectInputStream(fos);
				try{
					this.userL = (ListaUsuarios) this.fromUsersList.readObject();
					this.fromUsersList.close();
				}catch(IOException e2){
					System.out.println("Falha ao ler lista de usuarios.");
				}catch(ClassNotFoundException e3){
					System.out.println("Erro de classe.");
				}

			}catch(IOException e1){
				System.out.println("Falha ao estabelcer conexao para OBJETOS para ler lista de usuarios.");
			}

		}catch(IOException e){
			System.out.println("Falha ao estabelcer conexao para ler lista de usuarios.");
		}
	}

	private void saveFileList(){
		try{
			FileOutputStream fos = new FileOutputStream(this.fileDir + "list.filel");

			try{

				this.toFileList = new ObjectOutputStream(fos);
				try{
					this.toFileList.writeObject(this.fileL);
					this.toFileList.close();
				}catch(IOException e2){
					System.out.println("Falha ao salvar lista de arquivos.");
				}

			}catch(IOException e1){
				System.out.println("Falha ao estabelcer conexao para OBJETOS para atualizar lista de arquivos.");
			}

		}catch(IOException e){
			System.out.println("Falha ao estabelcer conexao para atualizar lista de arquivos.");
		}
	}

	private void getFileList(){
		try{
			FileInputStream fos = new FileInputStream(this.fileDir + "list.filel");

			try{

				this.fromFileList = new ObjectInputStream(fos);
				try{
					this.fileL = (ListaArquivos) this.fromFileList.readObject();
					this.fromFileList.close();
				}catch(IOException e2){
					System.out.println("Falha ao ler lista de arquivos.");
				}catch(ClassNotFoundException e3){
					System.out.println("Erro de classe.");
				}

			}catch(IOException e1){
				System.out.println("Falha ao estabelcer conexao para OBJETOS para ler lista de arquivos.");
			}

		}catch(IOException e){
			System.out.println("Falha ao estabelcer conexao para ler lista de arquivos.");
		}
	}

	public synchronized Usuario executeLogin(String login, String senha){
		Usuario user = this.userL.getUsuarioPorLogin(login);
		if( (user != null) && (user.getPw().equals(senha)) ){
			return user;
		}else{
			return null;
		}
	}

	public synchronized void executeSignUp(String login, String senha){
		Usuario novoUser = new Usuario(login, senha);
		this.userL.adicionarUsuario(novoUser);
		this.saveUserList();
	}

	public String getFilesDir(){
		return this.fileDir;
	}

	public String getRemainingDir(){
		return this.remainingDir;
	}

}