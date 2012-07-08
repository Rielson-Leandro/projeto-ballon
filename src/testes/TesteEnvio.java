package testes;

import network.EnviarArquivo;
import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;

public class TesteEnvio {
	public static void main(String[] args) throws ArquivoNaoEncontradoException, ErroConexaoException, InterruptedException {
		EnviarArquivo arquivo = new EnviarArquivo("Bastion.rar", 3000, 0);
		
		while(arquivo.getPorcentagem()<1){
			Thread.sleep(1000);
			System.out.println("--------------------------------------");
			System.out.println("Concluido: "+arquivo.getPorcentagem());
			System.out.println("Tempo restante: "+ arquivo.gettTempoRestante());
			System.out.println("Velocidade: "+ arquivo.getVelocidade());
			System.out.println("--------------------------------------");
		}
		
	}
}
