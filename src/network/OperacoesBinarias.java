package network;

public class OperacoesBinarias {

	//ok!
	public static  void intParaByte(byte[] retorno,int inteiro,int posicao,boolean isHalf, boolean isByte){ //Convertendo inteiro para array de bytes

		if(posicao>=0){
			if(isByte){
				if(posicao<retorno.length){
					retorno[posicao] = (byte) (inteiro  & 0xFF);
				}
			}else if(isHalf){
				if(posicao<(retorno.length-2)){
					retorno[posicao] = (byte) (inteiro  & 0xFF);
					retorno[posicao+1] = (byte) ((inteiro>>8)  & 0xFF);
				}
			}else{
				if(posicao<(retorno.length-4)){
					retorno[posicao] = (byte) (inteiro  & 0xFF);
					retorno[posicao+1] = (byte) ((inteiro>>8)  & 0xFF);
					retorno[posicao+2] = (byte) ((inteiro>>16)  & 0xFF);
					retorno[posicao+3] = (byte) ((inteiro>>24)  & 0xFF);
				}
			}
		}		
	}

	//ok!
	public static int bytesParaInt (byte[] numero,int posicao,boolean isHalf, boolean isByte){
		int retorno = 0; //retorno default
		if(posicao >=0){
			if(isByte){
				if(posicao<(numero.length))
				return  (numero[posicao] & 0xFF);
			}else if(isHalf){
				if(posicao<(numero.length-2))
				return  (numero[posicao+1] & 0xFF)<<8 | (numero[posicao] & 0xFF);
			}else{
				if(posicao<(numero.length-4))
				return  (numero[posicao+3] & 0xFF) << 24 | (numero[posicao+2] & 0xFF) << 16 | (numero[posicao+1] & 0xFF)<<8 | (numero[posicao] & 0xFF); //Fazendo o processo inverso de intParabyte
			}

		}
		return retorno;
	}

	//ok!
	public static byte[] inserirCabecalho(byte[]dados,int numeroSequencia, int numeroReconhecimento,boolean ACK, boolean RST,boolean SYN, boolean FIN,int comprimentoDados,int janelaRecepcao ){
		intParaByte(dados,numeroSequencia,0,false,false);
		intParaByte(dados, numeroReconhecimento, 4,false,false);
		intParaByte(dados, comprimentoDados, 8, true, false);
		intParaByte(dados, janelaRecepcao, 10, true, false);
		intParaByte(dados, ACK?1:0, 12, false, true);
		intParaByte(dados, RST?1:0, 13, false, true);
		intParaByte(dados, SYN?1:0, 14, false, true);
		intParaByte(dados, FIN?1:0, 15, false, true);
		return dados;
	}

	//ok!
	public static int extrairNumeroSequencia(byte[] data){
		return bytesParaInt(data, 0, false, false);
	}

	//ok!
	public static int extrairNumeroReconhecimento(byte[] data){
		return bytesParaInt(data, 4, false, false);
	}

	//ok!
	public static boolean extrairACK(byte[] data){
		if(bytesParaInt(data, 12, false, true)==1)
			return true;
		else
			return false;
	}

	//ok!
	public static boolean extrairRST(byte[] data){
		if(bytesParaInt(data, 13, false, true)==1)
			return true;
		else
			return false;
	}

	//ok!
	public static boolean extrairSYN(byte[] data){
		if(bytesParaInt(data, 14, false, true)==1)
			return true;
		else
			return false;
	}

	//ok!
	public static boolean extrairFIN(byte[] data){
		if(bytesParaInt(data, 15, false, true)==1)
			return true;
		else
			return false;
	}

	//ok!
	public static int extrairComprimentoDados(byte[] data){
		if(data!=null){
			return bytesParaInt(data, 8, true, false);
		}else{
			return 0;
		}
		
	}

	//ok!
	public static int extrairJanelaRecepcao(byte[] data){
		return bytesParaInt(data,10, true, false);
	}

}
