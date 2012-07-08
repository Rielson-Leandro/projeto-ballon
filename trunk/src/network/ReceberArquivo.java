package network;

import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;

import rede.gui.Velocidade;

public class ReceberArquivo {
	File arquivo_para_enviar;
	FileOutputStream stream_arquivo_receberr;
	miniSocket socket;
	miniServerSocket miniServerSocket;
	Velocidade grafico_velocide;
	long tamanhoArquivo;
	double porcentagem;
	double repVelo;
	long tempo_restante;
	
	public ReceberArquivo(String caminho_arquivo, int porta_servidor_arquivo, InetAddress endereco_servidor_sarquivo) {
		
	}
	
}
