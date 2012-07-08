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

	private JPanel contentPane;
	private static JTextPane textPane;
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Velocidade frame = new Velocidade();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
		Scanner in = new Scanner(System.in);
		for(int i =0 ;i<10;i++){
			setText(in.next());
		}
	}

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
		
		this.textPane = new JTextPane();
		contentPane.add(textPane, BorderLayout.CENTER);
	}
	
	
	public static void setText(String teste){
		textPane.setText(teste);
		 Font font2 = new Font("Calibri", Font.PLAIN, 200);
		textPane.setFont(font2);
	}
}
