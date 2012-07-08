package client;

import java.util.Scanner;

public class Ui{

	public static void main(String[] args) {

		Scanner in = new Scanner(System.in);
		Client client = new Client("localhost");
		String cmd = "";
		String login = "";
		String pw = "";
		boolean working = true;

		while(working){
			cmd = in.nextLine();

			if( cmd.equals("login") ){
				System.out.print("Login selecionado ...\nLogin: ");
				login = in.nextLine();
				System.out.print("Senha: ");
				pw = in.nextLine();

				client.login(login, pw);
			}

			if( cmd.equals("signup") ){
				System.out.print("Cadastro selecionado ...\nLogin: ");
				login = in.nextLine();
				System.out.print("Senha: ");
				pw = in.nextLine();

				client.signUp(login, pw);
			}

			if( cmd.equals("adicionar") ){
				System.out.print("Insira o caminho: ");
				String caminho = in.nextLine();
				client.adicionarArquivo(caminho);
			}

			if( cmd.equals("exit") ){
				client.disconnect();
				working = false;
			}

		}

	}
}