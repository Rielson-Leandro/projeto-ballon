package testes;

import network.EnviarArquivo;
import network.exception.ArquivoNaoEncontradoException;
import network.exception.ErroConexaoException;

public class TesteEnvio {
	public static void main(String[] args) throws ArquivoNaoEncontradoException, ErroConexaoException {
		EnviarArquivo envio = new EnviarArquivo("Bastion.rar", 3012, 0);
	}
}
