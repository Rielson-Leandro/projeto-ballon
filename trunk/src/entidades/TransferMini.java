package entidades;

import java.io.IOException;
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
	private int portaTransferencia;
	
	//campos sender
	private long seek;
	
	//campos reciever
	private InetAddress endereco;
	private long tamanhoArquivo;
	private boolean isAppend;
	
	public TransferMini(){
		this.isClient = false;
	}
	
	public TransferMini(Arquivo arq) {
		this.arquivo = arq;
		this.isClient = true;
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
					System.out.println("Enviados: " + this.sender.getPorcentagem() + "%");
					try {
						this.sleep(1000);
					} catch (InterruptedException e) {
						System.out.println("FALHA ao enviar a thread de envio para sleep.");
					}
				}
				
				this.arquivo.setSyncing(false);
				this.arquivo.setSyncStatus(true);
				
			} catch (ArquivoNaoEncontradoException e) {
				System.out.println("FALHA ao enviar o arquivo " + this.caminhoArquivo + " ... Arquivo nao encontrado.");
			} catch (ErroConexaoException e) {
				System.out.println("FALHA de conexao ao enviar " + this.caminhoArquivo);
			}	
		}
		
		if(this.isReciever){
			
			try {
				this.reciever = new ReceberArquivo(this.caminhoArquivo, this.portaTransferencia, this.endereco, this.tamanhoArquivo, this.isAppend);
				
				while(this.sender.getPorcentagem() < 100){
					System.out.println("Recebidos: " + this.sender.getPorcentagem() + "%");
					try {
						this.sleep(1000);
					} catch (InterruptedException e) {
						System.out.println("FALHA ao enviar a thread de envio para sleep.");
					}
				}
				
				this.arquivo.setSyncing(false);
				this.arquivo.setSyncStatus(true);
				
			} catch (IOException e1) {
				System.out.println("FALHA ao receber arquivo em " + this.caminhoArquivo);
			}
			
		}
		
	}
	
}
