package network.exception;

public class ErroConexaoException extends Exception{
	public ErroConexaoException(){
		super("Problema ao estabelecer conexao com a rede");
	}
}
