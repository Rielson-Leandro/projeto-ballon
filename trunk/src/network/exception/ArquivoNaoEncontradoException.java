package network.exception;

import java.io.FileNotFoundException;

public class ArquivoNaoEncontradoException extends FileNotFoundException {
	public ArquivoNaoEncontradoException() {
		super("Não foi possivel encontrar o arquivo no caminho passado");
	}
}
