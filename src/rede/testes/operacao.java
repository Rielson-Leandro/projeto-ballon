package rede.testes;

import rede.OperacoesBinarias;

public class operacao {
	public static void main(String[] args) {
		byte[] t = new byte[16];
		System.out.println("SYN");
		OperacoesBinarias.inserirCabecalho(t, 0, 0, false, false, true, false, 0, 0);
		System.out.print("[");
		for (int i = 0; i < t.length; i++) {
			System.out.print(t[i]+",");
		}
		System.out.print("]");
		System.out.println();
		System.out.println("SYN+ACK");
		OperacoesBinarias.inserirCabecalho(t, 0, 0, true, false, true, false, 0, 0);
		System.out.print("[");
		for (int i = 0; i < t.length; i++) {
			System.out.print(t[i]+",");
		}
		System.out.print("]");
		System.out.println();
		System.out.println("FIN");
		OperacoesBinarias.inserirCabecalho(t, 0, 0, false, false, false, true, 0, 0);
		System.out.print("[");
		for (int i = 0; i < t.length; i++) {
			System.out.print(t[i]+",");
		}
		System.out.print("]");
		System.out.println();
		System.out.println("FIN+ACK");
		OperacoesBinarias.inserirCabecalho(t, 0, 0, true, false, false, true, 0, 0);
		System.out.print("[");
		for (int i = 0; i < t.length; i++) {
			System.out.print(t[i]+",");
		}
		System.out.print("]");
	}
}
