package network.util;
import java.io.Serializable;
import java.net.DatagramPacket;



@SuppressWarnings("serial")
public class Pacote implements Serializable {
	
	public DatagramPacket pkt;
	byte[] dados;
	boolean is_send;
	public static final int head_payload = 16;
	public static int default_size = 1492;
	public static int util_load = default_size-head_payload;
	public int dataLenth;
	public long send_time;
	
	
	public Pacote(DatagramPacket pkt, long send_time){
		this.pkt = pkt;
		this.is_send = false;
		this.send_time = send_time;
	}
	
	public Pacote(DatagramPacket pkt, long send_time,int dataLenth){
		this.pkt = pkt;
		this.send_time = send_time;
		this.dataLenth = dataLenth;
	}
	
	public Pacote(DatagramPacket pkt){
		this.pkt = pkt;
		this.is_send = false;
		this.send_time = -1;
	}
	
	public boolean isEnviado() {
		return is_send;
	}

	public void setEnviado(boolean enviado) {
		this.is_send = enviado;
	}


	public int getNumeroSequencia() {
		return OperacoesBinarias.extrairNumeroSequencia(this.pkt.getData());
	}

	public static int getTamanhoPadrao() {
		return default_size;
	}

	public byte[] getData() {
		return this.pkt.getData();
	}

	public void setSend_time(long new_send_time){
		this.send_time = new_send_time;
	}
	
	public Pacote setSend_time_and_getme(long new_send_time){
		this.send_time = new_send_time;
		return this;
	}
	
	public int getDataLentgh(){
		return this.dataLenth;
	}
	
	@Override
	public String  toString(){
		int seqNum = OperacoesBinarias.extrairNumeroSequencia(this.pkt.getData());
		return "["+seqNum+"]\nState: "+this.is_send;
	}
}
