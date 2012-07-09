package client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import network.EnviarArquivo;
import network.exception.ErroConexaoException;

import entidades.Arquivo;
import entidades.ListaArquivos;
import entidades.Transfer;
import entidades.TransferMini;

public class Sincronizador extends Thread{

	private ListaArquivos lista;
	private Client cliente;
	private File[] listaFisica;
	private boolean firstTime;
	private DataOutputStream msgOutToServer;
	private BufferedReader msgInFromServer;
	private Socket skt;

	public Sincronizador(Client cliente, Socket socket){
		super();
		this.cliente = cliente;
		this.skt = socket;
		this.lista = this.cliente.getUser().getListaArquivos();
		this.firstTime = true;
		this.inicializadorCanaisMensagem();
	}

	private void inicializadorCanaisMensagem(){
		System.out.print("Iniciando canais da THREAD SYNCER ... ");
		try{
			this.msgOutToServer = new DataOutputStream(this.skt.getOutputStream());
			System.out.print("Saida OK ... ");

			try{
				this.msgInFromServer = new BufferedReader(new InputStreamReader(this.skt.getInputStream()));
				System.out.println("Entrada OK!");

			}catch(IOException e1){
				System.out.println("Entrada FAIL!");
			}
		}catch(IOException e){
			System.out.print("Saida FAIL ... ");
		}
	}

	private void compararDiretorioLista(){
		File[] listaDir = new File(this.cliente.getFilesDir()).listFiles();
		String[] nome;
		Arquivo temp = null;
		for (int i = 0; i < listaDir.length; i++) {

			nome = listaDir[i].getPath().replace(File.separatorChar, '#').split("#");
			temp = this.lista.getByNomeOriginal(nome[nome.length - 1]);

			if(temp != null){
				System.out.println(temp.isSyncing());
				if(!temp.isSyncing()){
					//se ta na lista e nao esta sincronizando  
					
					//verifica se foi modificado
					if(listaDir[i].lastModified() != temp.getUltimaModificacao() && !temp.isSyncing()){
						System.out.println("ARQUIVO NO DIR E NA LISTA, LAST MOD CHANGE");
						
						temp.setSyncStatus(false);
						temp.setSyncing(true);

						//se foi modificado, entao envia
						this.enviarArquivo(listaDir[i], temp);
						temp.setUltimaModificacao(listaDir[i].lastModified());

					}else{
						//se nao foi modificado, confirma a sincronizacao
						temp.setSyncStatus(true);
						temp.setSyncing(false);
					}
				}
			}else{
				//se nao ta na lista
				System.out.println("ARQUIVO NO DIR MAS NAO NA LISTA");
				//envia
				Arquivo arquivo = new Arquivo(this.cliente.getFilesDir() + nome[nome.length - 1], this.cliente.getUser().getLogin());
				if(!arquivo.isSyncing()){

					arquivo.setSyncing(true);
					arquivo.setSyncStatus(false);
					this.enviarArquivo(listaDir[i], arquivo);
					arquivo.setUltimaModificacao(listaDir[i].lastModified());
					this.cliente.getUser().getListaArquivos().addArquivo(arquivo);

				}
			}
			
			temp = null;
		}
	}

	private File getFileByName(File[] lista, String nome){
		File retorno = null;
		String[] temp = null;
		for (int i = 0; i < lista.length; i++) {
			temp = lista[i].getPath().replace(File.separatorChar, '#').split("#");
			if(nome.equals(temp[temp.length - 1])){
				retorno = lista[i];
				i = lista.length;
			}
		}
		return retorno;
	}

	private String getFileName(File arquivo){
		if(arquivo.isFile()){
			String[] temp = arquivo.getPath().replace(File.separatorChar, '#').split("#");
			return temp[temp.length - 1];
		}else{
			return null;
		}
	}

	private void compararListaDiretorio(){
		Vector<Arquivo> listNotSynced = this.lista.getAllNotSynced();
		for (int i = 0; i < listNotSynced.size(); i++) {
			//verifica se esta sincronizado(esta na lista mas nao esta no diretorio
			if(!listNotSynced.get(i).getSyncStatus() && !listNotSynced.get(i).isSyncing() && this.firstTime){
				//se nao estiver, solicita arquivo arquivo
				listNotSynced.get(i).setSyncing(true);
				this.solicitarArquivo(listNotSynced.get(i).getHash());
			}

			if(!listNotSynced.get(i).isSyncing() && !this.firstTime){
				System.out.println("DELETANDO arquivo " + listNotSynced.get(i).getCaminho());
				File temp = new File(listNotSynced.get(i).getCaminho());
				temp.delete();
				this.cliente.getUser().getListaArquivos().removerByNomeOriginal(listNotSynced.get(i).getNomeOriginal());
				this.cliente.sendMensagem("REMOVEFILE#" + listNotSynced.get(i).getHash());
			}
		}
	}

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

	private void enviarArquivo(File arquivoFisico, Arquivo arquivo){
		String caminho = arquivoFisico.getPath();
		String[] temp = caminho.replace(File.separatorChar, '#').split("#");
		Arquivo  arq = arquivo;
		//Socket transferSkt;

		System.out.println("ENVIANDO arquivo do servidor:\nNome original: " + temp[temp.length - 1] + "\nCodigo hash: " + (this.cliente.getUser().getLogin() + temp[temp.length - 1]).hashCode() );
		if(this.cliente.sendMensagem("GETREADY#" + this.cliente.getUser().getLogin() + "#" + arq.getCaminho() + "#" + arquivoFisico.length())){

			String[] msgPorta;
			boolean portaSelecionada = false;
			do{

				try{

					//le a msg de resposta do server para qual porta enviar o arquivo
					String lido = msgInFromServer.readLine();
					System.out.println(lido);
					msgPorta = lido.split("#");

					if( msgPorta[0].equals("SENDONPORT") && msgPorta[1].equals(this.cliente.getUser().getLogin()) && msgPorta[3].equals(arq.getHash()) ){

						TransferMini transfer = new TransferMini(arq);
						transfer.setSender(arq.getCaminho(), Integer.parseInt(msgPorta[2]), 0);
						transfer.start();
						
						/*try{
							transferSkt = new Socket(this.skt.getInetAddress().getHostAddress(), Integer.parseInt(msgPorta[2]));
							portaSelecionada = true;

							//incianado transferidor
							Transfer transferidor = new Transfer();
							transferidor.setSender(arq, this.cliente.getFilesDir() + arq.getNomeOriginal(), transferSkt, this.cliente.getRemainigDir());
							transferidor.start();
							arq.setUltimaModificacao(arquivoFisico.lastModified());

						}catch (IOException e) {
							System.out.println("FALHA ao iniciar o socket de transferencia para IP " + this.skt.getInetAddress().getHostAddress() + " e PORT: " + Integer.parseInt(msgPorta[2]) );
						}*/

					}

				}catch(IOException e){
					System.out.println("Falha ao ler mensagem de porta.");
				}

			}while( !portaSelecionada );
		}
	}

	private void solicitarArquivo(String hash){
		System.out.println("SOLICITANDO arquivo do servidor:\nNome original: " + this.lista.getByHash(hash).getNomeOriginal() + " ... Codigo hash: " +  this.lista.getByHash(hash).getHash() );	

		//ServerSocket socketReceptor = this.getServerDisponivel();

		int portaDisponivel = this.getPortaDisponivel();
		
		this.cliente.sendMensagem("SENDFILE#" + this.cliente.getUser().getLogin() + "#" + hash + "#" + portaDisponivel/*socketReceptor.getLocalPort()*/);
		
		Arquivo arq = this.lista.getByHash(hash);
		TransferMini transfer = new TransferMini(arq);
		transfer.setReciever(arq.getCaminho(), portaDisponivel, this.skt.getInetAddress(), arq.getTamanho(), false);
		transfer.start();
		
		/*try{
			Socket socketAceito = socketReceptor.accept();

			Transfer recpt = new Transfer();
			Arquivo novoArquivo = new Arquivo(this.lista.getByHash(hash).getCaminho(), this.cliente.getUser().getLogin());
			System.out.println("Criando arquivo em " + this.lista.getByHash(hash).getCaminho() + " para user " + this.cliente.getUser().getLogin());
			recpt.setReciever(socketAceito, novoArquivo, this.cliente.getFilesDir(), socketReceptor, true);
			recpt.start();

		}catch(IOException e){
			System.out.println("Falha ao capturar o socket para solicitacao do servidor.");
		}*/
	}

	public void run(){
		System.out.println("Thread SYNCER em execucao ... ");

		File pastaArquivos;

		while(this.cliente.isConnected()){

			//System.out.println("Iniciando varredura em " + this.cliente.getFilesDir());

			this.lista = this.cliente.getUser().getListaArquivos();

			pastaArquivos = new File(this.cliente.getFilesDir());

			//System.out.print("Listando arquivos ... ");
			this.listaFisica = pastaArquivos.listFiles();

			//System.out.print("Varrendo ... ");
			this.lista.setAllNotSynced();
			System.out.println(this.cliente.getUser().getListaArquivos().listagem());
			this.compararDiretorioLista();
			this.compararListaDiretorio();
			//System.out.println("Varredura finalizada!");

			if(this.firstTime){
				this.firstTime = false;
			}

			try{
				this.sleep(10000);
			}catch(InterruptedException e){
				System.out.println("Falha ao enviar a thread sincronizadora aguardar");
			}
		}

		System.out.println("SYNCER finalizada!");
	}
}