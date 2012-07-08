package network;

import java.io.File;
import java.io.FileInputStream;

public class EnviarArquivo {
	File arquivo_para_enviar;
	FileInputStream stream_arquivo_enviar;
	long bytes_transferidos_sucesso;
	miniSocket socket;
}
