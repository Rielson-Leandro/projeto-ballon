package network;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.TimerTask;

import javax.swing.JFrame;

import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;
import rede.gui.Velocidade;

public class EnviarArquivo {

	File arquivo_para_enviar;
	FileInputStream stream_arquivo_enviar;
	miniSocket socket;
	miniServerSocket miniServerSocket;
	Velocidade grafico_velocide;
	long tamanhoArquivo;
	double porcentagem;
	double repVelo;
	long tempo_restante;

	public EnviarArquivo(String caminho_arquivo,int porta_envio) throws ArquivoNaoEncontradoException, ErroConexaoException{
		File arquivo = new File(caminho_arquivo);
		try{
			if(arquivo.isFile()){
				this.tamanhoArquivo = arquivo.length();
				stream_arquivo_enviar = new FileInputStream(arquivo);
				miniServerSocket = new miniServerSocket(porta_envio, stream_arquivo_enviar);
				this.rodarGUI();
			}


		}catch(SocketException e){
			throw new ErroConexaoException();	
		} catch (FileNotFoundException e) {
			throw new ArquivoNaoEncontradoException();
		} catch (IOException e) {
			throw new ErroConexaoException();
		}
	}

	protected void rodarGUI(){
		grafico_velocide = new Velocidade();
		grafico_velocide.setBounds(300, 300, 300, 200);
		grafico_velocide.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void iniciarTransferencia() throws IOException{
		socket = miniServerSocket.accept();
	}

	public void mostrar_graficos(){
		grafico_velocide.setVisible(true);
	}

	public void esconder_grafico(){
		grafico_velocide.setVisible(true);
	}

	public void matar_grafico(){
		grafico_velocide.setEnabled(false);
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
					Velocidade.setText(0 + " Kb/s");
				}
			}else{
				contador_zeros = 0;
				repVelo = (repVelo * 0.825) + ((instant_velo / 1024)*0.175);
				grafico_velocide.setText((int) repVelo + " Kb/s");
			}
			porcentagem = socket.last_send.get()/tamanhoArquivo;
			tempo_restante = (long) ((tamanhoArquivo-socket.last_send.get())/repVelo);
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
