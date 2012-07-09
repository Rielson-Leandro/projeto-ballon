package entidades;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;

import network.EnviarArquivo;
import network.ReceberArquivo;
import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;

public class TransferMini extends Thread{

	private boolean isClient;
	private boolean isSender;
	private boolean isReciever;
	private Arquivo arquivo;
	private EnviarArquivo sender;
	private ReceberArquivo reciever;
	private String caminhoArquivo;
	private String remainingDir;
	private int portaTransferencia;
	private EstadoTransferencia transferStateFile;

	//campos sender
	private long seek;

	//campos reciever
	private InetAddress endereco;
	private long tamanhoArquivo;
	private boolean isAppend;

	public TransferMini(String remDir){
		this.isClient = false;
		this.remainingDir = remDir;
		this.setUpTransferStateFile();
	}

	public TransferMini(Arquivo arq, String remDir) {
		this.arquivo = arq;
		this.isClient = true;
		this.remainingDir = remDir;
		this.setUpTransferStateFile();
	}

	private void setUpTransferStateFile(){
		//selecionar o numero de ordem
		int numOrdem = 0;
		boolean selecionado = false;
		File remFile;
		do{
			System.out.println("buscando rem");
			remFile = new File(this.remainingDir + numOrdem + ".rem");
			if(remFile.exists()){
				numOrdem++;
			}else{
				selecionado = true;
			}
		}while(!selecionado);
		
		try {
			remFile.createNewFile();
		} catch (IOException e) {
			System.out.println("FALHA ao criar o arquivo de estado " + numOrdem + ".rem");
		}
		this.transferStateFile = new EstadoTransferencia(this.remainingDir, numOrdem);
		
		//salva os dados iniciais
		try{
			FileOutputStream fos = new FileOutputStream(remFile);
			try{
				ObjectOutputStream out = new ObjectOutputStream(fos);
				try{
					out.writeObject(this.transferStateFile);
					out.close();
					fos.close();
				}catch (Exception e) {
					System.out.println("FALHA escrever dados iniciais para o arquivo de estado " + numOrdem + ".rem");
				}
			}catch (IOException e) {
				System.out.println("FALHA ao criar o escritor de dados para o arquivo de estado " + numOrdem + ".rem");
			}
		}catch (IOException e) {
			System.out.println("FALHA ao criar o canal de saida de dados para o arquivo de estado " + numOrdem + ".rem");
		}
	}
	
	private void saveTransferStateFile(){
		//atualiza os dados do arquivo de estado
		File remFile = new File(this.remainingDir + this.transferStateFile.getNumOrdem() + ".rem");
		try{
			FileOutputStream fos = new FileOutputStream(remFile);
			try{
				ObjectOutputStream out = new ObjectOutputStream(fos);
				try{
					out.writeObject(this.transferStateFile);
					out.close();
					fos.close();
				}catch (Exception e) {
					System.out.println("FALHA escrever dados de atualizacao para o arquivo de estado " + this.transferStateFile.getNumOrdem() + ".rem");
				}
			}catch (IOException e) {
				System.out.println("FALHA ao criar o escritor de dados de atualizacao para o arquivo de estado " + this.transferStateFile.getNumOrdem() + ".rem");
			}
		}catch (IOException e) {
			System.out.println("FALHA ao criar o canal de saida de dados de atualizacao para o arquivo de estado " + this.transferStateFile.getNumOrdem() + ".rem");
		}
	}
	
	public void setSender(String caminho_arquivo,int porta_envio,long seek){
		this.isSender = true;
		this.isReciever = false;

		this.caminhoArquivo = caminho_arquivo;
		this.portaTransferencia = porta_envio;
		this.seek = seek;
	}

	public void setReciever(String caminho_arquivo, int porta_servidor_arquivo, InetAddress endereco_servidor_sarquivo,long tamanhoArquivo, boolean continuacao_arquivo){
		this.isSender = false;
		this.isReciever = true;

		this.caminhoArquivo = caminho_arquivo;
		this.portaTransferencia = porta_servidor_arquivo;
		this.endereco = endereco_servidor_sarquivo;
		this.tamanhoArquivo = tamanhoArquivo;
		this.isAppend = continuacao_arquivo;
	}

	public void run(){

		if(this.isSender){

			try {
				this.sender = new EnviarArquivo(this.caminhoArquivo, this.portaTransferencia, this.seek);

				while(this.sender.getPorcentagem() < 100){
					System.out.println("Enviados: " + (int) this.sender.getPorcentagem() + "%");
					this.saveTransferStateFile();
					try {
						this.sleep(1000);
					} catch (InterruptedException e) {
						System.out.println("FALHA ao enviar a thread de envio para sleep.");
					}
				}

				if(this.isClient){
					this.arquivo.setSyncing(false);
					this.arquivo.setSyncStatus(true);
				}

			} catch (ArquivoNaoEncontradoException e) {
				System.out.println("FALHA ao enviar o arquivo " + this.caminhoArquivo + " ... Arquivo nao encontrado.");
			} catch (ErroConexaoException e) {
				System.out.println("FALHA de conexao ao enviar " + this.caminhoArquivo);
			}	
		}

		if(this.isReciever){

			try {
				this.reciever = new ReceberArquivo(this.caminhoArquivo, this.portaTransferencia, this.endereco, this.tamanhoArquivo, this.isAppend);
				System.out.println("Passou pelo handshake.");
				while(this.reciever.getPorcentagem() < 100){
					System.out.println("Recebidos: " + (int) this.reciever.getPorcentagem() + "%");
					this.saveTransferStateFile();
					try {
						this.sleep(1000);
					} catch (InterruptedException e) {
						System.out.println("FALHA ao enviar a thread de envio para sleep.");
					}
				}

				if(isClient){
					this.arquivo.setSyncing(false);
					this.arquivo.setSyncStatus(true);
					File temp = new File(this.caminhoArquivo);
					this.arquivo.setUltimaModificacao(temp.lastModified());
				}

			} catch (IOException e1) {
				System.out.println("FALHA ao receber arquivo em " + this.caminhoArquivo);
			}

		}

		System.out.println("Finalizando transferidor.");

	}

}


class EstadoTransferencia implements Serializable{

	private static final long serialVersionUID = 1L;
	private long lastSentByte;
	private String caminhoArquivoParaEnviar;
	private int numOrdem;
	
	public EstadoTransferencia(String caminho, int numOrdem){
		this.caminhoArquivoParaEnviar = caminho;
		this.numOrdem = numOrdem;
	}

	public long getLastSentByte() {
		return lastSentByte;
	}

	public void setLastSentByte(long lastSentByte) {
		this.lastSentByte = lastSentByte;
	}

	public String getCaminho() {
		return caminhoArquivoParaEnviar;
	}

	public void setCaminho(String caminho) {
		this.caminhoArquivoParaEnviar = caminho;
	}

	public int getNumOrdem() {
		return numOrdem;
	}

	public void setNumOrdem(int numOrdem) {
		this.numOrdem = numOrdem;
	}
	
}