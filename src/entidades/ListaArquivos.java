package entidades;

import java.io.Serializable;
import java.util.Vector;

public class ListaArquivos implements Serializable{

	private static final long serialVersionUID = 1L;
	private Vector<Arquivo> lista;

	public ListaArquivos(){
		this.lista = new Vector<Arquivo>();
	}

	public Vector<Arquivo> getLista(){
		return this.lista;
	}

	public void addArquivo(Arquivo arq){
		//System.out.println("Adicionando arquivo: " + arq.getNomeOriginal());
		this.lista.add(arq);
	}

	public Arquivo getByNomeOriginal(String nome){
		Arquivo retorno = null;
		for(int i = 0; i < this.lista.size(); i++){
			if(this.lista.get(i).getNomeOriginal().equals(nome)){
				retorno = this.lista.get(i);
				i = this.lista.size();
				//System.out.println("Arquivo com nome '" + nome + "' encontrado com sucesso.");
			}
		}
		return retorno;
	}

	public Arquivo getByHash(String hash){
		Arquivo retorno = null;
		for(int i = 0; i < this.lista.size(); i++){
			if(this.lista.get(i).getHash().equals(hash)){
				retorno = this.lista.get(i);
				i = this.lista.size();
				System.out.println("Arquivo com hash '" + hash + "' encontrado com sucesso.");
			}
		}
		return retorno;
	}

	public void removerByNomeOriginal(String nome){
		for(int i = 0; i < this.lista.size(); i++){
			if(this.lista.get(i).getNomeOriginal().equals(nome)){
				this.lista.remove(i);
				i = this.lista.size();
				System.out.println("Arquivo com nome '" + nome + "' removido com sucesso.");
			}
		}
	}

	public void removerByHash(String hash){
		for(int i = 0; i < this.lista.size(); i++){
			if(this.lista.get(i).getHash().equals(hash)){
				this.lista.remove(i);
				i = this.lista.size();
				System.out.println("Arquivo com hash '" + hash + "' removido com sucesso.");
			}
		}
	}

	
	public String listagem(){
		String retorno = "Listagem dos arquivos.\nTamanho : " + this.lista.size() + "Arquivos: \n";
		
		for (int i = 0; i < this.lista.size() ; i++) {
			retorno = retorno + "   " + i + "- " + this.lista.get(i).getCaminho() + " | " + this.lista.get(i).getLoginUploader() + " | " + this.lista.get(i).getHash() + "\n";
		}
		
		retorno = retorno + " ===== FIM DA LISTA =====";
		return retorno;
	}
	
	public Vector<Arquivo> getAllNotSynced(){
		Vector<Arquivo> retorno = new Vector<Arquivo>();
		for (int i = 0; i < this.lista.size(); i++) {
			if(!this.lista.get(i).getSyncStatus()){
				retorno.add(this.lista.get(i));
			}
		}
		return retorno;
	}
	
	public void setAllNotSynced(){
		for (int i = 0; i < this.lista.size(); i++) {
			this.lista.get(i).setSyncStatus(false);
		}
	}
}