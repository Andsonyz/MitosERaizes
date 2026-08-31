package br.com.entremitoseraizes;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

/** Garante que todas as telas principais conseguem ser desenhadas sem exceções. */
public final class ScreenRenderSmokeTest {
    private ScreenRenderSmokeTest() { }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        GamePanel panel = new GamePanel();
        panel.setSize(1152, 720);
        Field screen = GamePanel.class.getDeclaredField("screen");
        screen.setAccessible(true);
        for (Screen value : new Screen[] { Screen.MENU, Screen.SLOT_SELECT, Screen.NAME_INPUT,
                Screen.WORLD, Screen.MAP, Screen.PAUSE, Screen.SETTINGS, Screen.EXTRAS, Screen.END }) {
            screen.set(panel, value);
            BufferedImage image = new BufferedImage(1152, 720, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            panel.paint(graphics);
            graphics.dispose();
        }
        System.out.println("Renderização das telas validada.");
        System.exit(0);
    }
}
