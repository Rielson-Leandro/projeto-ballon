package network.exception;

import java.io.FileNotFoundException;

public class ArquivoNaoEncontradoException extends FileNotFoundException {
	public ArquivoNaoEncontradoException() {
		super("N�o foi possivel encontrar o arquivo no caminho passado");
	}
}
