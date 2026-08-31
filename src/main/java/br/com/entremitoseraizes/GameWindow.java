package br.com.entremitoseraizes;

import javax.swing.JFrame;

/** Janela que hospeda a tela do jogo. */
final class GameWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    GameWindow() {
        super("Entre Mitos e Raízes");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new GamePanel());
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }
}
