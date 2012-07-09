package network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;

public class EnviarArquivo {

	File arquivo_para_enviar;
	RandomAccessFile stream_arquivo_enviar;
	miniSocket socket;
	miniServerSocket miniServerSocket;
	long tamanho_arquivo;
	double porcentagem;
	double repVelo;
	double tempo_restante;
	double incremento = 0;
	
	public EnviarArquivo(String caminho_arquivo,int porta_envio,long seek) throws ArquivoNaoEncontradoException, ErroConexaoException{
		this.arquivo_para_enviar = new File(caminho_arquivo);
		try{
			if(this.arquivo_para_enviar.isFile()){
				this.tamanho_arquivo = this.arquivo_para_enviar.length();
				if(seek>0){
					this.incremento = seek/this.tamanho_arquivo;
				}
				stream_arquivo_enviar = new RandomAccessFile(this.arquivo_para_enviar, "r");
				stream_arquivo_enviar.seek(seek);
				miniServerSocket = new miniServerSocket(porta_envio, stream_arquivo_enviar);
				socket = miniServerSocket.accept();
				new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
			}else{
				throw new ArquivoNaoEncontradoException();
			}
		}catch(SocketException e){
			throw new ErroConexaoException();	
		} catch (FileNotFoundException e) {
			throw new ArquivoNaoEncontradoException();
		} catch (IOException e) {
			throw new ErroConexaoException();
		}
	}
	
	private class Bandwidth extends TimerTask{
		int contador_zeros;
		long ultimo_valor;
		
		@Override
		public void run() {
			
			if(porcentagem==100){
				this.cancel();
			}
			
			long instant_velo = socket.last_send.get()-this.ultimo_valor;
			this.ultimo_valor = socket.last_send.get();
			if(instant_velo==0){
				contador_zeros++;
				if(contador_zeros%2==0){
					repVelo = (repVelo * 0.825) + ((instant_velo / 1024)*0.175);
				}
			}else{
				contador_zeros = 0;
				repVelo = (repVelo * 0.825) + ((instant_velo / 1024)*0.175);
			}
			
			porcentagem = ((double)socket.last_send.get()/(double)tamanho_arquivo)*100;
			
			tempo_restante = (((tempo_restante*0.125) + 0.8175*((double)(tamanho_arquivo-socket.last_send.get())/1024)/repVelo));
			
			System.out.println(Thread.currentThread().getName());
		}
	}

	public double getPorcentagem(){
		return porcentagem+incremento;
	}
	
	public double gettTempoRestante(){
		return tempo_restante;
	}
	
	public double getVelocidade(){
		return repVelo;
	}
	
	public long get_ultimo_byte_enviado(){
		return socket.last_send.get();
	}
}
