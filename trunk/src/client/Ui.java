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

			switch(cmd){
				case "login":
					System.out.print("Login selecionado ...\nLogin: ");
					login = in.nextLine();
					System.out.print("Senha: ");
					pw = in.nextLine();

					client.login(login, pw);
					break;

				case "signup":
					System.out.print("Cadastro selecionado ...\nLogin: ");
					login = in.nextLine();
					System.out.print("Senha: ");
					pw = in.nextLine();

					client.signUp(login, pw);
					break;

				case "adicionar":
					System.out.print("Insira o caminho: ");
					String caminho = in.nextLine();
					client.adicionarArquivo(caminho);
					break;

				case "exit":
					client.disconnect();
					working = false;
					break;

				default:
					System.out.println("Comando nao identificado ... ");
					break;
			}
		}

	}
}