package br.com.entremitoseraizes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

/** Jogo construído sobre os três cenários fornecidos: trilha, cabana e interior. */
final class GamePanel extends JPanel implements KeyListener {
    private static final long serialVersionUID = 3L;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final float SPEED = 220f;
    private static final float EDGE = 28f;
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    private final AccessibilitySettings accessibility = new AccessibilitySettings();
    private final SaveManager saves = new SaveManager();
    private final Set<Integer> heldKeys = new HashSet<Integer>();
    private final Timer clock;
    private BufferedImage forestPath;
    private BufferedImage cabinApproach;
    private BufferedImage cabinInterior;
    private BufferedImage title;
    private BufferedImage curupira;
    private BufferedImage gregIdle;
    private BufferedImage gregWalkPart1;
    private BufferedImage gregWalkPart2;
    private BufferedImage menuFallback;
    private ImageIcon menuAnimation;
    private GameSession session = new GameSession();
    private Screen screen = Screen.MENU;
    private Screen settingsReturn = Screen.MENU;
    private int menuIndex;
    private int slotIndex;
    private int pauseIndex;
    private int settingsIndex;
    private String typedName = "";
    private List<DialogueLine> dialogue = new ArrayList<DialogueLine>();
    private int dialogueIndex;
    private Runnable dialogueFinish;
    private long previousTick = System.nanoTime();
    private int animationFrame;
    private int walkingFrame;
    private boolean gregIsWalking;
    private int transitionFrame = -1;
    private Runnable transitionAction;
    private String toast = "";
    private int toastFrames;

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);
        loadAssets();
        clock = new Timer(16, event -> tick());
        clock.start();
    }

    @Override public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    void saveIfRunning() {
        if (screen != Screen.MENU && screen != Screen.SLOT_SELECT && screen != Screen.NAME_INPUT) saves.save(session);
    }


    private void loadAssets() {
        forestPath = loadImage("Cenarios", "CRPR-pt1.jpeg");
        cabinApproach = loadImage("Cenarios", "CRPR-pt2.jpeg");
        cabinInterior = loadImage("Cenarios", "CRPR-pt3.jpeg");
        menuFallback = loadImage("Cenarios", "MENU_loop.gif");
        title = loadImage("Cenarios", "Titulo.png");
        curupira = loadImage("Personagens", "CURUPIRA.PNG");
        gregIdle = loadImage("Greg", "GregParado.png");
        gregWalkPart1 = loadImage("Greg", "GregAndandoPT1.png");
        gregWalkPart2 = loadImage("Greg", "GregAndandoPT2.png");
        File gif = asset("Cenarios", "MENU_loop.gif");
        if (gif.isFile() && !Boolean.getBoolean("entremitos.test.noGif")) {
            menuAnimation = new ImageIcon(gif.getAbsolutePath());
        }
    }

    private void updateScale() {

        double scaleX = getWidth() / (double) WIDTH;
        double scaleY = getHeight() / (double) HEIGHT;

        // Mantém a proporção original do jogo.
        scale = Math.min(scaleX, scaleY);

        double scaledWidth = WIDTH * scale;
        double scaledHeight = HEIGHT * scale;

        // Centraliza o jogo na tela.
        offsetX = (getWidth() - scaledWidth) / 2.0;
        offsetY = (getHeight() - scaledHeight) / 2.0;
    }
    private BufferedImage loadImage(String folder, String name) {
        try {
            File file = asset(folder, name);
            return file.isFile() ? ImageIO.read(file) : null;
        } catch (IOException ignored) { return null; }
    }

    private File asset(String folder, String name) { return new File(new File(new File("Image"), folder), name); }

    private void tick() {
        long now = System.nanoTime();
        float delta = Math.min(0.05f, (now - previousTick) / 1_000_000_000f);
        previousTick = now;
        animationFrame++;
        if (toastFrames > 0) toastFrames--;
        if (transitionFrame >= 0) {
            transitionFrame++;
            if (transitionFrame == 18 && transitionAction != null) transitionAction.run();
            if (transitionFrame >= 36) { transitionFrame = -1; transitionAction = null; }
        } else if (screen == Screen.WORLD) movePlayer(delta);
        repaint();
    }

    private void movePlayer(float delta) {
        float dx = axis(KeyEvent.VK_D, KeyEvent.VK_RIGHT) - axis(KeyEvent.VK_A, KeyEvent.VK_LEFT);
        float dy = axis(KeyEvent.VK_S, KeyEvent.VK_DOWN) - axis(KeyEvent.VK_W, KeyEvent.VK_UP);
        if (dx == 0f && dy == 0f) {
            gregIsWalking = false;
            walkingFrame = 0;
            return;
        }
        gregIsWalking = true;
        walkingFrame++;
        float diagonal = dx != 0f && dy != 0f ? 0.7071f : 1f;
        float x = session.playerX() + dx * SPEED * diagonal * delta;
        float y = clamp(session.playerY() + dy * SPEED * diagonal * delta, 130f, HEIGHT - 52f);
        if (session.scene() == Scene.FOREST_PATH && x >= WIDTH - EDGE) {
            changeScene(Scene.CABIN_APPROACH, 78f, 525f);
        } else if (session.scene() == Scene.CABIN_APPROACH && x <= EDGE) {
            changeScene(Scene.FOREST_PATH, WIDTH - 82f, 525f);
        } else if (session.scene() == Scene.CABIN_INTERIOR && x >= WIDTH - EDGE) {
            changeScene(Scene.CABIN_APPROACH, 900f, 520f);
        } else {
            session.setPosition(clamp(x, EDGE, WIDTH - EDGE), y);
        }
    }

    private float axis(int first, int second) { return heldKeys.contains(first) || heldKeys.contains(second) ? 1f : 0f; }

    private void changeScene(final Scene next, final float x, final float y) {
        heldKeys.clear();
        beginTransition(new Runnable() {
            @Override public void run() {
                session.setScene(next);
                session.setPosition(x, y);
                saves.save(session);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics graphics) {

        super.paintComponent(graphics);

        updateScale();

        Graphics2D g = (Graphics2D) graphics.create();

        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_OFF
        );

        // Centraliza o jogo.
        g.translate(offsetX, offsetY);

        // Aplica a escala proporcional.
        g.scale(scale, scale);

        if (screen == Screen.MENU) {

            drawMenu(g);

        } else if (screen == Screen.SLOT_SELECT) {

            drawSlots(g);

        } else if (screen == Screen.NAME_INPUT) {

            drawNameInput(g);

        } else if (screen == Screen.SETTINGS) {

            drawSettings(g);

        } else if (screen == Screen.EXTRAS) {

            drawExtras(g);

        } else if (screen == Screen.END) {

            drawEnd(g);

        } else {

            drawScene(g);

            if (screen == Screen.DIALOGUE) {

                drawDialogue(g);

            } else if (screen == Screen.PAUSE) {

                drawPause(g);

            } else if (screen == Screen.MAP) {

                drawMap(g);
            }
        }

        if (transitionFrame >= 0) {

            drawIris(g);
        }

        g.dispose();
    }

    private void drawMenuBackground(Graphics2D g) {
        if (menuFallback != null) g.drawImage(menuFallback, 0, 0, WIDTH, HEIGHT, this);
        else { g.setColor(c(0x244B3B)); g.fillRect(0, 0, WIDTH, HEIGHT); }
        // O quadro estático por baixo elimina o brilho entre frames do GIF carregado.
        if (menuAnimation != null && menuAnimation.getImageLoadStatus() == java.awt.MediaTracker.COMPLETE) {
            g.drawImage(menuAnimation.getImage(), 0, 0, WIDTH, HEIGHT, this);
        }
    }

    private void drawMenu(Graphics2D g) {
        drawMenuBackground(g);
        g.setColor(new Color(3, 15, 16, 135)); g.fillRect(0, 0, WIDTH, HEIGHT);
        if (title != null) g.drawImage(title, WIDTH / 2 - 229, 24, 458, 228, this);
        String[] options = { "Novo jogo", "Continuar - slot 1", "Continuar - slot 2", "Configurações", "Extras", "Sair do jogo" };
        for (int i = 0; i < options.length; i++) drawOption(g, options[i], 280 + i * 58, i == menuIndex, WIDTH / 2 - 194, 388);
        g.setColor(c(0xFFF3C1)); g.setFont(normal(14)); centered(g, "WASD / setas para escolher • Enter para confirmar", WIDTH / 2, 674);
    }

    private void drawSlots(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 338, 128, 676, 462);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "ESCOLHA UM SLOT", WIDTH / 2, 188);
        g.setFont(normal(16)); centered(g, "Os saves ficam apenas no seu computador.", WIDTH / 2, 221);
        for (int i = 0; i < 2; i++) {
            int y = 275 + 116 * i; boolean active = slotIndex == i;
            int slotX = WIDTH / 2 - 285; int slotWidth = 570;
            g.setColor(active ? selected() : c(0xDDE9BE)); g.fillRoundRect(slotX, y, slotWidth, 84, 10, 10);
            g.setColor(outline()); g.drawRoundRect(slotX, y, slotWidth, 84, 10, 10);
            g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(20)); centered(g, "SLOT " + (i + 1), WIDTH / 2 - 155, y + 32);
            g.setFont(normal(14)); drawWrapped(g, saves.summary(i + 1), slotX + 31, y + 58, 495, 18, active && !accessibility.highContrast() ? Color.WHITE : ink());
        }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "Enter cria uma nova jornada nesse slot • Esc volta", WIDTH / 2, 548);
    }

    private void drawNameInput(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 306, 170, 612, 390);
        g.setColor(ink()); g.setFont(font(28)); centered(g, "COMO VOCÊ SE CHAMA?", WIDTH / 2, 238);
        g.setFont(normal(16)); centered(g, "O Curupira quer saber quem entrou na mata.", WIDTH / 2, 276);
        g.setColor(c(0xF9F0CF)); g.fillRoundRect(WIDTH / 2 - 233, 322, 466, 58, 8, 8); g.setColor(outline()); g.drawRoundRect(WIDTH / 2 - 233, 322, 466, 58, 8, 8);
        g.setColor(ink()); g.setFont(font(22)); centered(g, typedName.isEmpty() ? "Digite um apelido" : typedName + "_", WIDTH / 2, 360);
        g.setFont(normal(14)); centered(g, "Máximo de 18 caracteres • Enter inicia • Esc volta", WIDTH / 2, 440);
    }

    private void drawScene(Graphics2D g) {
        if (session.scene() == Scene.FOREST_PATH) drawImageScene(g, forestPath, "Cenário 1/3  •  Trilha da floresta", "Vá para a direita para encontrar a cabana.");
        else if (session.scene() == Scene.CABIN_APPROACH) drawCabinApproach(g);
        else drawCabinInterior(g);
        drawHud(g);
        if (toastFrames > 0) drawToast(g);
    }

    private void drawImageScene(Graphics2D g, BufferedImage image, String label, String help) {
        if (image != null) g.drawImage(image, 0, 0, WIDTH, HEIGHT, this);
        else { g.setColor(c(0x274D3A)); g.fillRect(0, 0, WIDTH, HEIGHT); }
        drawSceneCaption(g, label, help);
        if (session.scene() == Scene.FOREST_PATH) edgeArrow(g, WIDTH - 42, HEIGHT / 2, "→");
        drawGreg(g);
    }

    private void drawCabinApproach(Graphics2D g) {
        drawImageScene(g, cabinApproach, "Cenário 2/3  •  Entrada da cabana", "Vá à esquerda para voltar à trilha. Aproxime-se da porta e pressione E.");
        edgeArrow(g, 35, HEIGHT / 2, "←");
        int doorX = 950, doorY = 440;
        if (distance(session.playerX(), session.playerY(), doorX, doorY) < 118) {
            bubble(g, doorX, doorY - 125, "E");
            g.setColor(c(0xFFF3C1)); g.setFont(normal(14)); centered(g, "Entrar na cabana", doorX, doorY - 142);
        }
    }

    private void drawCabinInterior(Graphics2D g) {
        drawImageScene(g, cabinInterior, "Cenário 3/3  •  Dentro da cabana", "Aproxime-se do Curupira e pressione E. Vá à direita para sair.");
        int curupiraX = 720, curupiraY = 275;
        drawCurupira(g, curupiraX, curupiraY, 112, 136);
        edgeArrow(g, WIDTH - 42, HEIGHT / 2, "→");
        if (distance(session.playerX(), session.playerY(), curupiraX + 44, curupiraY + 90) < 128) bubble(g, curupiraX + 45, curupiraY - 20, "E");
    }

    private void drawSceneCaption(Graphics2D g, String titleText, String help) {
        g.setColor(new Color(8, 25, 23, 190)); g.fillRoundRect(20, 18, 500, 62, 10, 10);
        g.setColor(c(0xFFF3C1)); g.setFont(font(17)); centered(g, titleText, 20 + 250, 43);
        g.setFont(normal(13)); centered(g, help, 20 + 250, 65);
    }

    private void drawGreg(Graphics2D g) {
        BufferedImage sprite = gregIdle;
        if (gregIsWalking) {
            // Dois quadros alternados tornam o passo visível sem trocar de posição do sprite.
            boolean secondStep = (walkingFrame / 8) % 2 == 1;
            sprite = secondStep && gregWalkPart2 != null ? gregWalkPart2 : gregWalkPart1;
        }
        int w = 74, h = 132;
        int x = (int) session.playerX() - w / 2;
        int y = (int) session.playerY() - h + 22;
        g.setColor(new Color(0, 0, 0, 75)); g.fillOval(x + 17, y + h - 10, 43, 12);
        if (sprite != null) g.drawImage(sprite, x, y, w, h, this);
        else { g.setColor(c(0x8C6341)); g.fillRoundRect(x + 18, y + 20, 35, 90, 8, 8); }
        g.setFont(normal(12)); g.setColor(c(0xFFF3C1)); centered(g, session.playerName(), (int) session.playerX(), y - 4);
    }

    private void drawCurupira(Graphics2D g, int x, int y, int w, int h) {
        if (curupira != null) g.drawImage(curupira, x, y, w, h, this);
        else { g.setColor(c(0xE86A28)); g.fillOval(x, y, w, h); }
    }

    private void drawHud(Graphics2D g) {
        g.setColor(new Color(8, 25, 23, 190)); g.fillRoundRect(WIDTH / 2 - 175, 18, 350, 62, 10, 10);
        g.setColor(c(0xFFF3C1)); g.setFont(normal(13)); centered(g, "E interagir • M mapa • F5 salvar • Esc menu", WIDTH / 2, 43);
        g.setFont(font(14)); centered(g, session.stage().objective(), WIDTH / 2, 67);
    }

    private void drawDialogue(Graphics2D g) {
        shade(g, 110);
        if (dialogue.isEmpty()) return;
        DialogueLine line = dialogue.get(dialogueIndex);
        g.setColor(paper()); g.fillRoundRect(WIDTH / 2 - 530, 492, 1060, 184, 13, 13); g.setColor(outline()); g.setStroke(new BasicStroke(4)); g.drawRoundRect(WIDTH / 2 - 530, 492, 1060, 184, 13, 13);
        g.setColor(selected()); g.fillRoundRect(WIDTH / 2 - 512, 472, 255, 41, 8, 8); g.setColor(Color.WHITE); g.setFont(font(20)); centered(g, line.speaker, WIDTH / 2 - 384, 500);
        g.setColor(ink()); g.setFont(normal(21)); drawWrapped(g, line.text, WIDTH / 2 - 480, 550, 960, 29, ink());
        g.setFont(normal(13)); g.setColor(outline()); centered(g, "Enter / Espaço para continuar", WIDTH / 2, 652);
    }

    private void drawPause(Graphics2D g) {
        shade(g, 180); card(g, WIDTH / 2 - 224, 132, 448, 456); g.setColor(ink()); g.setFont(font(30)); centered(g, "MENU DA PARTIDA", WIDTH / 2, 190);
        String[] options = { "Continuar", "Salvar agora", "Configurações", "Voltar ao lobby", "Sair do jogo" };
        for (int i = 0; i < options.length; i++) drawOption(g, options[i], 225 + i * 58, i == pauseIndex, WIDTH / 2 - 184, 368);
        g.setColor(ink()); g.setFont(normal(13)); centered(g, "↑ ↓ e Enter • Esc para continuar", WIDTH / 2, 560);
    }

    private void drawMap(Graphics2D g) {
        shade(g, 175); card(g, WIDTH / 2 - 368, 126, 736, 452); g.setColor(ink()); g.setFont(font(28)); centered(g, "MAPA DA FASE", WIDTH / 2, 187);
        int y = 270; Scene[] scenes = Scene.values();
        for (int i = 0; i < scenes.length; i++) {
            boolean current = scenes[i] == session.scene();
            g.setColor(current ? selected() : c(0xDDE9BE)); g.fillRoundRect(WIDTH / 2 - 295, y + i * 74, 590, 48, 8, 8);
            g.setColor(current && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(17));
            centered(g, (i + 1) + ". " + sceneLabel(scenes[i]), WIDTH / 2, y + 31 + i * 74);
        }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "Caminho: Trilha → Entrada da cabana → Interior", WIDTH / 2, 520);
        centered(g, "Esc ou M para fechar", WIDTH / 2, 550);
    }

    private String sceneLabel(Scene scene) {
        return scene == Scene.FOREST_PATH ? "CRPR-pt1 - Trilha" : scene == Scene.CABIN_APPROACH ? "CRPR-pt2 - Entrada da cabana" : "CRPR-pt3 - Dentro da cabana";
    }

    private void drawSettings(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 306, 150, 612, 410); g.setColor(ink()); g.setFont(font(28)); centered(g, "CONFIGURAÇÕES", WIDTH / 2, 210);
        String[] labels = { "Tamanho do texto", "Alto contraste" }; String[] values = { accessibility.textSizeName(), accessibility.highContrast() ? "Ativado" : "Desativado" };
        for (int i = 0; i < 2; i++) { int y = 260 + i * 94; boolean active = i == settingsIndex; g.setColor(active ? selected() : c(0xDDE9BE)); g.fillRoundRect(WIDTH / 2 - 256, y, 512, 71, 8, 8); g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(19)); centered(g, labels[i], WIDTH / 2, y + 30); g.setFont(normal(15)); centered(g, "◀   " + values[i] + "   ▶", WIDTH / 2, y + 55); }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "↑ ↓ escolhe • ← → ou Enter altera • Esc volta", WIDTH / 2, 512);
    }

    private void drawExtras(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 300, 165, 600, 382); g.setColor(ink()); g.setFont(font(29)); centered(g, "EXTRAS", WIDTH / 2, 225);
        g.setFont(normal(16)); centered(g, "Entre Mitos e Raízes", WIDTH / 2, 263); g.setFont(font(22)); centered(g, "Antonio Andson", WIDTH / 2, 339); centered(g, "Sophia Hellen", WIDTH / 2, 386);
        g.setFont(normal(15)); centered(g, "Fase de teste: O Chamado do Curupira", WIDTH / 2, 450); centered(g, "Esc ou Enter para voltar", WIDTH / 2, 516);
    }

    private void drawEnd(Graphics2D g) {
        drawMenuBackground(g); shade(g, 145); card(g, WIDTH / 2 - 334, 137, 668, 446); g.setColor(ink()); g.setFont(font(31)); centered(g, "FASE DE TESTE CONCLUÍDA", WIDTH / 2, 204);
        if (curupira != null) g.drawImage(curupira, WIDTH / 2 - 58, 255, 116, 142, this);
        g.setFont(normal(19)); drawWrapped(g, "Você encontrou o Curupira e recebeu o chamado para proteger a mata. A próxima parte da aventura será desenvolvida a partir deste encontro.", WIDTH / 2 - 172, 293, 345, 29, ink());
        g.setFont(normal(15)); centered(g, "Enter ou Esc para voltar ao lobby", WIDTH / 2, 536);
    }

    private void drawOption(Graphics2D g, String label, int y, boolean active, int x, int width) {
        g.setColor(active ? selected() : new Color(16, 46, 47, 225)); g.fillRoundRect(x, y, width, 42, 8, 8); g.setColor(c(0xFFE5A1)); g.drawRoundRect(x, y, width, 42, 8, 8);
        g.setColor(Color.WHITE); g.setFont(font(18)); centered(g, label, x + width / 2, y + 27); if (active) { g.setColor(c(0xFFE5A1)); g.fillRect(x + 15, y + 15, 10, 10); }
    }

    private void bubble(Graphics2D g, int x, int y, String text) { g.setColor(c(0xFFF0A1)); g.fillRoundRect(x - 15, y - 12, 30, 25, 7, 7); g.setColor(c(0x333A36)); g.setFont(font(17)); centered(g, text, x, y + 7); }
    private void edgeArrow(Graphics2D g, int x, int y, String text) { g.setColor(new Color(8, 25, 23, 170)); g.fillRoundRect(x - 17, y - 25, 34, 50, 8, 8); g.setColor(c(0xFFF3C1)); g.setFont(font(26)); centered(g, text, x, y + 9); }
    private void drawToast(Graphics2D g) { g.setColor(new Color(8, 25, 23, 220)); g.fillRoundRect(WIDTH / 2 - 231, 104, 462, 38, 8, 8); g.setColor(c(0xFFF3C1)); g.setFont(normal(14)); centered(g, toast, WIDTH / 2, 129); }
    private void shade(Graphics2D g, int alpha) { g.setColor(new Color(0, 0, 0, alpha)); g.fillRect(0, 0, WIDTH, HEIGHT); }
    private void card(Graphics2D g, int x, int y, int w, int h) { g.setColor(paper()); g.fillRoundRect(x, y, w, h, 15, 15); g.setColor(outline()); g.setStroke(new BasicStroke(4)); g.drawRoundRect(x, y, w, h, 15, 15); }

    private void drawIris(Graphics2D g) {
        float visibleRadius = transitionFrame <= 18 ? 900f * (1f - transitionFrame / 18f) : 900f * ((transitionFrame - 18) / 18f);
        Area cover = new Area(new Rectangle2D.Float(0, 0, WIDTH, HEIGHT)); cover.subtract(new Area(new Ellipse2D.Float(WIDTH / 2f - visibleRadius, HEIGHT / 2f - visibleRadius, visibleRadius * 2f, visibleRadius * 2f)));
        g.setColor(Color.BLACK); g.fill(cover);
    }

    private void interact() {
        if (session.scene() == Scene.CABIN_APPROACH && distance(session.playerX(), session.playerY(), 950, 440) < 118) {
            session.setStage(Stage.TALK_TO_CURUPIRA); changeScene(Scene.CABIN_INTERIOR, 180f, 540f); return;
        }
        if (session.scene() == Scene.CABIN_INTERIOR && distance(session.playerX(), session.playerY(), 764, 365) < 128) {
            openDialogue(lines("Curupira", "Você não deveria estar aqui, " + session.playerName() + ".", session.playerName(), "Eu não vim para destruir a floresta. Quero aprender a protegê-la.", "Curupira", "Então escute a mata. A floresta não precisa de heróis; precisa de quem esteja disposto a defendê-la."), new Runnable() {
                @Override public void run() { session.setStage(Stage.COMPLETE); saves.save(session); screen = Screen.END; }
            });
        } else showToast("Aproxime-se da porta ou do Curupira para interagir.");
    }

    private void startNew() {
        session.startNew(typedName, slotIndex + 1); saves.save(session);
        openDialogue(lines("Narradora", "Há muito tempo, histórias são contadas sobre seres que vivem nas florestas brasileiras.", "Narradora", session.playerName() + " chega à borda da mata. Um assobio distante anuncia que o Curupira está por perto."), new Runnable() {
            @Override public void run() { screen = Screen.WORLD; beginTransition(null); }
        });
    }

    private void openDialogue(DialogueLine[] lines, Runnable finish) { heldKeys.clear(); dialogue = new ArrayList<DialogueLine>(Arrays.asList(lines)); dialogueIndex = 0; dialogueFinish = finish; screen = Screen.DIALOGUE; }
    private DialogueLine[] lines(String... source) { List<DialogueLine> result = new ArrayList<DialogueLine>(); for (int i = 0; i + 1 < source.length; i += 2) result.add(new DialogueLine(source[i], source[i + 1])); return result.toArray(new DialogueLine[result.size()]); }
    private void advanceDialogue() { if (dialogueIndex + 1 < dialogue.size()) { dialogueIndex++; return; } Runnable finish = dialogueFinish; dialogueFinish = null; if (finish != null) finish.run(); else screen = Screen.WORLD; }
    private void beginTransition(Runnable action) { heldKeys.clear(); transitionFrame = 0; transitionAction = action; }
    private void showToast(String message) { toast = message; toastFrames = 100; }

    private void handleMenu(int key) {
        if (up(key)) menuIndex = (menuIndex + 5) % 6; else if (down(key)) menuIndex = (menuIndex + 1) % 6; else if (confirm(key)) {
            if (menuIndex == 0) { slotIndex = 0; screen = Screen.SLOT_SELECT; }
            else if (menuIndex == 1 || menuIndex == 2) load(menuIndex);
            else if (menuIndex == 3) { settingsReturn = Screen.MENU; screen = Screen.SETTINGS; }
            else if (menuIndex == 4) screen = Screen.EXTRAS;
            else System.exit(0);
        }
    }

    private void load(int slot) { GameSession loaded = saves.load(slot); if (loaded == null) { showToast("O slot " + slot + " está vazio."); return; } session = loaded; screen = Screen.WORLD; beginTransition(null); }
    private void handleSlots(int key) { if (up(key) || down(key)) slotIndex = 1 - slotIndex; else if (confirm(key)) { typedName = ""; screen = Screen.NAME_INPUT; } else if (key == KeyEvent.VK_ESCAPE) screen = Screen.MENU; }
    private void handleName(int key) { if (key == KeyEvent.VK_BACK_SPACE && !typedName.isEmpty()) typedName = typedName.substring(0, typedName.length() - 1); else if (confirm(key)) startNew(); else if (key == KeyEvent.VK_ESCAPE) screen = Screen.SLOT_SELECT; }
    private void handlePause(int key) { if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; } if (up(key)) pauseIndex = (pauseIndex + 4) % 5; else if (down(key)) pauseIndex = (pauseIndex + 1) % 5; else if (confirm(key)) { if (pauseIndex == 0) screen = Screen.WORLD; else if (pauseIndex == 1) { saves.save(session); showToast("Progresso salvo."); screen = Screen.WORLD; } else if (pauseIndex == 2) { settingsReturn = Screen.PAUSE; screen = Screen.SETTINGS; } else if (pauseIndex == 3) { saves.save(session); screen = Screen.MENU; } else { saves.save(session); System.exit(0); } } }
    private void handleSettings(int key) { if (up(key) || down(key)) settingsIndex = 1 - settingsIndex; else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) changeSetting(-1); else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D || confirm(key)) changeSetting(1); else if (key == KeyEvent.VK_ESCAPE) screen = settingsReturn; }
    private void changeSetting(int direction) { if (settingsIndex == 0) { if (direction < 0) accessibility.decreaseText(); else accessibility.increaseText(); } else accessibility.toggleContrast(); }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (screen == Screen.WORLD && movement(key)) heldKeys.add(key);
        if (screen == Screen.MENU) handleMenu(key); else if (screen == Screen.SLOT_SELECT) handleSlots(key); else if (screen == Screen.NAME_INPUT) handleName(key); else if (screen == Screen.WORLD) { if (key == KeyEvent.VK_E) interact(); else if (key == KeyEvent.VK_M) screen = Screen.MAP; else if (key == KeyEvent.VK_F5) { saves.save(session); showToast("Progresso salvo."); } else if (key == KeyEvent.VK_ESCAPE) { heldKeys.clear(); pauseIndex = 0; screen = Screen.PAUSE; } } else if (screen == Screen.DIALOGUE && confirm(key)) advanceDialogue(); else if (screen == Screen.MAP && (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M)) screen = Screen.WORLD; else if (screen == Screen.PAUSE) handlePause(key); else if (screen == Screen.SETTINGS) handleSettings(key); else if (screen == Screen.EXTRAS && (confirm(key) || key == KeyEvent.VK_ESCAPE)) screen = Screen.MENU; else if (screen == Screen.END && (confirm(key) || key == KeyEvent.VK_ESCAPE)) screen = Screen.MENU;
    }

    @Override public void keyReleased(KeyEvent event) { heldKeys.remove(event.getKeyCode()); }
    @Override public void keyTyped(KeyEvent event) { char character = event.getKeyChar(); if (screen == Screen.NAME_INPUT && typedName.length() < 18 && (Character.isLetterOrDigit(character) || character == ' ' || character == '-' || character == '_')) typedName += character; }

    private boolean movement(int key) { return up(key) || down(key) || key == KeyEvent.VK_A || key == KeyEvent.VK_D || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT; }
    private boolean up(int key) { return key == KeyEvent.VK_UP || key == KeyEvent.VK_W; }
    private boolean down(int key) { return key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S; }
    private boolean confirm(int key) { return key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE; }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private float distance(float x1, float y1, float x2, float y2) { float dx = x1 - x2, dy = y1 - y2; return (float) Math.sqrt(dx * dx + dy * dy); }
    private Font font(int size) { int extra = accessibility.textScale() == 2 ? 3 : accessibility.textScale() == 0 ? -2 : 0; return new Font("Dialog", Font.BOLD, Math.max(11, size + extra)); }
    private Font normal(int size) { int extra = accessibility.textScale() == 2 ? 2 : accessibility.textScale() == 0 ? -1 : 0; return new Font("Dialog", Font.PLAIN, Math.max(11, size + extra)); }
    private Color c(int rgb) { return new Color(rgb); }
    private Color ink() { return accessibility.highContrast() ? Color.WHITE : c(0x183343); }
    private Color paper() { return accessibility.highContrast() ? c(0x111111) : c(0xFFF7D5); }
    private Color outline() { return accessibility.highContrast() ? c(0xFFE600) : c(0x173447); }
    private Color selected() { return accessibility.highContrast() ? c(0x0A58CA) : c(0xD85C3A); }
    private void centered(Graphics2D g, String text, int x, int y) { g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y); }
    private int drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, Color color) { g.setColor(color); FontMetrics metrics = g.getFontMetrics(); String line = ""; int current = y; for (String word : text.split(" ")) { String next = line.isEmpty() ? word : line + " " + word; if (metrics.stringWidth(next) > width && !line.isEmpty()) { g.drawString(line, x, current); line = word; current += lineHeight; } else line = next; } if (!line.isEmpty()) g.drawString(line, x, current); return current; }
}
