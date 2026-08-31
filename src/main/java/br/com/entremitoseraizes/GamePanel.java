package br.com.entremitoseraizes;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
import javax.swing.JPanel;
import javax.swing.Timer;

/** Tela principal: menu, fase do Curupira, câmera, colisões e interfaces. */
final class GamePanel extends JPanel implements KeyListener {
    private static final long serialVersionUID = 2L;
    private static final int WIDTH = 1152;
    private static final int HEIGHT = 720;
    private static final float PLAYER_RADIUS = 18f;
    private static final float SPEED = 210f;

    private final AccessibilitySettings accessibility = new AccessibilitySettings();
    private final SaveManager saves = new SaveManager();
    private final Set<Integer> heldKeys = new HashSet<Integer>();
    private final List<Obstacle> obstacles = new ArrayList<Obstacle>();
    private final Timer clock;

    private BufferedImage forestImage;
    private BufferedImage cabinImage;
    private BufferedImage interiorImage;
    private BufferedImage titleImage;
    private BufferedImage curupiraImage;
    private Image menuLoop;
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
    private String[] choices = new String[0];
    private int choiceIndex;
    private ChoiceType choiceType;
    private float cameraX;
    private float cameraY;
    private int transitionFrame = -1;
    private Runnable transitionChange;
    private int animationFrame;
    private long lastTick = System.nanoTime();
    private long lastAutoSave;
    private int toastFrames;
    private String toast = "";
    private boolean introOnBlack;

    private enum ChoiceType { PATH, FALSE_PATH }

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(this);
        loadAssets();
        buildObstacles();
        clock = new Timer(16, event -> tick());
        clock.start();
    }

    @Override public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    void saveIfRunning() {
        if (screen == Screen.WORLD || screen == Screen.DIALOGUE || screen == Screen.PAUSE || screen == Screen.MAP) {
            saves.save(session);
        }
    }

    private void loadAssets() {
        forestImage = loadStatic("Cenarios", "CRPR-pt1.jpeg");
        cabinImage = loadStatic("Cenarios", "CRPR-pt2.jpeg");
        interiorImage = loadStatic("Cenarios", "CRPR-pt3.jpeg");
        titleImage = loadStatic("Cenarios", "Titulo.png");
        curupiraImage = loadStatic("Personagens", "CURUPIRA.PNG");
        File loop = asset("Cenarios", "MENU_loop.gif");
        // Toolkit carrega o GIF grande de forma assíncrona e mantém seus quadros animados.
        if (loop.isFile()) menuLoop = Toolkit.getDefaultToolkit().createImage(loop.getAbsolutePath());
        if (menuLoop == null) menuLoop = loadStatic("Cenarios", "MENU.jpeg");
    }

    private BufferedImage loadStatic(String folder, String name) {
        try {
            File file = asset(folder, name);
            return file.isFile() ? ImageIO.read(file) : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private File asset(String folder, String name) {
        return new File(new File(new File("Image"), folder), name);
    }

    private void buildObstacles() {
        int[][] trees = {
            {380, 420}, {610, 380}, {890, 510}, {1150, 330}, {1500, 410}, {1760, 340},
            {2050, 450}, {2380, 300}, {2560, 390}, {3350, 300}, {3400, 640}, {3100, 1480},
            {2700, 1570}, {2300, 1710}, {1850, 1580}, {1450, 1660}, {990, 1580}, {520, 1660},
            {470, 1040}, {760, 940}, {1100, 1160}, {1470, 1050}, {1740, 1240}, {2420, 1000},
            {2760, 1120}, {3250, 1260}, {3350, 900}, {2050, 780}, {1500, 700}, {880, 680}
        };
        for (int[] tree : trees) obstacles.add(new Obstacle(tree[0], tree[1], 92, 92, ObstacleKind.TREE));
        obstacles.add(new Obstacle(2895, 305, 355, 270, ObstacleKind.HOUSE));
        obstacles.add(new Obstacle(460, 1350, 430, 90, ObstacleKind.WATER));
        obstacles.add(new Obstacle(1280, 1320, 350, 82, ObstacleKind.WATER));
        obstacles.add(new Obstacle(2580, 820, 210, 62, ObstacleKind.LOG));
        obstacles.add(new Obstacle(2950, 1450, 230, 70, ObstacleKind.LOG));
    }

    private void tick() {
        long now = System.nanoTime();
        float delta = Math.min(0.05f, (now - lastTick) / 1_000_000_000f);
        lastTick = now;
        animationFrame++;
        if (toastFrames > 0) toastFrames--;
        if (transitionFrame >= 0) {
            transitionFrame++;
            if (transitionFrame == 18 && transitionChange != null) transitionChange.run();
            if (transitionFrame >= 36) { transitionFrame = -1; transitionChange = null; }
        } else if (screen == Screen.WORLD) {
            updatePlayer(delta);
            updateRace();
            updateCamera(delta);
            if (System.currentTimeMillis() - lastAutoSave > 6000L) persist(false);
        }
        repaint();
    }

    private void updatePlayer(float delta) {
        float dx = 0f;
        float dy = 0f;
        if (heldKeys.contains(KeyEvent.VK_W) || heldKeys.contains(KeyEvent.VK_UP)) dy -= 1f;
        if (heldKeys.contains(KeyEvent.VK_S) || heldKeys.contains(KeyEvent.VK_DOWN)) dy += 1f;
        if (heldKeys.contains(KeyEvent.VK_A) || heldKeys.contains(KeyEvent.VK_LEFT)) dx -= 1f;
        if (heldKeys.contains(KeyEvent.VK_D) || heldKeys.contains(KeyEvent.VK_RIGHT)) dx += 1f;
        if (dx == 0f && dy == 0f) return;
        float factor = (dx != 0f && dy != 0f) ? 0.7071f : 1f;
        float nextX = session.playerX() + dx * SPEED * factor * delta;
        float nextY = session.playerY() + dy * SPEED * factor * delta;
        if (session.scene() == Scene.CABIN) moveInside(nextX, nextY);
        else moveOutside(nextX, nextY);
    }

    private void moveOutside(float x, float y) {
        float safeX = session.playerX();
        float safeY = session.playerY();
        if (!blocked(x, safeY)) safeX = x;
        if (!blocked(safeX, y)) safeY = y;
        session.setPosition(safeX, safeY);
    }

    private void moveInside(float x, float y) {
        x = clamp(x, 88f, 1070f);
        y = clamp(y, 300f, 640f);
        Rectangle2D.Float player = new Rectangle2D.Float(x - PLAYER_RADIUS, y - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
        Rectangle2D.Float table = new Rectangle2D.Float(505, 565, 190, 85);
        Rectangle2D.Float bed = new Rectangle2D.Float(540, 215, 230, 170);
        if (!player.intersects(table) && !player.intersects(bed)) session.setPosition(x, y);
    }

    private boolean blocked(float x, float y) {
        if (x < PLAYER_RADIUS || y < PLAYER_RADIUS || x > GameSession.OUTDOOR_WIDTH - PLAYER_RADIUS || y > GameSession.OUTDOOR_HEIGHT - PLAYER_RADIUS) return true;
        Rectangle2D.Float player = new Rectangle2D.Float(x - PLAYER_RADIUS, y - PLAYER_RADIUS, PLAYER_RADIUS * 2, PLAYER_RADIUS * 2);
        for (Obstacle obstacle : obstacles) if (player.intersects(obstacle.bounds)) return true;
        return false;
    }

    private void updateCamera(float delta) {
        if (session.scene() != Scene.OUTDOOR) { cameraX = 0; cameraY = 0; return; }
        float targetX = clamp(session.playerX() - WIDTH / 2f, 0, GameSession.OUTDOOR_WIDTH - WIDTH);
        float targetY = clamp(session.playerY() - HEIGHT / 2f, 0, GameSession.OUTDOOR_HEIGHT - HEIGHT);
        float smooth = Math.min(1f, delta * 7f);
        cameraX += (targetX - cameraX) * smooth;
        cameraY += (targetY - cameraY) * smooth;
    }

    private void updateRace() {
        if (!session.raceIsActive()) return;
        if (System.currentTimeMillis() >= session.raceEndsAt()) {
            session.setPosition(2300, 1260);
            session.startRace();
            persist(true);
            showToast("Os invasores alcançaram a trilha. Tente novamente!");
        } else if (distance(session.playerX(), session.playerY(), 3260, 990) < 92) {
            session.stopRace();
            session.setStage(Stage.CONFRONT);
            persist(true);
            openDialogue(lines(
                "Narradora", "Você chega à árvore ancestral antes deles. Um machado se ergue, mas um assobio corta a mata: FIIIIIIU!",
                "Curupira", "Agora, " + session.playerName() + ", vamos confundi-los e levá-los para fora sem ferir ninguém."), null, false);
        }
    }

    private void persist(boolean visible) {
        saves.save(session);
        lastAutoSave = System.currentTimeMillis();
        if (visible) showToast("Progresso salvo no slot " + session.activeSlot() + ".");
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        double scale = Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT);
        g.translate((getWidth() - WIDTH * scale) / 2d, (getHeight() - HEIGHT * scale) / 2d);
        g.scale(scale, scale);
        switch (screen) {
            case MENU: drawMenu(g); break;
            case SLOT_SELECT: drawSlotSelect(g); break;
            case NAME_INPUT: drawNameInput(g); break;
            case SETTINGS: drawSettings(g); break;
            case EXTRAS: drawExtras(g); break;
            case END: drawEnd(g); break;
            default:
                drawWorld(g);
                if (screen == Screen.DIALOGUE) drawDialogue(g);
                if (screen == Screen.CHOICE) drawChoice(g);
                if (screen == Screen.MAP) drawMap(g);
                if (screen == Screen.PAUSE) drawPause(g);
        }
        if (transitionFrame >= 0) drawIrisTransition(g);
        g.dispose();
    }

    private void drawMenu(Graphics2D g) {
        drawMenuBackground(g);
        g.setColor(new Color(8, 21, 29, 130));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        if (titleImage != null) g.drawImage(titleImage, 337, 30, 478, 238, this);
        else {
            g.setFont(font(44)); g.setColor(c(0xFFE5A1)); centered(g, "ENTRE MITOS E RAÍZES", WIDTH / 2, 168);
        }
        g.setFont(normal(15)); g.setColor(c(0xFFF4CE)); centered(g, "Aventura 2D de proteção à floresta", WIDTH / 2, 266);
        String[] options = { "Novo jogo", "Continuar - slot 1", "Continuar - slot 2", "Configurações", "Extras", "Sair do jogo" };
        for (int i = 0; i < options.length; i++) drawMenuOption(g, options[i], 318 + i * 56, i == menuIndex);
        g.setColor(c(0xFFF4CE)); g.setFont(normal(14));
        centered(g, "WASD / setas para escolher • Enter para confirmar", WIDTH / 2, 674);
    }

    private void drawMenuBackground(Graphics2D g) {
        if (menuLoop != null) g.drawImage(menuLoop, 0, 0, WIDTH, HEIGHT, this);
        else { g.setColor(c(0x164D43)); g.fillRect(0, 0, WIDTH, HEIGHT); }
    }

    private void drawMenuOption(Graphics2D g, String text, int y, boolean selected) {
        int x = 380, w = 392;
        g.setColor(selected ? c(0xD85C3A) : new Color(17, 45, 48, 220));
        g.fillRoundRect(x, y, w, 42, 8, 8);
        g.setStroke(new BasicStroke(2)); g.setColor(c(0xFFE5A1)); g.drawRoundRect(x, y, w, 42, 8, 8);
        g.setColor(Color.WHITE); g.setFont(font(18)); centered(g, text, WIDTH / 2, y + 27);
        if (selected) { g.setColor(c(0xFFE5A1)); g.fillRect(x + 15, y + 15, 10, 10); }
    }

    private void drawSlotSelect(Graphics2D g) {
        drawMenuBackground(g); drawDarkOverlay(g, 125);
        drawCard(g, 236, 126, 680, 468);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "ESCOLHA UM SLOT", WIDTH / 2, 188);
        g.setFont(normal(16)); centered(g, "Seu apelido e progresso ficam somente neste computador.", WIDTH / 2, 220);
        for (int i = 0; i < 2; i++) {
            int y = 276 + i * 116; boolean selected = i == slotIndex;
            g.setColor(selected ? selected() : c(0xDFE9BD)); g.fillRoundRect(290, y, 572, 86, 9, 9);
            g.setColor(outline()); g.drawRoundRect(290, y, 572, 86, 9, 9);
            g.setColor(selected && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(20));
            g.drawString("SLOT " + (i + 1), 322, y + 32);
            g.setFont(normal(14)); drawWrapped(g, saves.summary(i + 1), 322, y + 59, 490, 18, selected && !accessibility.highContrast() ? Color.WHITE : ink());
        }
        g.setFont(normal(14)); g.setColor(ink()); centered(g, "Selecionar um slot inicia uma nova aventura e substitui o progresso dele.", WIDTH / 2, 545);
        centered(g, "Esc para voltar", WIDTH / 2, 570);
    }

    private void drawNameInput(Graphics2D g) {
        drawMenuBackground(g); drawDarkOverlay(g, 135); drawCard(g, 270, 170, 612, 390);
        g.setColor(ink()); g.setFont(font(28)); centered(g, "COMO VOCÊ SE CHAMA?", WIDTH / 2, 238);
        g.setFont(normal(16)); centered(g, "O Curupira quer saber quem entrou na mata.", WIDTH / 2, 276);
        g.setColor(c(0xF9F0CF)); g.fillRoundRect(343, 322, 466, 58, 8, 8); g.setColor(outline()); g.drawRoundRect(343, 322, 466, 58, 8, 8);
        g.setColor(ink()); g.setFont(font(22)); g.drawString(typedName.isEmpty() ? "Digite um apelido" : typedName + "_", 367, 360);
        g.setFont(normal(14)); centered(g, "Máximo de 18 caracteres. Enter para iniciar • Esc para voltar", WIDTH / 2, 440);
        centered(g, "Slot selecionado: " + (slotIndex + 1), WIDTH / 2, 470);
    }

    private void drawWorld(Graphics2D g) {
        if (session.scene() == Scene.CABIN) drawCabin(g); else drawOutside(g);
        drawHud(g);
        if (toastFrames > 0) drawToast(g);
    }

    private void drawOutside(Graphics2D g) {
        g.setColor(c(0x102B2B)); g.fillRect(0, 0, WIDTH, HEIGHT);
        Graphics2D world = (Graphics2D) g.create();
        world.translate(-cameraX, -cameraY);
        if (forestImage != null) world.drawImage(forestImage, 0, 0, (int) GameSession.OUTDOOR_WIDTH, (int) GameSession.OUTDOOR_HEIGHT, this);
        else { world.setColor(c(0x4F8C55)); world.fillRect(0, 0, (int) GameSession.OUTDOOR_WIDTH, (int) GameSession.OUTDOOR_HEIGHT); }
        drawWorldDetails(world);
        if (cabinImage != null) world.drawImage(cabinImage, 1950, 0, 1408, 768, this);
        drawCamp(world);
        drawPoints(world);
        drawCurupiraOutside(world);
        drawAnimal(world);
        drawPlayer(world, session.playerX(), session.playerY());
        world.dispose();
        drawVignette(g);
    }

    private void drawWorldDetails(Graphics2D g) {
        for (Obstacle obstacle : obstacles) {
            if (obstacle.kind == ObstacleKind.TREE) drawTree(g, obstacle.bounds.x, obstacle.bounds.y);
            else if (obstacle.kind == ObstacleKind.WATER) drawWater(g, obstacle.bounds);
            else if (obstacle.kind == ObstacleKind.LOG) drawLog(g, obstacle.bounds);
        }
        drawAncientTree(g, 3260, 990);
        drawSafeGlade(g, 1790, 1500);
        drawBrokenArea(g, 2270, 880);
    }

    private void drawTree(Graphics2D g, float x, float y) {
        g.setColor(c(0x1B513D)); g.fillOval((int) x, (int) y, 92, 76);
        g.setColor(c(0x2B7A50)); g.fillOval((int) x + 8, (int) y - 8, 76, 67);
        g.setColor(c(0x4E9B57)); g.fillOval((int) x + 22, (int) y - 14, 44, 42);
        g.setColor(c(0x5D3827)); g.fillRect((int) x + 39, (int) y + 50, 15, 48);
        g.setColor(c(0x8E5B32)); g.fillRect((int) x + 45, (int) y + 50, 5, 45);
    }

    private void drawWater(Graphics2D g, Rectangle2D.Float b) {
        g.setColor(c(0x2C8CB7)); g.fillRoundRect((int) b.x, (int) b.y, (int) b.width, (int) b.height, 30, 30);
        g.setColor(c(0x9CD9E4)); for (int x = (int) b.x + 18; x < b.x + b.width; x += 42) g.fillRect(x, (int) b.y + 24, 24, 3);
    }

    private void drawLog(Graphics2D g, Rectangle2D.Float b) {
        g.setColor(c(0x4E3022)); g.fillRoundRect((int) b.x, (int) b.y, (int) b.width, (int) b.height, 16, 16);
        g.setColor(c(0xA26D3E)); g.fillRoundRect((int) b.x + 8, (int) b.y + 10, (int) b.width - 16, 12, 8, 8);
    }

    private void drawAncientTree(Graphics2D g, int x, int y) {
        g.setColor(c(0x32251E)); g.fillOval(x - 125, y - 160, 250, 270);
        g.setColor(c(0x6D432C)); g.fillRect(x - 38, y - 8, 76, 230);
        g.setColor(c(0xA46A38)); g.fillRect(x - 12, y + 6, 18, 206);
        g.setColor(c(0x1A5A3C)); g.fillOval(x - 190, y - 210, 380, 210);
        g.setColor(c(0x397C48)); g.fillOval(x - 130, y - 252, 265, 175);
        g.setColor(c(0x70AE50)); g.fillOval(x - 55, y - 260, 115, 85);
    }

    private void drawSafeGlade(Graphics2D g, int x, int y) {
        g.setColor(new Color(200, 240, 144, 100)); g.fillOval(x - 120, y - 75, 240, 150);
        g.setColor(c(0xF6CE55)); for (int i = 0; i < 10; i++) g.fillOval(x - 90 + i * 18, y + (i % 3) * 16, 6, 6);
    }

    private void drawBrokenArea(Graphics2D g, int x, int y) {
        g.setColor(new Color(70, 38, 25, 160)); g.fillOval(x - 180, y - 110, 370, 210);
        g.setColor(c(0x74452A)); g.fillRect(x - 95, y - 15, 90, 18);
        g.setColor(c(0x34302A)); g.fillRect(x + 60, y + 55, 58, 10);
    }

    private void drawCamp(Graphics2D g) {
        if (session.stage().ordinal() < Stage.INVESTIGATE_CAMP.ordinal()) return;
        int x = 620, y = 410;
        g.setColor(c(0xC9A06B)); g.fillPolygon(new int[] {x, x + 105, x + 210}, new int[] {y + 145, y + 35, y + 145}, 3);
        g.setColor(c(0x6A4430)); g.fillRect(x + 30, y + 145, 150, 70);
        g.setColor(c(0xE76F39)); g.fillOval(x + 235, y + 175, 32, 20);
        g.setColor(c(0xF5C04E)); g.fillOval(x + 243, y + 179, 16, 9);
        g.setColor(c(0x784728)); g.fillRect(x + 285, y + 120, 48, 45);
        g.setColor(c(0x9A6A42)); g.fillRect(x + 350, y + 150, 34, 55);
    }

    private void drawPoints(Graphics2D g) {
        for (WorldPoint point : activePoints()) {
            int x = (int) point.x, y = (int) point.y;
            int bob = animationFrame / 12 % 2 == 0 ? 0 : -3;
            g.setColor(c(0xFFF0A1)); g.fillRoundRect(x - 11, y - 54 + bob, 22, 26, 7, 7);
            g.setColor(c(0x333A36)); g.setFont(font(17)); g.drawString("E", x - 6, y - 36 + bob);
            g.setFont(normal(12)); g.setColor(c(0xFFF4CE)); g.drawString(point.label, x - 44, y + 34);
            drawPointObject(g, point);
        }
        if (session.stage() == Stage.RACE_TO_TREE) {
            g.setColor(c(0xFFE56B)); g.fillOval(3246, 792, 28, 28);
            g.setColor(c(0xFFF4CE)); g.setFont(font(18)); g.drawString("★", 3251, 813);
        }
    }

    private void drawPointObject(Graphics2D g, WorldPoint point) {
        int x = (int) point.x, y = (int) point.y;
        if (point.id.startsWith("clue") || point.id.startsWith("track")) {
            g.setColor(c(0x62432C)); g.fillOval(x - 22, y - 8, 15, 9); g.fillOval(x + 5, y - 8, 15, 9);
        } else if (point.id.equals("animal")) {
            g.setColor(c(0xD7A44F)); g.fillOval(x - 23, y - 13, 45, 28); g.setColor(c(0x5D3827)); g.drawOval(x - 30, y - 20, 60, 42);
        } else if (point.id.startsWith("restore")) {
            g.setColor(c(0x75B85A)); g.fillOval(x - 16, y - 15, 32, 30); g.setColor(c(0x70442F)); g.fillRect(x - 3, y, 7, 28);
        } else { g.setColor(c(0xD5B17B)); g.fillOval(x - 13, y - 13, 26, 26); }
    }

    private void drawCurupiraOutside(Graphics2D g) {
        if (session.stage().ordinal() < Stage.INVESTIGATE.ordinal()) return;
        drawCurupira(g, 2750, 690, 72, 98);
    }

    private void drawCurupira(Graphics2D g, int x, int y, int w, int h) {
        if (curupiraImage != null) g.drawImage(curupiraImage, x, y, w, h, this);
        else { g.setColor(c(0xE85B24)); g.fillOval(x + 10, y, w - 20, h / 2); g.setColor(c(0x68A351)); g.fillRect(x + 18, y + h / 2, w - 36, h / 2); }
    }

    private void drawAnimal(Graphics2D g) {
        if (!session.animalFollowing()) return;
        int x = (int) session.playerX() - 48, y = (int) session.playerY() + 24;
        g.setColor(c(0xC48B52)); g.fillOval(x, y, 28, 17); g.fillOval(x + 20, y - 8, 15, 14);
        g.setColor(c(0xFFF4CE)); g.fillRect(x + 31, y - 3, 3, 3);
    }

    private void drawPlayer(Graphics2D g, float x, float y) {
        int px = (int) x, py = (int) y;
        g.setColor(new Color(22, 31, 28, 100)); g.fillOval(px - 20, py + 18, 40, 12);
        g.setColor(c(0x304D91)); g.fillRect(px - 12, py - 1, 24, 29);
        g.setColor(c(0xF1BE90)); g.fillRect(px - 10, py - 22, 20, 22);
        g.setColor(c(0x2A2831)); g.fillRect(px - 12, py - 30, 24, 10);
        g.setColor(c(0xD65B44)); g.fillRect(px - 15, py - 15, 30, 5);
        g.setColor(c(0xFFF4CE)); g.setFont(normal(12)); centeredAt(g, session.playerName(), px, py - 39);
    }

    private void drawCabin(Graphics2D g) {
        if (interiorImage != null) g.drawImage(interiorImage, 0, 0, WIDTH, HEIGHT, this);
        else { g.setColor(c(0x5B3828)); g.fillRect(0, 0, WIDTH, HEIGHT); }
        drawCurupira(g, 666, 215, 112, 134);
        drawPlayer(g, session.playerX(), session.playerY());
        if (distance(session.playerX(), session.playerY(), 720, 350) < 110) drawInteractionBubble(g, 720, 210, "E");
        g.setColor(new Color(12, 27, 25, 170)); g.fillRoundRect(26, 24, 430, 62, 10, 10);
        g.setColor(c(0xFFF4CE)); g.setFont(font(18)); g.drawString("Cabana do Curupira", 48, 52);
        g.setFont(normal(13)); g.drawString("Aproxime-se do guardião para conversar.", 48, 74);
    }

    private void drawInteractionBubble(Graphics2D g, int x, int y, String key) {
        g.setColor(c(0xFFF0A1)); g.fillRoundRect(x - 15, y - 12, 30, 25, 7, 7);
        g.setColor(c(0x333A36)); g.setFont(font(17)); centeredAt(g, key, x, y + 7);
    }

    private void drawHud(Graphics2D g) {
        if (screen != Screen.WORLD) return;
        g.setColor(new Color(10, 30, 29, 195)); g.fillRoundRect(20, 18, 695, 72, 10, 10);
        g.setColor(c(0xFFE79A)); g.setFont(font(14)); g.drawString("FASE 1  •  O CHAMADO DO CURUPIRA", 40, 43);
        g.setColor(Color.WHITE); g.setFont(font(17)); drawWrapped(g, session.stage().objective(), 40, 70, 630, 20, Color.WHITE);
        g.setColor(new Color(10, 30, 29, 195)); g.fillRoundRect(870, 18, 262, 72, 10, 10);
        g.setColor(c(0xFFF4CE)); g.setFont(normal(12)); g.drawString("E interagir   M mapa   F5 salvar", 890, 44);
        g.drawString("Esc pausa   Slot " + session.activeSlot(), 890, 68);
        if (session.raceIsActive()) {
            long remaining = Math.max(0L, session.raceEndsAt() - System.currentTimeMillis());
            g.setColor(c(0xA7342C)); g.fillRoundRect(464, 98, 224, 37, 8, 8);
            g.setColor(Color.WHITE); g.setFont(font(19)); centered(g, "CORRA: " + (remaining / 1000L + 1) + "s", 576, 123);
        }
    }

    private void drawDialogue(Graphics2D g) {
        if (introOnBlack) { g.setColor(Color.BLACK); g.fillRect(0, 0, WIDTH, HEIGHT); drawIntroSpecks(g); }
        else drawDarkOverlay(g, 115);
        if (dialogue.isEmpty()) return;
        DialogueLine line = dialogue.get(dialogueIndex);
        g.setColor(paper()); g.fillRoundRect(46, 492, 1060, 184, 13, 13);
        g.setColor(outline()); g.setStroke(new BasicStroke(4)); g.drawRoundRect(46, 492, 1060, 184, 13, 13);
        g.setColor(selected()); g.fillRoundRect(72, 472, 255, 41, 8, 8);
        g.setColor(Color.WHITE); g.setFont(font(20)); g.drawString(line.speaker, 92, 500);
        g.setColor(ink()); g.setFont(normal(21)); drawWrapped(g, line.text, 82, 550, 960, 29, ink());
        g.setFont(normal(13)); g.setColor(outline()); g.drawString("Enter / Espaço para continuar", 858, 652);
        g.drawString((dialogueIndex + 1) + "/" + dialogue.size(), 82, 652);
    }

    private void drawIntroSpecks(Graphics2D g) {
        g.setColor(c(0x6A9850));
        for (int i = 0; i < 30; i++) g.fillRect((i * 113) % WIDTH, 80 + (i * 59) % 370, 2, 2);
        g.setColor(c(0xFFF4CE)); g.setFont(normal(15)); centered(g, "Folhas balançando  •  Pássaros  •  Água corrente  •  Insetos", WIDTH / 2, 148);
    }

    private void drawChoice(Graphics2D g) {
        drawDarkOverlay(g, 165); drawCard(g, 220, 154, 712, 430);
        g.setColor(ink()); g.setFont(font(27)); centered(g, choiceType == ChoiceType.PATH ? "QUAL CAMINHO É O VERDADEIRO?" : "ORGANIZE A TRILHA FALSA", WIDTH / 2, 212);
        g.setFont(normal(16)); centered(g, "Observe as pistas da mata e escolha com cuidado.", WIDTH / 2, 243);
        for (int i = 0; i < choices.length; i++) {
            int y = 280 + i * 70; boolean active = i == choiceIndex;
            g.setColor(active ? selected() : c(0xDFE9BD)); g.fillRoundRect(272, y, 608, 48, 7, 7);
            g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(16)); g.drawString((char) ('A' + i) + ". " + choices[i], 300, y + 30);
        }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "↑ ↓ para escolher • Enter para confirmar", WIDTH / 2, 548);
    }

    private void drawMap(Graphics2D g) {
        drawDarkOverlay(g, 180); drawCard(g, 150, 80, 852, 570);
        g.setColor(ink()); g.setFont(font(27)); centered(g, "MAPA DA MATA", WIDTH / 2, 127);
        int x0 = 230, y0 = 170, w = 690, h = 365;
        g.setColor(c(0x80B95A)); g.fillRoundRect(x0, y0, w, h, 16, 16);
        g.setColor(c(0x3B8EB5)); g.fillRoundRect(x0 + 85, y0 + 268, 190, 18, 9, 9);
        g.setColor(c(0x59402E)); g.fillRoundRect(x0 + 555, y0 + 52, 80, 60, 8, 8);
        g.setColor(c(0x3C7447)); for (Obstacle o : obstacles) if (o.kind == ObstacleKind.TREE) {
            int tx = x0 + (int) (o.bounds.x / GameSession.OUTDOOR_WIDTH * w);
            int ty = y0 + (int) (o.bounds.y / GameSession.OUTDOOR_HEIGHT * h);
            g.fillOval(tx, ty, 15, 15);
        }
        for (WorldPoint point : activePoints()) {
            int px = x0 + (int) (point.x / GameSession.OUTDOOR_WIDTH * w);
            int py = y0 + (int) (point.y / GameSession.OUTDOOR_HEIGHT * h);
            g.setColor(c(0xD85C3A)); g.fillRect(px - 4, py - 4, 9, 9);
        }
        int playerX = x0 + (int) (session.playerX() / (session.scene() == Scene.OUTDOOR ? GameSession.OUTDOOR_WIDTH : WIDTH) * w);
        int playerY = y0 + (int) (session.playerY() / (session.scene() == Scene.OUTDOOR ? GameSession.OUTDOOR_HEIGHT : HEIGHT) * h);
        g.setColor(c(0x183343)); g.fillOval(playerX - 7, playerY - 7, 14, 14);
        g.setColor(ink()); g.setFont(normal(16)); centered(g, "● Você    ■ Objetivo atual    Cabana ao nordeste", WIDTH / 2, 579);
        centered(g, "Esc ou M para fechar", WIDTH / 2, 613);
    }

    private void drawPause(Graphics2D g) {
        drawDarkOverlay(g, 180); drawCard(g, 352, 132, 448, 456);
        g.setColor(ink()); g.setFont(font(30)); centered(g, "MENU DA PARTIDA", WIDTH / 2, 190);
        String[] options = { "Continuar", "Salvar agora", "Configurações", "Voltar ao lobby", "Sair do jogo" };
        for (int i = 0; i < options.length; i++) {
            int y = 225 + i * 58; boolean active = i == pauseIndex;
            g.setColor(active ? selected() : c(0xDFE9BD)); g.fillRoundRect(392, y, 368, 42, 7, 7);
            g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(17)); centered(g, options[i], WIDTH / 2, y + 27);
        }
        g.setColor(ink()); g.setFont(normal(13)); centered(g, "↑ ↓ e Enter • Esc para continuar", WIDTH / 2, 560);
    }

    private void drawSettings(Graphics2D g) {
        drawMenuBackground(g); drawDarkOverlay(g, 140); drawCard(g, 270, 150, 612, 410);
        g.setColor(ink()); g.setFont(font(28)); centered(g, "CONFIGURAÇÕES", WIDTH / 2, 210);
        String[] labels = { "Tamanho do texto", "Alto contraste" };
        String[] values = { accessibility.textSizeName(), accessibility.highContrast() ? "Ativado" : "Desativado" };
        for (int i = 0; i < 2; i++) {
            int y = 260 + i * 94; boolean active = i == settingsIndex;
            g.setColor(active ? selected() : c(0xDFE9BD)); g.fillRoundRect(320, y, 512, 71, 8, 8);
            g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(19)); g.drawString(labels[i], 350, y + 30);
            g.setFont(normal(15)); g.drawString("◀   " + values[i] + "   ▶", 350, y + 55);
        }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "↑ ↓ escolhe • ← → ou Enter altera • Esc volta", WIDTH / 2, 512);
    }

    private void drawExtras(Graphics2D g) {
        drawMenuBackground(g); drawDarkOverlay(g, 140); drawCard(g, 276, 165, 600, 382);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "EXTRAS", WIDTH / 2, 225);
        g.setFont(normal(16)); centered(g, "Entre Mitos e Raízes", WIDTH / 2, 263);
        g.setFont(font(22)); centered(g, "Antonio Andson", WIDTH / 2, 339); centered(g, "Sophia Hellen", WIDTH / 2, 386);
        g.setFont(normal(15)); centered(g, "Criação e desenvolvimento", WIDTH / 2, 432);
        centered(g, "Fase de teste: O Chamado do Curupira", WIDTH / 2, 462);
        centered(g, "Esc ou Enter para voltar", WIDTH / 2, 516);
    }

    private void drawEnd(Graphics2D g) {
        drawMenuBackground(g); drawDarkOverlay(g, 140); drawCard(g, 206, 84, 740, 558);
        g.setColor(ink()); g.setFont(font(31)); centered(g, "FASE CONCLUÍDA", WIDTH / 2, 150);
        g.setFont(font(22)); centered(g, "O Chamado do Curupira", WIDTH / 2, 191);
        if (curupiraImage != null) g.drawImage(curupiraImage, 330, 230, 130, 156, this);
        g.setFont(normal(17)); g.setColor(ink());
        int y = 264;
        String[] done = { "Investigou a área destruída", "Encontrou as pistas dos invasores", "Resgatou e escoltou um animal", "Descobriu o acampamento", "Criou uma trilha falsa", "Impediu o desmatamento", "Expulsou os invasores", "Recuperou a floresta" };
        for (String item : done) { g.setColor(c(0x2A9D63)); g.fillRoundRect(490, y - 16, 18, 18, 4, 4); g.setColor(Color.WHITE); g.drawString("✓", 493, y - 2); g.setColor(ink()); g.drawString(item, 520, y); y += 34; }
        g.setFont(normal(17)); drawWrapped(g, "A floresta é o lar de muitos seres. Protegê-la também é proteger aqueles que vivem nela.", 285, 560, 585, 24, ink());
        g.setFont(normal(14)); centered(g, "Enter ou Esc para voltar ao lobby", WIDTH / 2, 614);
    }

    private void drawIrisTransition(Graphics2D g) {
        float fraction = transitionFrame <= 18 ? 1f - transitionFrame / 18f : (transitionFrame - 18) / 18f;
        float radius = 900f * fraction;
        Area covered = new Area(new Rectangle2D.Float(0, 0, WIDTH, HEIGHT));
        covered.subtract(new Area(new Ellipse2D.Float(WIDTH / 2f - radius, HEIGHT / 2f - radius, radius * 2, radius * 2)));
        g.setColor(Color.BLACK); g.fill(covered);
    }

    private void drawCard(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(paper()); g.fillRoundRect(x, y, w, h, 15, 15); g.setColor(outline()); g.setStroke(new BasicStroke(4)); g.drawRoundRect(x, y, w, h, 15, 15);
    }

    private void drawDarkOverlay(Graphics2D g, int alpha) { g.setColor(new Color(0, 0, 0, alpha)); g.fillRect(0, 0, WIDTH, HEIGHT); }
    private void drawVignette(Graphics2D g) { g.setColor(new Color(5, 18, 22, 75)); g.fillRect(0, 0, WIDTH, 12); g.fillRect(0, HEIGHT - 12, WIDTH, 12); }
    private void drawToast(Graphics2D g) { g.setColor(new Color(11, 33, 32, 225)); g.fillRoundRect(343, 103, 466, 38, 8, 8); g.setColor(c(0xFFF4CE)); g.setFont(normal(14)); centered(g, toast, WIDTH / 2, 128); }

    private List<WorldPoint> activePoints() {
        List<WorldPoint> points = new ArrayList<WorldPoint>();
        Stage stage = session.stage();
        if (stage == Stage.INVESTIGATE) addIncomplete(points, clues());
        else if (stage == Stage.FOLLOW_TRACKS) addIncomplete(points, tracks());
        else if (stage == Stage.CHOOSE_PATH) points.add(new WorldPoint("fork", "Bifurcação", 1080, 550));
        else if (stage == Stage.INVESTIGATE_CAMP) points.add(new WorldPoint("camp", "Acampamento", 690, 595));
        else if (stage == Stage.RESCUE_ANIMAL) points.add(new WorldPoint("animal", "Animal preso", 860, 1250));
        else if (stage == Stage.ESCORT_ANIMAL) points.add(new WorldPoint("glade", "Clareira segura", 1790, 1500));
        else if (stage == Stage.FALSE_TRAIL) addIncomplete(points, falseTrail());
        else if (stage == Stage.CONFRONT) addIncomplete(points, confrontation());
        else if (stage == Stage.RESTORE) addIncomplete(points, restoration());
        return points;
    }

    private void addIncomplete(List<WorldPoint> target, WorldPoint[] source) { for (WorldPoint point : source) if (!session.isComplete(point.id)) target.add(point); }
    private WorldPoint[] clues() { return new WorldPoint[] { new WorldPoint("clue-footprints", "Pegadas", 2250, 920), new WorldPoint("clue-axe", "Marcas de machado", 2070, 850), new WorldPoint("clue-trap", "Armadilha", 1900, 1100) }; }
    private WorldPoint[] tracks() { return new WorldPoint[] { new WorldPoint("track-leaves", "Folhas amassadas", 1740, 900), new WorldPoint("track-branch", "Galhos quebrados", 1510, 740), new WorldPoint("track-boots", "Marcas de botas", 1300, 620) }; }
    private WorldPoint[] falseTrail() { return new WorldPoint[] { new WorldPoint("false-branch", "Galhos", 2050, 1310), new WorldPoint("false-leaves", "Folhas", 2180, 1400), new WorldPoint("false-stones", "Pedras", 2320, 1305) }; }
    private WorldPoint[] confrontation() { return new WorldPoint[] { new WorldPoint("confront-sound", "Ativar sons", 2990, 1150), new WorldPoint("confront-block", "Bloquear trilha", 3150, 1280), new WorldPoint("confront-tools", "Recolher machados", 2800, 1370), new WorldPoint("confront-cages", "Abrir gaiolas", 2640, 1160) }; }
    private WorldPoint[] restoration() { return new WorldPoint[] { new WorldPoint("restore-seedlings", "Plantar mudas", 2390, 930), new WorldPoint("restore-traps", "Remover armadilhas", 2160, 1040), new WorldPoint("restore-bridge", "Reparar ponte", 1490, 1280), new WorldPoint("restore-clean", "Limpar área", 2290, 1110) }; }

    private void interact() {
        if (session.scene() == Scene.CABIN) { interactCabin(); return; }
        if (session.stage() == Stage.FIND_CABIN && distance(session.playerX(), session.playerY(), 3130, 625) < 105) { enterCabin(); return; }
        WorldPoint point = nearbyPoint();
        if (point == null) { showToast("Nada importante para investigar aqui."); return; }
        handlePoint(point);
    }

    private void interactCabin() {
        if (session.stage() == Stage.FIND_CABIN && distance(session.playerX(), session.playerY(), 720, 350) < 115) {
            openDialogue(lines(
                "Curupira", "Você não deveria estar aqui. A mata está sendo ferida, " + session.playerName() + ".",
                session.playerName(), "Eu não vim para destruir a floresta. Quero ajudar.",
                "Curupira", "Então prove. Há pegadas humanas, marcas de machado e uma armadilha na clareira. Encontre as três pistas."), new Runnable() {
                    @Override public void run() {
                        session.setStage(Stage.INVESTIGATE); session.setScene(Scene.OUTDOOR); session.setPosition(2520, 780); persist(true);
                        beginTransition(null);
                    }
                }, false);
        } else showToast("Curupira observa em silêncio. Aproxime-se dele.");
    }

    private void enterCabin() {
        openDialogue(lines("Narradora", "Uma cabana surge entre as árvores. A porta está aberta; lá dentro, uma luz vermelha dança junto a uma fogueira."), new Runnable() {
            @Override public void run() {
                beginTransition(new Runnable() {
                    @Override public void run() { session.setScene(Scene.CABIN); session.setPosition(360, 560); persist(true); }
                });
            }
        }, false);
    }

    private void handlePoint(WorldPoint point) {
        Stage stage = session.stage();
        if (stage == Stage.INVESTIGATE) investigate(point);
        else if (stage == Stage.FOLLOW_TRACKS) followTrack(point);
        else if (stage == Stage.CHOOSE_PATH) openPathChoice();
        else if (stage == Stage.INVESTIGATE_CAMP) inspectCamp();
        else if (stage == Stage.RESCUE_ANIMAL) rescueAnimal();
        else if (stage == Stage.ESCORT_ANIMAL) escortAnimal();
        else if (stage == Stage.FALSE_TRAIL) buildFalseTrail(point);
        else if (stage == Stage.CONFRONT) confront(point);
        else if (stage == Stage.RESTORE) restore(point);
    }

    private void investigate(WorldPoint point) {
        session.complete(point.id); persist(true);
        String text = point.id.equals("clue-footprints") ? "Pegadas recentes. Alguém passou por aqui." : point.id.equals("clue-axe") ? "A árvore foi cortada recentemente. O corte deixou a mata mais vulnerável." : "Alguém está caçando nesta floresta. A armadilha não deve ser tocada sem cuidado.";
        if (session.completedAll("clue-footprints", "clue-axe", "clue-trap")) {
            session.setStage(Stage.FOLLOW_TRACKS); persist(true);
            openDialogue(lines("Descoberta", text, "Curupira", "Eles estão por perto. Siga os sinais que deixaram, mas observe: nem todo rastro mostra a verdade."), null, false);
        } else openDialogue(lines("Pista encontrada", text), null, false);
    }

    private void followTrack(WorldPoint point) {
        session.complete(point.id); persist(true);
        if (session.completedAll("track-leaves", "track-branch", "track-boots")) {
            session.setStage(Stage.CHOOSE_PATH); persist(true);
            openDialogue(lines("Narradora", "Folhas amassadas, galhos quebrados e marcas de botas levam até uma bifurcação."), null, false);
        } else openDialogue(lines("Rastro", "Você registra " + point.label.toLowerCase() + ". A direção dos sinais conta mais do que uma única pegada."), null, false);
    }

    private void openPathChoice() {
        choices = new String[] { "Seguir apenas as pegadas para a esquerda", "Observar galhos e marcas de botas à direita", "Escolher o caminho sem pistas" };
        choiceIndex = 0; choiceType = ChoiceType.PATH; screen = Screen.CHOICE;
    }

    private void inspectCamp() {
        session.complete("camp"); session.setStage(Stage.RESCUE_ANIMAL); persist(true);
        openDialogue(lines("Acampamento", "Uma pequena barraca, machados, caixas, gaiolas e armadilhas confirmam a invasão. Você ouve um rosnado vindo da mata.", "Missão", "Encontre o animal preso e liberte-o com cuidado."), null, false);
    }

    private void rescueAnimal() {
        session.complete("animal"); session.setAnimalFollowing(true); session.setStage(Stage.ESCORT_ANIMAL); persist(true);
        openDialogue(lines("Resgate", "Com atenção, você solta a armadilha sem assustar o pequeno animal. Ele escolhe seguir seus passos.", "Missão", "Leve-o para a clareira segura. Evite obstáculos e espere por ele."), null, false);
    }

    private void escortAnimal() {
        session.complete("glade"); session.setAnimalFollowing(false); session.setStage(Stage.FALSE_TRAIL); persist(true);
        openDialogue(lines("Curupira", "A floresta não precisa de heróis. Precisa de quem esteja disposto a protegê-la.", "Narradora", "Ao voltar, você vê árvores marcadas e fogo começando. Os invasores chegaram à parte mais antiga da mata."), null, false);
    }

    private void buildFalseTrail(WorldPoint point) {
        session.complete(point.id); persist(true);
        if (session.completedAll("false-branch", "false-leaves", "false-stones")) {
            openFalseTrailChoice();
        } else openDialogue(lines("Trilha falsa", "Você posiciona " + point.label.toLowerCase() + ". As marcas devem levar os invasores para longe da área protegida."), null, false);
    }

    private void openFalseTrailChoice() {
        choices = new String[] { "Floresta → rio → árvore gigante → trilha falsa → saída", "Árvore gigante → acampamento → rio → saída", "Trilha falsa → floresta → acampamento" };
        choiceIndex = 0; choiceType = ChoiceType.FALSE_PATH; screen = Screen.CHOICE;
    }

    private void confront(WorldPoint point) {
        session.complete(point.id); persist(true);
        if (session.completedAll("confront-sound", "confront-block", "confront-tools", "confront-cages")) {
            session.setStage(Stage.RESTORE); persist(true);
            openDialogue(lines("Caçador", "Ele está em todo lugar! Vamos embora!", "Narradora", "Os invasores fogem sem serem feridos. Agora a mata precisa ser reconstruída."), null, false);
        } else openDialogue(lines("Ação", point.label + " concluído. O Curupira usa o assobio, sons e rastros para confundir os invasores."), null, false);
    }

    private void restore(WorldPoint point) {
        session.complete(point.id); persist(true);
        if (session.completedAll("restore-seedlings", "restore-traps", "restore-bridge", "restore-clean")) {
            session.setStage(Stage.COMPLETE); persist(true);
            openDialogue(lines("Curupira", "Você ajudou a floresta.", session.playerName(), "Então acabou?", "Curupira", "Uma floresta nunca está completamente protegida. Enquanto houver quem a destrua, haverá quem precise defendê-la."), new Runnable() {
                @Override public void run() { screen = Screen.END; }
            }, false);
        } else openDialogue(lines("Reconstrução", point.label + " concluído. A floresta começa a ficar mais viva."), null, false);
    }

    private WorldPoint nearbyPoint() {
        for (WorldPoint point : activePoints()) if (distance(session.playerX(), session.playerY(), point.x, point.y) < 92) return point;
        return null;
    }

    private void resolveChoice() {
        if (choiceType == ChoiceType.PATH) {
            if (choiceIndex == 1) {
                session.setStage(Stage.INVESTIGATE_CAMP); persist(true);
                openDialogue(lines("Curupira", "Nem sempre o caminho que parece certo é o verdadeiro. As marcas de botas mostram que eles seguiram à direita."), null, false);
            } else openDialogue(lines("Curupira", "Esses rastros foram deixados para enganar. Observe galhos, folhas e marcas de botas antes de tentar novamente."), null, false);
        } else if (choiceIndex == 0) {
            session.startRace(); session.setPosition(2310, 1260); persist(true);
            openDialogue(lines("Curupira", "Funcionou! Mas um caçador viu a trilha real. Corra até a árvore ancestral antes deles!"), null, false);
        } else openDialogue(lines("Curupira", "Os rastros não estão funcionando. Reorganize a sequência e tente novamente."), null, false);
    }

    private void startNewGame() {
        session.startNew(typedName, slotIndex + 1); persist(true); introOnBlack = true;
        openDialogue(lines(
            "Narradora", "Há muito tempo, histórias são contadas sobre seres que vivem nas florestas brasileiras.",
            "Narradora", "Alguns protegem as águas. Outros protegem os animais. E entre as árvores vive um dos mais conhecidos guardiões da mata...",
            "Som", "Folhas balançam. Pássaros respondem. A água corre. Um assobio atravessa o silêncio: FIIIIIIU!",
            "Narradora", session.playerName() + " entra pela borda da mata. Duas pequenas pegadas voltadas para trás desaparecem no caminho."), new Runnable() {
                @Override public void run() { introOnBlack = false; screen = Screen.WORLD; beginTransition(null); }
            }, true);
    }

    private void openDialogue(DialogueLine[] lines, Runnable finish, boolean black) {
        heldKeys.clear(); dialogue = new ArrayList<DialogueLine>(Arrays.asList(lines)); dialogueIndex = 0; dialogueFinish = finish; introOnBlack = black; screen = Screen.DIALOGUE;
    }

    private DialogueLine[] lines(String... values) {
        List<DialogueLine> result = new ArrayList<DialogueLine>();
        for (int i = 0; i + 1 < values.length; i += 2) result.add(new DialogueLine(values[i], values[i + 1]));
        return result.toArray(new DialogueLine[result.size()]);
    }

    private void advanceDialogue() {
        if (dialogueIndex + 1 < dialogue.size()) { dialogueIndex++; return; }
        Runnable finish = dialogueFinish; dialogueFinish = null; introOnBlack = false;
        if (finish != null) finish.run(); else screen = Screen.WORLD;
    }

    private void beginTransition(Runnable change) { heldKeys.clear(); transitionFrame = 0; transitionChange = change; }
    private void showToast(String message) { toast = message; toastFrames = 120; }

    private void handleMenuKey(int key) {
        if (isUp(key)) menuIndex = (menuIndex + 5) % 6;
        else if (isDown(key)) menuIndex = (menuIndex + 1) % 6;
        else if (isConfirm(key)) {
            if (menuIndex == 0) { slotIndex = 0; screen = Screen.SLOT_SELECT; }
            else if (menuIndex == 1 || menuIndex == 2) loadSlot(menuIndex);
            else if (menuIndex == 3) { settingsReturn = Screen.MENU; screen = Screen.SETTINGS; }
            else if (menuIndex == 4) screen = Screen.EXTRAS;
            else System.exit(0);
        }
    }

    private void loadSlot(int slot) {
        GameSession loaded = saves.load(slot);
        if (loaded == null) { showToast("O slot " + slot + " ainda está vazio."); return; }
        session = loaded; screen = Screen.WORLD; updateCamera(1f); beginTransition(null); showToast("Slot " + slot + " carregado.");
    }

    private void handleSlotKey(int key) {
        if (isUp(key) || isDown(key)) slotIndex = 1 - slotIndex;
        else if (isConfirm(key)) { typedName = ""; screen = Screen.NAME_INPUT; }
        else if (key == KeyEvent.VK_ESCAPE) screen = Screen.MENU;
    }

    private void handleNameKey(int key) {
        if (key == KeyEvent.VK_BACK_SPACE && !typedName.isEmpty()) typedName = typedName.substring(0, typedName.length() - 1);
        else if (isConfirm(key)) startNewGame();
        else if (key == KeyEvent.VK_ESCAPE) screen = Screen.SLOT_SELECT;
    }

    private void handleWorldKey(int key) {
        if (key == KeyEvent.VK_E) interact();
        else if (key == KeyEvent.VK_M && session.scene() == Scene.OUTDOOR) screen = Screen.MAP;
        else if (key == KeyEvent.VK_F5) persist(true);
        else if (key == KeyEvent.VK_ESCAPE) { heldKeys.clear(); pauseIndex = 0; screen = Screen.PAUSE; }
    }

    private void handlePauseKey(int key) {
        if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; }
        if (isUp(key)) pauseIndex = (pauseIndex + 4) % 5;
        else if (isDown(key)) pauseIndex = (pauseIndex + 1) % 5;
        else if (isConfirm(key)) {
            if (pauseIndex == 0) screen = Screen.WORLD;
            else if (pauseIndex == 1) { persist(true); screen = Screen.WORLD; }
            else if (pauseIndex == 2) { settingsReturn = Screen.PAUSE; screen = Screen.SETTINGS; }
            else if (pauseIndex == 3) { persist(true); screen = Screen.MENU; }
            else { persist(false); System.exit(0); }
        }
    }

    private void handleSettingsKey(int key) {
        if (isUp(key) || isDown(key)) settingsIndex = 1 - settingsIndex;
        else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) changeSetting(-1);
        else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D || isConfirm(key)) changeSetting(1);
        else if (key == KeyEvent.VK_ESCAPE) screen = settingsReturn;
    }

    private void changeSetting(int direction) {
        if (settingsIndex == 0) { if (direction < 0) accessibility.decreaseText(); else accessibility.increaseText(); }
        else accessibility.toggleContrast();
    }

    private void handleChoiceKey(int key) {
        if (isUp(key)) choiceIndex = (choiceIndex + choices.length - 1) % choices.length;
        else if (isDown(key)) choiceIndex = (choiceIndex + 1) % choices.length;
        else if (isConfirm(key)) resolveChoice();
        else if (key == KeyEvent.VK_ESCAPE) screen = Screen.WORLD;
    }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (screen == Screen.WORLD && (isMovement(key))) heldKeys.add(key);
        if (screen == Screen.MENU) handleMenuKey(key);
        else if (screen == Screen.SLOT_SELECT) handleSlotKey(key);
        else if (screen == Screen.NAME_INPUT) handleNameKey(key);
        else if (screen == Screen.WORLD) handleWorldKey(key);
        else if (screen == Screen.DIALOGUE && isConfirm(key)) advanceDialogue();
        else if (screen == Screen.CHOICE) handleChoiceKey(key);
        else if (screen == Screen.MAP && (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M)) screen = Screen.WORLD;
        else if (screen == Screen.PAUSE) handlePauseKey(key);
        else if (screen == Screen.SETTINGS) handleSettingsKey(key);
        else if (screen == Screen.EXTRAS && (key == KeyEvent.VK_ESCAPE || isConfirm(key))) screen = Screen.MENU;
        else if (screen == Screen.END && (key == KeyEvent.VK_ESCAPE || isConfirm(key))) screen = Screen.MENU;
    }

    @Override public void keyReleased(KeyEvent event) { heldKeys.remove(event.getKeyCode()); }

    @Override public void keyTyped(KeyEvent event) {
        if (screen != Screen.NAME_INPUT || typedName.length() >= 18) return;
        char value = event.getKeyChar();
        if (Character.isLetterOrDigit(value) || value == ' ' || value == '-' || value == '_') typedName += value;
    }

    private boolean isMovement(int key) { return isUp(key) || isDown(key) || key == KeyEvent.VK_A || key == KeyEvent.VK_D || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT; }
    private boolean isUp(int key) { return key == KeyEvent.VK_UP || key == KeyEvent.VK_W; }
    private boolean isDown(int key) { return key == KeyEvent.VK_DOWN || key == KeyEvent.VK_S; }
    private boolean isConfirm(int key) { return key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE; }
    private float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private float distance(float x1, float y1, float x2, float y2) { float x = x1 - x2, y = y1 - y2; return (float) Math.sqrt(x * x + y * y); }
    private Font font(int size) { int extra = accessibility.textScale() == 2 ? 3 : accessibility.textScale() == 0 ? -2 : 0; return new Font("Dialog", Font.BOLD, Math.max(11, size + extra)); }
    private Font normal(int size) { int extra = accessibility.textScale() == 2 ? 2 : accessibility.textScale() == 0 ? -1 : 0; return new Font("Dialog", Font.PLAIN, Math.max(11, size + extra)); }
    private Color c(int rgb) { return new Color(rgb); }
    private Color ink() { return accessibility.highContrast() ? Color.WHITE : c(0x183343); }
    private Color paper() { return accessibility.highContrast() ? c(0x111111) : c(0xFFF7D5); }
    private Color outline() { return accessibility.highContrast() ? c(0xFFE600) : c(0x173447); }
    private Color selected() { return accessibility.highContrast() ? c(0x0A58CA) : c(0xD85C3A); }

    private int drawWrapped(Graphics2D g, String text, int x, int y, int width, int lineHeight, Color color) {
        g.setColor(color); FontMetrics metrics = g.getFontMetrics(); String line = ""; int currentY = y;
        for (String word : text.split(" ")) { String test = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(test) > width && !line.isEmpty()) { g.drawString(line, x, currentY); line = word; currentY += lineHeight; } else line = test; }
        if (!line.isEmpty()) g.drawString(line, x, currentY); return currentY;
    }

    private void centered(Graphics2D g, String text, int x, int y) { g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y); }
    private void centeredAt(Graphics2D g, String text, int x, int y) { centered(g, text, x, y); }

    private static final class WorldPoint {
        final String id, label; final float x, y;
        WorldPoint(String id, String label, float x, float y) { this.id = id; this.label = label; this.x = x; this.y = y; }
    }

    private enum ObstacleKind { TREE, HOUSE, WATER, LOG }
    private static final class Obstacle {
        final Rectangle2D.Float bounds; final ObstacleKind kind;
        Obstacle(float x, float y, float w, float h, ObstacleKind kind) { bounds = new Rectangle2D.Float(x, y, w, h); this.kind = kind; }
    }
}
