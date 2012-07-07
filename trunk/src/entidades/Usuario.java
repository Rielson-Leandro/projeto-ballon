package entidades;

import java.io.Serializable;

public class Usuario implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String login;
	private String pw;
	private boolean conectado;
	private ListaArquivos filesList;;

	public Usuario(String novoLogin, String novoPw){
		this.login = novoLogin;
		this.pw = novoPw;
		this.conectado = false;
		this.filesList = new ListaArquivos();
	}

	public String getLogin(){
		return this.login;
	}

	public void setLogin(String novoLogin){
		this.login = novoLogin;
	}

	public String getPw(){
		return this.pw;
	}

	public void setPw(String novoPw){
		this.pw = novoPw;
	}

	public ListaArquivos getListaArquivos(){
		return this.filesList;
	}

	public void setListaArquivos(ListaArquivos novaLista){
		this.filesList = novaLista;
	}

	public synchronized boolean getConectado(){
		return this.conectado;
	}

	public synchronized void setConectado(boolean setCon){
		this.conectado = setCon;
	}


}