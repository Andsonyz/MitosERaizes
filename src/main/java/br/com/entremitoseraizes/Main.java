package br.com.entremitoseraizes;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Ponto de entrada do jogo. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // A aparência padrão do Java continua adequada se o sistema não estiver disponível.
        }
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                new GameWindow().setVisible(true);
            }
        });
    }
}
