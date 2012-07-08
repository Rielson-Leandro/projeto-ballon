package network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;

import rede.gui.Velocidade;

public class ReceberArquivo {
	File arquivo_para_enviar;
	FileOutputStream stream_arquivo_receber;
	miniSocket socket;
	long tamanho_arquivo;
	double porcentagem;
	double repVelo;
	long tempo_restante;
	boolean velocidade_zero = false;

	public ReceberArquivo(String caminho_arquivo, int porta_servidor_arquivo, InetAddress endereco_servidor_sarquivo,long tamanhoArquivo) throws IOException {
		socket = new miniSocket(porta_servidor_arquivo, endereco_servidor_sarquivo, stream_arquivo_receber);
		this.tamanho_arquivo = tamanhoArquivo;
		new Timer().scheduleAtFixedRate(new Bandwidth(), 1000, 1000); 
	}

	private class Bandwidth extends TimerTask{
		int contador_zeros;
		long ultimo_valor;

		@Override
		public void run() {
			long instant_velo = socket.last_receiverd.get()-this.ultimo_valor;
			this.ultimo_valor = socket.last_receiverd.get();
			if(instant_velo==0){
				contador_zeros++;
				if(contador_zeros%2==0){
					velocidade_zero = true;
					repVelo = (repVelo * 0.825) + ((instant_velo / 1024)*0.175);
				}
			}else{
				velocidade_zero = false;
				contador_zeros = 0;
				repVelo = (repVelo * 0.825) + ((instant_velo / 1024)*0.175);
			}

			porcentagem = socket.last_receiverd.get()/tamanho_arquivo;

			tempo_restante = (long)((tempo_restante*0.125) + 0.8175*((tamanho_arquivo-socket.last_receiverd.get())/repVelo));
		}
	}

	public double getPorcentagem(){
		return porcentagem;
	}

	public long gettTempoRestante(){
		return tempo_restante;
	}

	public double getVelocidade(){
		if(velocidade_zero)
			return 0;
		else
			return repVelo;

	}
}
