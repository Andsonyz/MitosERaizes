package br.com.entremitoseraizes;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Janela que hospeda a tela do jogo. */
final class GameWindow extends JFrame {
    private static final long serialVersionUID = 1L;
    GameWindow() {
        super("Entre Mitos e Raízes");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        final GamePanel panel = new GamePanel();
        setContentPane(panel);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                panel.saveIfRunning();
                dispose();
                System.exit(0);
            }
        });
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }
}
