package network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;

import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;

public class EnviarArquivo {

	File arquivo_para_enviar;
	FileInputStream stream_arquivo_enviar;
	miniSocket socket;
	miniServerSocket miniServerSocket;
	long tamanho_arquivo;
	double porcentagem;
	double repVelo;
	long tempo_restante;

	public EnviarArquivo(String caminho_arquivo,int porta_envio) throws ArquivoNaoEncontradoException, ErroConexaoException{
		File arquivo = new File(caminho_arquivo);
		try{
			if(arquivo.isFile()){
				this.tamanho_arquivo = arquivo.length();
				stream_arquivo_enviar = new FileInputStream(arquivo);
				miniServerSocket = new miniServerSocket(porta_envio, stream_arquivo_enviar);
				socket = miniServerSocket.accept();
				new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000);
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
			porcentagem = socket.last_send.get()/tamanho_arquivo;
			
			tempo_restante = (long)((tempo_restante*0.125) + 0.8175*((tamanho_arquivo-socket.last_send.get())/repVelo));
		}
	}

	public double getPorcentagem(){
		return porcentagem;
	}
	
	public long gettTempoRestante(){
		return tempo_restante;
	}
	
	public double getVelocidade(){
		return repVelo;
		
	}
}
