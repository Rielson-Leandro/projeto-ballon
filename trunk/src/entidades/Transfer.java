package entidades;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

public class Transfer extends Thread{

	//Thread referente a uma transferencia. Nesse caso, refere-se a transferencia do cliente para o servidor.
	//Esse transferidor mantem um arquivo auxiliar que contem dados do estado da transferencia.

	private boolean sender;
	private boolean reciever;
	private FileOutputStream outDados;
	private byte[] buffer;
	private TransferState estado;
	private Arquivo arq;
	private Socket socket;
	private ServerSocket original;
	private boolean stayAlive;
	private boolean rcvIsClient;

	public Transfer(){
		super();
		this.buffer = new byte[262144];
		this.stayAlive = true;
		this.rcvIsClient = false;
	}

	public void setSender(Arquivo arquivo, String pathFileToTransfer, Socket skt, String caminhoPastaEstados){
		this.sender = true;
		this.reciever = false;

		this.socket = skt;
		this.estado = new TransferState(pathFileToTransfer, caminhoPastaEstados);
		this.arq = arquivo;
		this.setUpTransferStateFile();
	}

	public void setReciever(Socket skt, Arquivo arquivo, String fileDir, ServerSocket original, boolean isClient){
		this.sender = false;
		this.reciever = true;
		this.rcvIsClient = isClient;

		System.out.println("Inicializando transferidor de arquivo para" + skt.getRemoteSocketAddress().toString());

		this.socket = skt;
		this.original = original;

		this.arq = arquivo;

		if(isClient){
			System.out.print("Criando arquivo " + this.arq.getNomeOriginal() + ".file ... ");
			File temp = new File(fileDir + this.arq.getNomeOriginal());
			
			try{
				temp.createNewFile();
				System.out.println("Criado.");
			}catch(IOException e){
				System.out.println("Falha.");
			}

			System.out.print("Inicializando canais de comunicacao ... ");
			try{
				this.outDados = new FileOutputStream(temp);
				System.out.println("Canais criados.\nTransferidor de arquivo inicializado.");
			}catch(IOException e){
				System.out.println("Falha.");
			}
			
		}else{
			
			System.out.print("Criando arquivo " + this.arq.getHash() + ".file ... ");
			File temp = new File(fileDir + this.arq.getHash() + ".file");
			
			try{
				temp.createNewFile();
				System.out.println("Criado.");
			}catch(IOException e){
				System.out.println("Falha.");
			}

			System.out.print("Inicializando canais de comunicacao ... ");
			try{
				this.outDados = new FileOutputStream(temp);
				System.out.println("Canais criados.\nTransferidor de arquivo inicializado.");
			}catch(IOException e){
				System.out.println("Falha.");
			}
			
		}
	}

	private void setUpTransferStateFile(){
		int numOrdem = 0;

		boolean correto = false;

		do{

			File novoRem = new File(this.estado.getStateFileFolder() + File.separatorChar + numOrdem + ".rem");

			if(!novoRem.exists()){

				this.estado.setStateFileName(numOrdem+"");

				correto = true;

				try{
					novoRem.createNewFile();
					FileOutputStream fos = new FileOutputStream(novoRem);
					try{
						ObjectOutputStream out = new ObjectOutputStream(fos);
						try{

							out.writeObject(this.estado);
							out.close();
							fos.close();
						}catch(IOException e2){
							System.out.println("Falha ao escrever o arquivo de estado para o arquivo: " + this.estado.getPath());
						}
					}catch(IOException e1){
						System.out.println("Falha ao criar a saida de OBJETOS para o arquivo: " + this.estado.getPath());
					}
				}catch(IOException e){
					System.out.println("Falha ao criar o canal de saida para o arquivo: " + this.estado.getPath());
				}

			}else{
				numOrdem++;
			}

		}while(!correto);


	}

	private void atualizarEstado(){
		this.estado.ackBlockSent();

		File temp = new File(this.estado.getStateFileFolder() + File.separatorChar + this.estado.getStateFileName() + ".rem");

		try{
			FileOutputStream fos = new FileOutputStream(temp);

			try{
				ObjectOutputStream out = new ObjectOutputStream(fos);

				try{

					out.writeObject(this.estado);
					out.close();
					fos.close();
				}catch(IOException e2){
					System.out.println("Falha ao escrever atualizacao do estado.");
				}
			}catch(IOException e1){
				System.out.println("Falha ao estabelcer conexao para OBJETOS para atualizar estado.");
			}
		}catch(IOException e){
			System.out.println("Falha ao estabelcer conexao para atualizar estado.");
		}
	}

	private void deleteState(){
		File temp = new File(this.estado.getStateFileFolder() + File.separatorChar + this.estado.getStateFileName() + ".rem");
		temp.delete();
	}

	private boolean checkConnection(){
		if(this.socket.isClosed()){
			return false;
		}else{
			return true;
		}
	}

	private void setStayAlive(boolean valor){
		this.stayAlive = valor;
	}

	public void run(){

		if(this.sender){

			System.out.println("Conectado...");
			File arquivo = new File(this.estado.getPath());
			System.out.println("Arquivo pronto para ser transferido.");

			System.out.println("Iniciando canais de comunicacao para: " + this.estado.getPath());
			FileInputStream fileIn;
			DataOutputStream dataOut;
			try {

				fileIn = new FileInputStream(arquivo);

				try {

					dataOut = new DataOutputStream(this.socket.getOutputStream());

					int lidos = 0;
					long total = 0;
					do{
						if(this.stayAlive){
							try{

								lidos = fileIn.read(this.buffer);
								total = total + lidos;

								System.out.println("Bytes lidos: " + lidos + " | total: " + total);
								try{
									if(lidos != -1){

										dataOut.write(this.buffer, 0, lidos);
										this.atualizarEstado();

										try{
											this.sleep(30);
										}catch(InterruptedException e4){
											System.out.println("Falha ao enviar a o transfer aguardar a conexao.");
										}
									}

								}catch(IOException e2){
									System.out.println("Falha ao escrever dados.");
								}
							}catch(IOException e1){
								System.out.println("Falha ao ler dados.");

							}
						}

						this.setStayAlive(this.checkConnection());

					}while(lidos != -1 && this.stayAlive);

					System.out.println("Transferencia concluida: Fechando canais de comunicacao...");

					this.socket.close();
					fileIn.close();

				} catch (IOException e) {
					System.out.println("FALHA ao criar o canal de saida.");
				}
			} catch (FileNotFoundException e) {
				System.out.println("O arquivo " + this.estado.getPath() + " nao foi encontrado.");
			}

			this.arq.setSyncing(false);
			this.arq.setSyncStatus(true);

			this.deleteState();
		}


		if(this.reciever){

			System.out.println("Transferidor para " + this.socket.getRemoteSocketAddress().toString() + " em execucao ...");

			System.out.print("Inicializando canais de comunicacao ... ");
			try{

				DataInputStream entradaDados = new DataInputStream(this.socket.getInputStream());
				DataOutputStream saidaDados = new DataOutputStream(this.outDados);
				System.out.println("Canais criados.\nTransferidor de arquivo inicializado.");

				int lidos = 0;
				this.arq.setSyncing(true);
				while(lidos != -1 && this.stayAlive){
					try{
						lidos = entradaDados.read(buffer);

						try{
							if(lidos > 0){
								saidaDados.write(this.buffer, 0, lidos);
								saidaDados.flush();
							}
						}catch(IOException e2){
							System.out.println("Falha ao escrever dados no arquivo recebido.");
						}

					}catch(IOException e1){
						System.out.println("Falha ao ler dados da entrada da transferencia.");
					}

					this.setStayAlive(this.checkConnection());

				}

				try{
					entradaDados.close();
					saidaDados.close();
				}catch(IOException e){
					System.out.println("Falha ao fechar os canais de transferencia.");
				}

				this.arq.setReadyStatus(true);

				System.out.println("Transferencia concluida.");

				this.socket.close();
				this.original.close();
				
				if(this.rcvIsClient){
					this.arq.setSyncing(false);
					this.arq.setSyncStatus(true);
				}

			}catch(IOException e){
				System.out.println("Falha.");
			}

		}

		System.out.println("Transferidor finalizado!.");

	}

}

class TransferState implements Serializable{

	//Essa classe tem como objetivo manter informaçoes de estado acerca das transferencias realizadas DO CLIENTE PARA O SERVIDOR.
	//Um arquivo do tipo remaining (.rem) eh mantido em uma pasta exclusiva no lado do cliente. Sempre que houver um arquivo desse tipo nessa pasta determinada, é uma pendendcia que o cliente precisa envia para servidor.
	//As informaçoes mantidas nesse arquivo (.rem) sao referentes a uma transferencia em especial, e sao atualizadas sempre que possivel.


	private static final long serialVersionUID = 1L;
	private String stateFileFolder; //armazena o endereco do local onde deverao ser postos os arquivos de estado
	private String stateFileName;
	private String path;
	private long tamanhoArquivo;
	private long blocosEnviados;
	private long quantidadeTotalBlocos;

	TransferState(String caminho, String caminhoPastaArmazenamento){
		this.path = caminho;
		File arquivo = new File(this.path);
		this.tamanhoArquivo = arquivo.length();
		arquivo = null;
		this.stateFileFolder = caminhoPastaArmazenamento;

		//dividindo temporariamente em blocos de 256kb
		this.quantidadeTotalBlocos = (int) (this.tamanhoArquivo / 262144);
		if( this.tamanhoArquivo % 262144 > 0 ){
			this.quantidadeTotalBlocos++;
		}
	}

	public String getPath(){
		return this.path;
	}

	public void setPath(String caminho){
		this.path = caminho;
	}

	public String getStateFileFolder(){
		return this.stateFileFolder;
	}

	public String getStateFileName(){
		return this.stateFileName;
	}

	public void setStateFileName(String nome){
		this.stateFileName = nome;
	}

	public void ackBlockSent(){
		this.blocosEnviados++;
	}

	public long quantidadeRestante(){
		return (this.tamanhoArquivo - (262144*this.blocosEnviados));
	}

	public String toString(){
		return "Estado para arquivo: " + this.path + " tamanho: " + this.tamanhoArquivo + " total blocos: " + this.quantidadeTotalBlocos + " blocos enviados: " + this.blocosEnviados + " restando: " + this.quantidadeRestante();
	}

}