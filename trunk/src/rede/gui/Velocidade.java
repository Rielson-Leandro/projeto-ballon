package rede.gui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextPane;

public class Velocidade extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 924092640127481489L;
	private JPanel contentPane;
	private static JTextPane textPane;
	
	/**
	 * Create the frame.
	 */
	public Velocidade() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		Velocidade.textPane = new JTextPane();
		contentPane.add(textPane, BorderLayout.CENTER);
	}
	
	
	public static void setText(String teste){
		textPane.setText(teste);
		 Font font2 = new Font("Calibri", Font.PLAIN, 150);
		textPane.setFont(font2);
	}
}
