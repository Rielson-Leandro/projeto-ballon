package entidades;

import java.io.Serializable;
import java.util.Vector;

public class ListaUsuarios implements Serializable{

	private static final long serialVersionUID = 1L;
	private Vector<Usuario> lista;

	public ListaUsuarios(){
		this.lista = new Vector<Usuario>();
	}

	public void adicionarUsuario(Usuario user){
		this.lista.add(user);
	}

	public void removerUsuarioPorLogin(String login){
		for(int i = 0; i < this.lista.size(); i++){
			if( this.lista.get(i).getLogin().equals(login) ){
				this.lista.remove(i);
				i = this.lista.size();
			}
		}
	}

	public Usuario getUsuarioPorLogin(String login){
		Usuario retorno = null;

		for(int i = 0; i < this.lista.size(); i++){
			if( this.lista.get(i).getLogin().equals(login) ){
				retorno = this.lista.get(i);
				i = this.lista.size();
			}
		}

		return retorno;
	}

	public boolean existeUsuarioPorLogin(String login){
		if( (this.getUsuarioPorLogin(login)).equals(null) ){
			return false;
		}else{
			return true;
		}
	}

}