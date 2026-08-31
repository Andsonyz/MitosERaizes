package br.com.entremitoseraizes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Interface, mapa e controles. Os gráficos são pixel art desenhados em Java2D. */
final class GamePanel extends JPanel implements KeyListener {
    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 1024;
    private static final int HEIGHT = 720;
    private static final int TILE = 32;
    private static final int MAP_X = 16;
    private static final int MAP_Y = 96;

    private final GameSession session = new GameSession();
    private final AccessibilitySettings accessibility = new AccessibilitySettings();
    private Screen screen = Screen.MENU;
    private Screen overlayReturn = Screen.MENU;
    private List<DialogueLine> dialogue = new ArrayList<DialogueLine>();
    private int dialogueIndex;
    private DialogueExit dialogueExit = DialogueExit.WORLD;
    private int menuIndex;
    private int settingsIndex;
    private int pauseIndex;
    private int codexIndex;
    private String transientMessage = "";
    private int messageFrames;
    private int animationFrame;

    private enum DialogueExit { WORLD, NEXT_CHAPTER, END }

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        new Timer(1000 / 20, e -> {
            animationFrame++;
            if (messageFrames > 0) messageFrames--;
            repaint();
        }).start();
    }

    @Override public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        int offsetX = (int) ((getWidth() - WIDTH * scale) / 2);
        int offsetY = (int) ((getHeight() - HEIGHT * scale) / 2);
        g.translate(offsetX, offsetY);
        g.scale(scale, scale);

        if (screen == Screen.MENU) drawMenu(g);
        else if (screen == Screen.SETTINGS) drawSettings(g);
        else if (screen == Screen.EXTRAS) drawExtras(g);
        else if (screen == Screen.CODEX) drawCodex(g);
        else if (screen == Screen.MAP) drawMap(g);
        else if (screen == Screen.END) drawEnd(g);
        else {
            drawWorld(g);
            if (screen == Screen.DIALOGUE) drawDialogue(g);
            if (screen == Screen.PAUSE) drawPause(g);
        }
        g.dispose();
    }

    private Font font(int size) {
        int multiplier = accessibility.textScale() == 0 ? -2 : accessibility.textScale() == 2 ? 3 : 0;
        return new Font("Dialog", Font.BOLD, Math.max(11, size + multiplier));
    }

    private Font normal(int size) {
        int multiplier = accessibility.textScale() == 0 ? -1 : accessibility.textScale() == 2 ? 2 : 0;
        return new Font("Dialog", Font.PLAIN, Math.max(11, size + multiplier));
    }

    private Color c(int rgb) { return new Color(rgb); }
    private Color ink() { return accessibility.highContrast() ? Color.WHITE : c(0x183343); }
    private Color paper() { return accessibility.highContrast() ? c(0x101010) : c(0xFFF7D5); }
    private Color outline() { return accessibility.highContrast() ? c(0xFFE600) : c(0x173447); }
    private Color selected() { return accessibility.highContrast() ? c(0x0057FF) : c(0xE56B3F); }

    private void drawMenu(Graphics2D g) {
        drawMenuBackdrop(g);
        g.setColor(c(0x152A3A));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawMenuBackdrop(g);
        g.setColor(c(0xFFF3B0));
        g.setFont(font(46));
        centered(g, "ENTRE MITOS", WIDTH / 2, 110);
        centered(g, "E RAÍZES", WIDTH / 2, 160);
        g.setColor(c(0x183343));
        g.setFont(font(17));
        centered(g, "Uma aventura de cuidado com a Mata do Encanto", WIDTH / 2, 192);

        String[] items = { "Jogar", "Configurações", "Extras" };
        for (int i = 0; i < items.length; i++) drawMenuOption(g, items[i], 360 + i * 66, i == menuIndex);

        g.setColor(c(0xFFF3B0));
        g.setFont(normal(15));
        centered(g, "Campanha educativa: 30–40 minutos • Teclado", WIDTH / 2, 596);
        centered(g, "Use ↑ ↓ e Enter", WIDTH / 2, 622);
        drawPixelFlower(g, 92, 588, c(0xF4D35E));
        drawPixelFlower(g, 914, 588, c(0xF4D35E));
    }

    private void drawMenuBackdrop(Graphics2D g) {
        g.setColor(c(0x76C6C8));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(c(0xB8E986));
        g.fillRect(0, 415, WIDTH, HEIGHT - 415);
        g.setColor(c(0x51A96A));
        for (int x = -20; x < WIDTH; x += 64) {
            int h = 75 + (x / 64 % 3) * 18;
            g.fillRect(x, 415 - h, 46, h + 90);
            g.setColor(c(0x2D7A52));
            g.fillRect(x + 9, 390 - h, 30, 40);
            g.setColor(c(0x51A96A));
        }
        g.setColor(c(0xF6D365));
        g.fillRect(816, 60, 82, 82);
        g.setColor(c(0x76C6C8));
        g.fillRect(838, 52, 88, 72);
        for (int i = 0; i < 15; i++) {
            int x = (i * 71 + 42) % WIDTH;
            int y = 235 + (i * 43) % 150;
            g.setColor(i % 2 == 0 ? c(0xFFF3B0) : c(0xF6A6B2));
            g.fillRect(x, y, 4, 4);
        }
    }

    private void drawMenuOption(Graphics2D g, String label, int y, boolean isSelected) {
        int x = 360;
        int w = 304;
        g.setColor(isSelected ? selected() : paper());
        g.fillRoundRect(x, y, w, 50, 8, 8);
        g.setColor(outline());
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, y, w, 50, 8, 8);
        if (isSelected) {
            g.setColor(c(0xFFF3B0));
            g.fillRect(x + 13, y + 19, 12, 12);
        }
        g.setColor(isSelected && !accessibility.highContrast() ? Color.WHITE : ink());
        g.setFont(font(21));
        centered(g, label, x + w / 2 + (isSelected ? 8 : 0), y + 32);
    }

    private void drawWorld(Graphics2D g) {
        g.setColor(c(0x163A42));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(c(0xFFF3B0));
        g.setFont(font(24));
        g.drawString("MATA DO ENCANTO", MAP_X, 38);
        g.setFont(normal(14));
        g.drawString(session.chapterName() + " • Guardião: " + session.guardian(), MAP_X, 61);

        for (int y = 0; y < 15; y++) {
            for (int x = 0; x < 24; x++) drawTile(g, x, y);
        }
        drawGuardian(g, 3, 5, "Saci", c(0xD8443A));
        drawGuardian(g, 10, 11, "Caipora", c(0xD98725));
        drawGuardian(g, 18, 8, "Iara", c(0x2F86C9));
        drawGuardian(g, 20, 5, "Boitatá", c(0xE5B840));
        for (Task task : session.remainingTasks()) drawTaskMarker(g, task);
        drawPlayer(g);
        drawHud(g);
        if (messageFrames > 0) drawToast(g, transientMessage);
    }

    private void drawTile(Graphics2D g, int x, int y) {
        int px = MAP_X + x * TILE;
        int py = MAP_Y + y * TILE;
        if (isWater(x, y)) {
            g.setColor(c((x + y + animationFrame / 8) % 2 == 0 ? 0x4CA8D8 : 0x4398C8));
            g.fillRect(px, py, TILE, TILE);
            g.setColor(c(0xBDE8E7));
            g.fillRect(px + 5, py + 11, 10, 2);
            g.fillRect(px + 18, py + 22, 8, 2);
        } else if (session.isBlocked(x, y)) {
            g.setColor(c(0x2D7A52));
            g.fillRect(px, py, TILE, TILE);
            g.setColor(c(0x165A3A));
            g.fillRect(px + 4, py + 4, 24, 23);
            g.setColor(c(0x4FA35F));
            g.fillRect(px + 8, py + 1, 17, 10);
            g.setColor(c(0x70442F));
            g.fillRect(px + 13, py + 21, 7, 11);
        } else {
            g.setColor((x + y) % 2 == 0 ? c(0x94C95D) : c(0x8EC257));
            g.fillRect(px, py, TILE, TILE);
            g.setColor(c(0x6AAC4C));
            g.fillRect(px + 4, py + 7, 2, 5);
            g.fillRect(px + 24, py + 21, 2, 5);
            if ((x == 19 && y == 11)) {
                g.setColor(c(0xB97844));
                g.fillRect(px, py + 5, TILE, 22);
                g.setColor(c(0xE6B06B));
                for (int i = 3; i < TILE; i += 8) g.fillRect(px + i, py + 6, 3, 20);
            }
        }
        g.setColor(c(0x497F47));
        g.drawRect(px, py, TILE, TILE);
    }

    private boolean isWater(int x, int y) {
        return x >= 16 && x <= 22 && y >= 9 && y <= 12 && !(x == 19 && y == 11);
    }

    private void drawTaskMarker(Graphics2D g, Task task) {
        int px = MAP_X + task.x * TILE;
        int py = MAP_Y + task.y * TILE;
        int bob = animationFrame / 5 % 2 == 0 ? 0 : -2;
        g.setColor(c(0xFBEA64));
        g.fillRect(px + 10, py + 1 + bob, 13, 16);
        g.setColor(c(0x713E32));
        g.drawRect(px + 10, py + 1 + bob, 13, 16);
        g.setColor(c(0x713E32));
        g.setFont(font(15));
        g.drawString("!", px + 14, py + 15 + bob);
    }

    private void drawGuardian(Graphics2D g, int x, int y, String name, Color color) {
        int px = MAP_X + x * TILE;
        int py = MAP_Y + y * TILE;
        g.setColor(c(0x315D3D));
        g.fillOval(px + 7, py + 25, 20, 6);
        g.setColor(color);
        g.fillRect(px + 10, py + 11, 13, 16);
        g.fillRect(px + 8, py + 7, 17, 9);
        g.setColor(c(0xFFF0C7));
        g.fillRect(px + 12, py + 8, 9, 7);
        g.setColor(c(0x173447));
        g.fillRect(px + 14, py + 10, 2, 2);
        g.fillRect(px + 19, py + 10, 2, 2);
        if (!name.isEmpty()) {
            g.setColor(c(0xFFF3B0));
            g.setFont(normal(10));
            g.drawString(name.substring(0, 1), px + 13, py + 5);
        }
    }

    private void drawPlayer(Graphics2D g) {
        int px = MAP_X + session.playerX() * TILE;
        int py = MAP_Y + session.playerY() * TILE;
        g.setColor(c(0x315D3D));
        g.fillOval(px + 7, py + 26, 20, 6);
        g.setColor(c(0x394A90));
        g.fillRect(px + 10, py + 14, 13, 14);
        g.setColor(c(0xFFF0C7));
        g.fillRect(px + 11, py + 7, 11, 9);
        g.setColor(c(0x3D2935));
        g.fillRect(px + 10, py + 4, 13, 5);
        g.setColor(c(0xE05A47));
        g.fillRect(px + 8, py + 9, 17, 3);
        g.setColor(c(0x173447));
        g.fillRect(px + 14, py + 11, 2, 2);
        g.fillRect(px + 19, py + 11, 2, 2);
        Task nearby = session.nearbyTask();
        if (nearby != null) {
            g.setColor(c(0xFFF3B0));
            g.fillRoundRect(px + 4, py - 20, 24, 16, 4, 4);
            g.setColor(c(0x173447));
            g.setFont(font(13));
            g.drawString("E", px + 11, py - 7);
        }
    }

    private void drawHud(Graphics2D g) {
        int x = 800;
        g.setColor(paper());
        g.fillRoundRect(x, 96, 208, 485, 12, 12);
        g.setColor(outline());
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(x, 96, 208, 485, 12, 12);
        g.setColor(selected());
        g.fillRoundRect(x + 13, 110, 182, 28, 7, 7);
        g.setColor(accessibility.highContrast() ? Color.WHITE : c(0xFFFFFF));
        g.setFont(font(16));
        centered(g, "MISSÃO ATUAL", x + 104, 131);
        g.setColor(ink());
        g.setFont(font(18));
        int y = drawWrapped(g, session.objective(), x + 18, 168, 170, 22, ink());
        int total = session.remainingTasks().size();
        g.setColor(c(0x4D815A));
        g.fillRoundRect(x + 18, y + 12, 170, 20, 6, 6);
        int all = session.chapter() == 3 ? 4 : 3;
        int done = all - total;
        g.setColor(selected());
        g.fillRoundRect(x + 18, y + 12, (int) (170 * (done / (double) all)), 20, 6, 6);
        g.setColor(ink());
        g.setFont(font(14));
        g.drawString(done + " de " + all + " ações", x + 58, y + 28);

        g.setColor(outline());
        g.drawLine(x + 18, y + 56, x + 188, y + 56);
        g.setFont(font(14));
        g.setColor(ink());
        g.drawString("CONTROLES", x + 18, y + 82);
        g.setFont(normal(13));
        drawWrapped(g, "WASD / setas: mover", x + 18, y + 108, 170, 18, ink());
        drawWrapped(g, "E: interagir", x + 18, y + 140, 170, 18, ink());
        drawWrapped(g, "C: Códice  •  M: mapa", x + 18, y + 172, 170, 18, ink());
        drawWrapped(g, "Esc: pausar", x + 18, y + 204, 170, 18, ink());
        g.setColor(c(0x4D815A));
        g.fillRoundRect(x + 18, 522, 170, 40, 7, 7);
        g.setColor(c(0xFFF3B0));
        g.setFont(font(12));
        centered(g, "! = ponto da missão", x + 103, 547);
    }

    private void drawDialogue(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 145));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(paper());
        g.fillRoundRect(32, 458, 944, 230, 12, 12);
        g.setColor(outline());
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(32, 458, 944, 230, 12, 12);
        if (dialogue.isEmpty()) return;
        DialogueLine line = dialogue.get(dialogueIndex);
        g.setColor(selected());
        g.fillRoundRect(57, 439, 230, 42, 8, 8);
        g.setColor(accessibility.highContrast() ? Color.WHITE : c(0xFFFFFF));
        g.setFont(font(20));
        g.drawString(line.speaker, 74, 467);
        g.setColor(ink());
        g.setFont(normal(22));
        drawWrapped(g, line.text, 65, 523, 875, 30, ink());
        g.setFont(normal(14));
        g.setColor(outline());
        g.drawString("Enter / Espaço para continuar", 690, 665);
        g.drawString((dialogueIndex + 1) + "/" + dialogue.size(), 65, 665);
    }

    private void drawSettings(Graphics2D g) {
        drawMenuBackdrop(g);
        drawPageTitle(g, "CONFIGURAÇÕES", "Ajustes de acessibilidade e leitura");
        String[] labels = { "Tamanho do texto", "Alto contraste" };
        String[] values = { accessibility.textSizeName(), accessibility.highContrast() ? "Ativado" : "Desativado" };
        for (int i = 0; i < labels.length; i++) {
            int y = 255 + i * 92;
            g.setColor(i == settingsIndex ? selected() : paper());
            g.fillRoundRect(242, y, 540, 70, 10, 10);
            g.setColor(outline());
            g.setStroke(new BasicStroke(3));
            g.drawRoundRect(242, y, 540, 70, 10, 10);
            g.setColor(i == settingsIndex && !accessibility.highContrast() ? Color.WHITE : ink());
            g.setFont(font(20));
            g.drawString(labels[i], 270, y + 31);
            g.setFont(normal(16));
            g.drawString("◀   " + values[i] + "   ▶", 270, y + 56);
        }
        g.setColor(c(0x183343));
        g.setFont(normal(16));
        centered(g, "Use ↑ ↓ para escolher e ← → ou Enter para alterar.", WIDTH / 2, 510);
        centered(g, "Esc para voltar", WIDTH / 2, 540);
    }

    private void drawExtras(Graphics2D g) {
        drawMenuBackdrop(g);
        drawPageTitle(g, "EXTRAS", "Conheça quem criou esta aventura");
        g.setColor(paper());
        g.fillRoundRect(194, 230, 636, 290, 14, 14);
        g.setColor(outline());
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(194, 230, 636, 290, 14, 14);
        g.setColor(c(0xE56B3F));
        g.fillRect(220, 258, 564, 4);
        g.setColor(ink());
        g.setFont(font(25));
        centered(g, "CRÉDITOS", WIDTH / 2, 300);
        g.setFont(font(22));
        centered(g, "Antonio Andson", WIDTH / 2, 360);
        centered(g, "Sophia Hellen", WIDTH / 2, 406);
        g.setFont(normal(16));
        centered(g, "Criação, desenvolvimento e cuidado com as raízes brasileiras.", WIDTH / 2, 466);
        g.setFont(normal(15));
        centered(g, "Esc para voltar", WIDTH / 2, 570);
    }

    private void drawCodex(Graphics2D g) {
        g.setColor(c(0x264653));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(c(0xFFF3B0));
        g.setFont(font(30));
        g.drawString("CÓDICE DA MATA", 54, 62);
        g.setFont(normal(15));
        g.drawString("Use ↑ ↓ para consultar • Esc para voltar", 54, 88);
        FolkloreEntry[] entries = session.codex();
        g.setColor(paper());
        g.fillRoundRect(38, 115, 270, 540, 12, 12);
        g.setColor(outline());
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(38, 115, 270, 540, 12, 12);
        for (int i = 0; i < entries.length; i++) {
            int y = 145 + i * 88;
            boolean active = i == codexIndex;
            g.setColor(active ? selected() : c(0xD9E7B4));
            g.fillRoundRect(57, y, 232, 62, 7, 7);
            g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink());
            g.setFont(font(18));
            g.drawString(entries[i].name, 75, y + 27);
            g.setFont(normal(12));
            g.drawString(entries[i].title, 75, y + 48);
        }
        FolkloreEntry entry = entries[codexIndex];
        g.setColor(paper());
        g.fillRoundRect(335, 115, 650, 540, 12, 12);
        g.setColor(outline());
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(335, 115, 650, 540, 12, 12);
        g.setColor(c(entry.color));
        g.fillRoundRect(370, 150, 580, 44, 7, 7);
        g.setColor(Color.WHITE);
        g.setFont(font(25));
        g.drawString(entry.name, 395, 180);
        g.setColor(ink());
        g.setFont(font(20));
        g.drawString(entry.title, 378, 240);
        g.setFont(normal(20));
        drawWrapped(g, entry.text, 378, 290, 535, 30, ink());
        drawCodexSymbol(g, 877, 492, entry.color);
    }

    private void drawMap(Graphics2D g) {
        drawWorld(g);
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(paper());
        g.fillRoundRect(138, 84, 748, 550, 14, 14);
        g.setColor(outline());
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(138, 84, 748, 550, 14, 14);
        g.setColor(ink());
        g.setFont(font(27));
        centered(g, "MAPA DA MATA DO ENCANTO", WIDTH / 2, 126);
        g.setFont(normal(16));
        centered(g, "Você está aqui:  ●     Tarefa atual:  !", WIDTH / 2, 154);
        int miniX = 214;
        int miniY = 184;
        int mini = 22;
        for (int y = 0; y < 15; y++) for (int x = 0; x < 24; x++) {
            g.setColor(isWater(x, y) ? c(0x4CA8D8) : session.isBlocked(x, y) ? c(0x2D7A52) : c(0x94C95D));
            g.fillRect(miniX + x * mini, miniY + y * mini, mini - 1, mini - 1);
        }
        for (Task task : session.remainingTasks()) {
            g.setColor(c(0xE56B3F));
            g.fillRect(miniX + task.x * mini + 6, miniY + task.y * mini + 5, 10, 12);
        }
        g.setColor(c(0x163A42));
        g.fillOval(miniX + session.playerX() * mini + 5, miniY + session.playerY() * mini + 5, 12, 12);
        g.setColor(ink());
        g.setFont(normal(16));
        centered(g, "Objetivo: " + session.objective(), WIDTH / 2, 560);
        centered(g, "Esc ou M para fechar", WIDTH / 2, 600);
    }

    private void drawPause(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setColor(paper());
        g.fillRoundRect(340, 210, 344, 290, 14, 14);
        g.setColor(outline());
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(340, 210, 344, 290, 14, 14);
        g.setColor(ink());
        g.setFont(font(30));
        centered(g, "PAUSA", WIDTH / 2, 266);
        drawPauseOption(g, "Continuar", 310, pauseIndex == 0);
        drawPauseOption(g, "Voltar ao menu", 382, pauseIndex == 1);
        g.setFont(normal(14));
        g.setColor(ink());
        centered(g, "↑ ↓ e Enter • Esc para continuar", WIDTH / 2, 466);
    }

    private void drawPauseOption(Graphics2D g, String text, int y, boolean active) {
        g.setColor(active ? selected() : c(0xD9E7B4));
        g.fillRoundRect(375, y, 274, 48, 7, 7);
        g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink());
        g.setFont(font(19));
        centered(g, text, WIDTH / 2, y + 31);
    }

    private void drawEnd(Graphics2D g) {
        drawMenuBackdrop(g);
        g.setColor(new Color(0x163A42));
        g.fillRoundRect(148, 70, 728, 564, 18, 18);
        g.setColor(c(0xFFF3B0));
        g.setStroke(new BasicStroke(4));
        g.drawRoundRect(148, 70, 728, 564, 18, 18);
        g.setFont(font(34));
        centered(g, "A MATA RESPIRA", WIDTH / 2, 138);
        g.setFont(normal(20));
        int y = drawWrapped(g, "Você concluiu a jornada de Entre Mitos e Raízes. Saci, Caipora, Iara e Boitatá lembram que preservar a floresta é uma escolha coletiva: prevenir queimadas, proteger a água, respeitar a fauna e escutar quem cuida do território.", 210, 205, 604, 31, c(0xFFF3B0));
        g.setFont(font(19));
        centered(g, "Tempo de campanha planejado: 30–40 minutos", WIDTH / 2, y + 64);
        g.setFont(normal(17));
        centered(g, "Consulte o Códice em uma nova jornada para revisitar os aprendizados.", WIDTH / 2, y + 104);
        drawGuardian(g, 12, 14, "", c(0xD8443A));
        drawGuardian(g, 14, 14, "", c(0xD98725));
        drawGuardian(g, 16, 14, "", c(0x2F86C9));
        drawGuardian(g, 18, 14, "", c(0xE5B840));
        g.setColor(c(0xFFF3B0));
        g.setFont(normal(16));
        centered(g, "Enter ou Esc para retornar ao menu", WIDTH / 2, 616);
    }

    private void drawPageTitle(Graphics2D g, String title, String subtitle) {
        g.setColor(new Color(0x163A42));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        drawMenuBackdrop(g);
        g.setColor(c(0xFFF3B0));
        g.setFont(font(34));
        centered(g, title, WIDTH / 2, 130);
        g.setColor(c(0x183343));
        g.setFont(normal(17));
        centered(g, subtitle, WIDTH / 2, 164);
    }

    private void drawToast(Graphics2D g, String text) {
        g.setColor(new Color(0x173447));
        g.fillRoundRect(90, 42, 580, 34, 8, 8);
        g.setColor(c(0xFFF3B0));
        g.setFont(normal(14));
        g.drawString(text, 105, 64);
    }

    private void drawCodexSymbol(Graphics2D g, int x, int y, int rgb) {
        g.setColor(c(rgb));
        g.fillOval(x - 50, y - 50, 100, 100);
        g.setColor(c(0xFFF3B0));
        g.fillRect(x - 8, y - 36, 16, 72);
        g.fillRect(x - 36, y - 8, 72, 16);
        g.setColor(outline());
        g.drawOval(x - 50, y - 50, 100, 100);
    }

    private void drawPixelFlower(Graphics2D g, int x, int y, Color color) {
        g.setColor(c(0x2D7A52));
        g.fillRect(x, y, 4, 25);
        g.setColor(color);
        g.fillRect(x - 7, y - 7, 12, 12);
        g.fillRect(x + 2, y - 11, 12, 12);
        g.setColor(c(0xE56B3F));
        g.fillRect(x - 1, y - 3, 7, 7);
    }

    private int drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, Color color) {
        g.setColor(color);
        FontMetrics metrics = g.getFontMetrics();
        String[] words = text.split(" ");
        String line = "";
        int currentY = y;
        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(test) > width && !line.isEmpty()) {
                g.drawString(line, x, currentY);
                line = word;
                currentY += lineHeight;
            } else line = test;
        }
        if (!line.isEmpty()) g.drawString(line, x, currentY);
        return currentY;
    }

    private void centered(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
    }

    private void openDialogue(DialogueLine[] lines, DialogueExit exit) {
        dialogue = new ArrayList<DialogueLine>(Arrays.asList(lines));
        dialogueIndex = 0;
        dialogueExit = exit;
        screen = Screen.DIALOGUE;
    }

    private void handleInteract() {
        Task task = session.nearbyTask();
        if (task == null) {
            showMessage("Nada para investigar aqui. Procure um símbolo ! no mapa.");
            return;
        }
        boolean chapterComplete = session.complete(task);
        List<DialogueLine> lines = new ArrayList<DialogueLine>(Arrays.asList(session.taskFeedback(task)));
        if (chapterComplete) lines.addAll(Arrays.asList(session.closing()));
        DialogueExit exit = chapterComplete
            ? (session.chapter() == GameSession.LAST_CHAPTER ? DialogueExit.END : DialogueExit.NEXT_CHAPTER)
            : DialogueExit.WORLD;
        openDialogue(lines.toArray(new DialogueLine[lines.size()]), exit);
    }

    private void advanceDialogue() {
        if (dialogueIndex + 1 < dialogue.size()) {
            dialogueIndex++;
            return;
        }
        if (dialogueExit == DialogueExit.NEXT_CHAPTER) {
            session.nextChapter();
            openDialogue(session.introduction(), DialogueExit.WORLD);
        } else if (dialogueExit == DialogueExit.END) screen = Screen.END;
        else screen = Screen.WORLD;
    }

    private void showMessage(String message) {
        transientMessage = message;
        messageFrames = 45;
    }

    private void movePlayer(int dx, int dy) {
        int x = session.playerX() + dx;
        int y = session.playerY() + dy;
        if (!session.isBlocked(x, y)) session.movePlayer(dx, dy);
        else showMessage("Esse caminho está bloqueado. Procure uma rota segura.");
    }

    @Override public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (screen == Screen.MENU) handleMenuKey(key);
        else if (screen == Screen.WORLD) handleWorldKey(key);
        else if (screen == Screen.DIALOGUE) {
            if (isConfirm(key)) advanceDialogue();
        } else if (screen == Screen.CODEX) handleCodexKey(key);
        else if (screen == Screen.MAP) {
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M) screen = overlayReturn;
        } else if (screen == Screen.SETTINGS) handleSettingsKey(key);
        else if (screen == Screen.EXTRAS) {
            if (key == KeyEvent.VK_ESCAPE || isConfirm(key)) screen = Screen.MENU;
        } else if (screen == Screen.PAUSE) handlePauseKey(key);
        else if (screen == Screen.END && (key == KeyEvent.VK_ESCAPE || isConfirm(key))) screen = Screen.MENU;
        repaint();
    }

    private void handleMenuKey(int key) {
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) menuIndex = (menuIndex + 2) % 3;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) menuIndex = (menuIndex + 1) % 3;
        else if (isConfirm(key)) {
            if (menuIndex == 0) {
                session.reset();
                openDialogue(session.introduction(), DialogueExit.WORLD);
            } else if (menuIndex == 1) screen = Screen.SETTINGS;
            else screen = Screen.EXTRAS;
        }
    }

    private void handleWorldKey(int key) {
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) movePlayer(0, -1);
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) movePlayer(0, 1);
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) movePlayer(-1, 0);
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D) movePlayer(1, 0);
        else if (key == KeyEvent.VK_E) handleInteract();
        else if (key == KeyEvent.VK_C) { overlayReturn = Screen.WORLD; screen = Screen.CODEX; }
        else if (key == KeyEvent.VK_M) { overlayReturn = Screen.WORLD; screen = Screen.MAP; }
        else if (key == KeyEvent.VK_ESCAPE) screen = Screen.PAUSE;
    }

    private void handleCodexKey(int key) {
        int length = session.codex().length;
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) codexIndex = (codexIndex + length - 1) % length;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) codexIndex = (codexIndex + 1) % length;
        else if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_C) screen = overlayReturn;
    }

    private void handleSettingsKey(int key) {
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W) settingsIndex = (settingsIndex + 1) % 2;
        else if (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) settingsIndex = (settingsIndex + 1) % 2;
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) changeSetting(-1);
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D || isConfirm(key)) changeSetting(1);
        else if (key == KeyEvent.VK_ESCAPE) screen = Screen.MENU;
    }

    private void changeSetting(int direction) {
        if (settingsIndex == 0) {
            if (direction < 0) accessibility.decreaseText(); else accessibility.increaseText();
        } else accessibility.toggleContrast();
    }

    private void handlePauseKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; }
        if (key == KeyEvent.VK_UP || key == KeyEvent.VK_W || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S) pauseIndex = 1 - pauseIndex;
        else if (isConfirm(key)) screen = pauseIndex == 0 ? Screen.WORLD : Screen.MENU;
    }

    private boolean isConfirm(int key) {
        return key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE || key == KeyEvent.VK_E;
    }

    @Override public void keyReleased(KeyEvent e) { }
    @Override public void keyTyped(KeyEvent e) { }
}
