package br.com.entremitoseraizes.game;

import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

public final class GameWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    public GameWindow() {

        super("Entre Mitos e Raízes");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        final GamePanel panel = new GamePanel();
        setContentPane(panel);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent event) {

                panel.saveIfRunning();

                dispose();

                System.exit(0);
            }
        });

        // Remove a barra superior da janela
        setUndecorated(true);

        // Impede redimensionamento manual
        setResizable(false);

        Dimension tamanhoTela = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds()
                .getSize();
        setSize(tamanhoTela);
        setLocation(0, 0);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
}