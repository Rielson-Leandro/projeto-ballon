package entidades;

import java.io.Serializable;
import java.io.File;

public class Arquivo implements Serializable{

	private static final long serialVersionUID = 1L;
	private String nomeOriginal;
	private String[] caminho;
	private String loginUploader;
	private String hash;
	private boolean isSynced;
	private boolean isSyncing;
	private boolean isReady;
	private long ultimaMod;

	public Arquivo(String caminho, String uploader){
		this.caminho = caminho.replace(File.separatorChar, '#').split("#");
		this.nomeOriginal = this.caminho[this.caminho.length - 1];
		this.loginUploader = uploader;
		String temp = this.loginUploader + this.nomeOriginal;
		this.hash = Integer.toString(temp.hashCode());
		this.isSynced = false;
		this.isSyncing = false;
		this.isReady = false;
		File temp2 = new File(caminho);
		this.ultimaMod = temp2.lastModified();
	}

	public synchronized String getNomeOriginal(){
		return this.nomeOriginal;
	}

	public synchronized String getCaminho(){
		String temp = "";

		for(int i = 0; i < this.caminho.length; i++){
			if(i != this.caminho.length - 1){
				temp = temp + this.caminho[i] + File.separatorChar;
			}else{
				temp = temp + this.caminho[i];
			}
		}

		return temp;
	}

	public synchronized String getLoginUploader(){
		return this.loginUploader;
	}

	public synchronized String getHash(){
		return this.hash;
	}

	public synchronized boolean getSyncStatus(){
		return this.isSynced;
	}

	public synchronized boolean isSyncing() {
		return isSyncing;
	}

	public synchronized void setSyncing(boolean isSyncing) {
		this.isSyncing = isSyncing;
	}

	public synchronized boolean getReadyStatus(){
		return this.isReady;
	}

	public synchronized long getUltimaModificacao(){
		return this.ultimaMod;
	}

	public synchronized void setNomeOriginal(String nome){
		this.nomeOriginal = nome;
	}

	public synchronized void setCaminho(String caminho){
		this.caminho = caminho.replace(File.separatorChar, '#').split("#");
	}

	public synchronized void setLoginUploader(String uploader){
		this.loginUploader = uploader;
	}

	public synchronized void setHash(){
		String temp = this.loginUploader + this.nomeOriginal;

		this.hash = Integer.toString(temp.hashCode());
	}

	public synchronized void setUltimaModificacao(long mod){
		this.ultimaMod = mod;
	}

	public synchronized void setSyncStatus(boolean status){
		this.isSynced = status;
	}

	public synchronized void setReadyStatus(boolean status){
		this.isReady = status;
	}

}