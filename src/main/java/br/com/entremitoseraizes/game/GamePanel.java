package br.com.entremitoseraizes.game;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.event.MouseMotionListener; 

import br.com.entremitoseraizes.accessibility.AccessibilitySettings;
import br.com.entremitoseraizes.dialogue.DialogueLine;

/** Jogo construído sobre os três cenários fornecidos: trilha, cabana e interior. */
final class GamePanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener {
    private static final long serialVersionUID = 3L;
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final float SPEED = 220f;
    private static final float EDGE = 28f;
    private static final int HELP_BUTTON_SIZE = 100;
    private static final int HELP_BUTTON_MARGIN = 24;
    private static final int MAX_VILLAIN_HEALTH = 5;
    private static final int MAX_PLAYER_HEALTH = 100;
    private static final int VILLAIN_DAMAGE = 1;
    private static final int PLAYER_DAMAGE = 20;
    private static final String[] BATTLE_QUESTIONS = {
        "Qual atitude ajuda a preservar a floresta?",
        "O que devemos fazer ao encontrar um animal silvestre ferido?",
        "Por que a caça ilegal prejudica a natureza?",
        "Qual e uma forma correta de proteger os animais?",
        "Qual e uma consequencia da caça ilegal?"
    };
    private static final String[][] BATTLE_OPTIONS = {
        { "A) Jogar lixo no rio", "B) Plantar e cuidar das arvores", "C) Queimar a mata", "D) Retirar os ninhos" },
        { "A) Chama-lo para casa", "B) Assusta-lo", "C) Procurar ajuda especializada", "D) Vende-lo" },
        { "A) Reduz as especies e desequilibra o ambiente", "B) Limpa a floresta", "C) Ajuda todos os animais", "D) Nao causa nenhum problema" },
        { "A) Respeitar seu habitat", "B) Prende-lo por diversao", "C) Comprar animais capturados", "D) Destruir seu abrigo" },
        { "A) Aumenta a biodiversidade", "B) Extingue especies e causa sofrimento", "C) Melhora os rios", "D) Protege a floresta" }
    };
    private static final int[] BATTLE_CORRECT = { 1, 2, 0, 0, 1 };
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
    private BufferedImage unlockedForestPath;
    private BufferedImage forestPt4;
    private BufferedImage forestPt5;
    private BufferedImage forestPt6;
    private BufferedImage title;
    private BufferedImage novoJogo;
    private BufferedImage continuar;
    private BufferedImage configuracoes;
    private BufferedImage sair;
    private BufferedImage helpButton;
    private BufferedImage curupira;
    private BufferedImage villainSmall;
    private BufferedImage villainAdult;
    private BufferedImage gregIdleLeft;
    private BufferedImage gregIdleRight;
    private BufferedImage gregWalkLeftPart1;
    private BufferedImage gregWalkLeftPart2;
    private BufferedImage gregWalkRightPart1;
    private BufferedImage gregWalkRightPart2;
    private BufferedImage menuFallback;
    private ImageIcon menuAnimation;
    private File menuVideo;
    private Object videoGrab;
    private SeekableByteChannel videoChannel;
    private BufferedImage menuVideoFrame;
    private long nextVideoFrameAt;
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
    private Runnable dialogueExit;
    private Scene dialogueReturnScene;
    private long dialogueAutoAdvanceAt;
    private boolean dialogueShowsCharacters;
    private long previousTick = System.nanoTime();
    private int animationFrame;
    private int walkingFrame;
    private float menuButtonScale = 1.0f;
    private float helpButtonScale = 1.0f;
    private boolean helpButtonHovered;
    private boolean loadingSave;
    private boolean gregIsWalking;
    private int gregDirection = 1;
    private int transitionFrame = -1;
    private Runnable transitionAction;
    private String toast = "";
    private int toastFrames;
    private int continueIndex;
    private int endIndex;
    private int battleQuestion;
    private int villainHealth;
    private int playerHealth;
    private String battleMessage = "";
    private int battleFeedbackFrames;
    private boolean finishBattleAfterFeedback;

    GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        loadAssets();
        clock = new Timer(16, event -> tick());
        clock.start();
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if (screen != Screen.MENU) {
            helpButtonHovered = false;
            return;
        }

        double x = (event.getX() - offsetX) / scale;
        double y = (event.getY() - offsetY) / scale;

        helpButtonHovered = insideHelpButton(x, y);
        for (int index = 0; index < 4; index++) {
            if (insideMenuButton(x, y, index)) {
                menuIndex = index;
                break;
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent event) {}

    @Override public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    void saveIfRunning() {
        if (screen != Screen.MENU && screen != Screen.SLOT_SELECT && screen != Screen.NAME_INPUT) saves.save(session);
    }


    private void loadAssets() {
        forestPath = loadImage("Cenarios/CURUPIRA", "CRPRBLOQUE-pt1.png");
        cabinApproach = loadImage("Cenarios/CURUPIRA", "CRPR-pt2.jpeg");
        cabinInterior = loadImage("Cenarios/CURUPIRA", "CRPR-pt3.jpeg");
        unlockedForestPath = loadImage("Cenarios/CURUPIRA", "CRPRDESBLO-pt1.jpeg");
        forestPt4 = loadImage("Cenarios/CURUPIRA", "CRPR-pt4.jpeg");
        forestPt5 = loadImage("Cenarios/CURUPIRA", "CRPR-pt5.jpeg");
        forestPt6 = loadImage("Cenarios/CURUPIRA", "CRPR-pt6.jpeg");
        menuFallback = loadImage("Cenarios/Menu", "MENU_loop.gif");
        title = loadImage("Cenarios/Menu", "Titulo.png");
        novoJogo = loadImage("Cenarios/Menu", "NovoGame.png");
        continuar = loadImage("Cenarios/Menu", "Continuar.png");
        configuracoes = loadImage("Cenarios/Menu", "Configuracoes.png");
        sair = loadImage("Cenarios/Menu", "Sair.png");
        helpButton = loadImage("Cenarios/Menu", "Interrogacao.png");
        curupira = loadImage("Personagens/guardioes", "CURUPIRA.PNG");
        villainSmall = loadImage("Personagens/Viloes", "Vilao1Pequeno.png");
        villainAdult = loadImage("Personagens/Viloes", "Vilao1Adulto.png");
        gregIdleLeft = loadImage("Personagens/Greg", "GregParadoE.png");
        gregIdleRight = loadImage("Personagens/Greg", "GregParadoD.png");
        gregWalkLeftPart1 = loadImage("Personagens/Greg", "GregAndandoEPT1.png");
        gregWalkLeftPart2 = loadImage("Personagens/Greg", "GregAndandoEPT2.png");
        gregWalkRightPart1 = loadImage("Personagens/Greg", "GregAndandoDPT1.png");
        gregWalkRightPart2 = loadImage("Personagens/Greg", "GregAndandoDPT2.png");
        menuVideo = asset("Cenarios/Menu", "MENU_loop.mp4");
        if (!openMenuVideo()) {
            File gif = asset("Cenarios/Menu", "MENU_loop.gif");
            if (gif.isFile() && !Boolean.getBoolean("entremitos.test.noGif")) menuAnimation = new ImageIcon(gif.getAbsolutePath());
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

    private boolean openMenuVideo() {
        if (menuVideo == null || !menuVideo.isFile()) return false;
        try {
            Class<?> nioUtils = Class.forName("org.jcodec.common.io.NIOUtils");
            videoChannel = (SeekableByteChannel) nioUtils.getMethod("readableChannel", File.class).invoke(null, menuVideo);
            Class<?> frameGrabClass = Class.forName("org.jcodec.api.FrameGrab");
            videoGrab = frameGrabClass.getMethod("createFrameGrab", SeekableByteChannel.class).invoke(null, videoChannel);
            nextVideoFrameAt = 0L;
            return true;
        } catch (Exception ignored) {
            closeMenuVideo();
            return false;
        }
    }

    private void closeMenuVideo() {
        if (videoChannel != null) {
            try { videoChannel.close(); } catch (IOException ignored) { }
        }
        videoChannel = null;
        videoGrab = null;
        menuVideoFrame = null;
    }

    private void updateMenuVideo(long now) {
        if (videoGrab == null || now < nextVideoFrameAt) return;
        try {
            Object picture = videoGrab.getClass().getMethod("getNativeFrame").invoke(videoGrab);
            if (picture == null) {
                closeMenuVideo();
                openMenuVideo();
                return;
            }
            Class<?> awtUtil = Class.forName("org.jcodec.scale.AWTUtil");
            for (Method method : awtUtil.getMethods()) {
                if ("toBufferedImage".equals(method.getName())
                    && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].isAssignableFrom(picture.getClass())) {
                    menuVideoFrame = (BufferedImage) method.invoke(null, picture);
                    break;
                }
            }
            nextVideoFrameAt = now + 33_000_000L;
        } catch (Exception ignored) {
            closeMenuVideo();
        }
    }

    private File asset(String folder, String name) { return new File(new File(new File("Image"), folder), name); }

    private void tick() {
        long now = System.nanoTime();
        float delta = Math.min(0.05f, (now - previousTick) / 1_000_000_000f);
        previousTick = now;
        animationFrame++;
        if (screen == Screen.MENU) {
            menuButtonScale += (1.08f - menuButtonScale) * 0.15f;
        } else {
            menuButtonScale += (1.0f - menuButtonScale) * 0.15f;
        }

        if (screen == Screen.MENU && helpButtonHovered) {
            helpButtonScale += (1.10f - helpButtonScale) * 0.15f;
        } else {
            helpButtonScale += (1.0f - helpButtonScale) * 0.15f;
        }

        if (screen == Screen.MENU) updateMenuVideo(now);

        if (toastFrames > 0) toastFrames--;
        if (screen == Screen.DIALOGUE && dialogueAutoAdvanceAt > 0 && System.currentTimeMillis() >= dialogueAutoAdvanceAt) {
            dialogueAutoAdvanceAt = 0L;
            advanceDialogue();
        }
        if (transitionFrame >= 0) {
            transitionFrame++;
            if (transitionFrame == 18 && transitionAction != null) transitionAction.run();
            if (transitionFrame >= 36) { transitionFrame = -1; transitionAction = null; }
        } else if (screen == Screen.BATTLE && battleFeedbackFrames > 0) {
            battleFeedbackFrames--;
            if (battleFeedbackFrames == 0) advanceBattleRound();
        } else if (screen == Screen.WORLD && session.scene() != Scene.CABIN_INTERIOR) movePlayer(delta);
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
        if (dx < 0f) gregDirection = -1;
        else if (dx > 0f) gregDirection = 1;
        gregIsWalking = true;
        walkingFrame++;
        float diagonal = dx != 0f && dy != 0f ? 0.7071f : 1f;
        float x = session.playerX() + dx * SPEED * diagonal * delta;
        float y = clamp(session.playerY() + dy * SPEED * diagonal * delta, 130f, HEIGHT - 52f);
        Scene scene = session.scene();
        if (scene == Scene.FOREST_PATH && session.hasTalkedToCurupira() && x >= WIDTH - EDGE) {
            changeScene(Scene.CABIN_APPROACH, 78f, 525f);
        } else if (scene == Scene.CABIN_APPROACH && x >= WIDTH - EDGE) {
            session.setPosition(950f, 440f);
        } else if (scene == Scene.CABIN_APPROACH && x <= EDGE) {
            if (session.hasVisitedCabin() && session.stage() == Stage.RETURN_TO_PT2) {
                session.setStage(Stage.ACCESS_UNLOCKED_FOREST);
                changeScene(Scene.UNLOCKED_FOREST_PATH, WIDTH - 82f, 620f);
            } else {
                changeScene(Scene.UNLOCKED_FOREST_PATH, WIDTH - 82f, 620f);
            }
        } else if (scene == Scene.UNLOCKED_FOREST_PATH && x >= WIDTH - EDGE) {
            changeScene(Scene.CABIN_APPROACH, 78f, 525f);
        } else if (scene == Scene.UNLOCKED_FOREST_PATH && x <= EDGE
                && session.hasTalkedToCurupira() && session.hasVisitedCabin()) {
            session.setStage(Stage.REACH_PT4);
            changeScene(Scene.FOREST_PT4, WIDTH - 82f, 620f);
        } else if (scene == Scene.FOREST_PT4 && x <= EDGE
            && session.stage().ordinal() >= Stage.REACH_PT4.ordinal()) {
            session.setStage(Stage.REACH_PT5);
            changeScene(Scene.FOREST_PT5, WIDTH - 82f, 620f);
        } else if (scene == Scene.FOREST_PT4 && x >= WIDTH - EDGE) {
            changeScene(Scene.UNLOCKED_FOREST_PATH, 78f, 620f);
        } else if (scene == Scene.FOREST_PT5 && x <= EDGE
            && session.stage().ordinal() >= Stage.REACH_PT5.ordinal()) {
            session.setStage(Stage.REACH_PT6);
            changeScene(Scene.FOREST_PT6, WIDTH - 82f, 620f);
        } else if (scene == Scene.FOREST_PT5 && x >= WIDTH - EDGE) {
            changeScene(Scene.FOREST_PT4, WIDTH - 82f, 620f);
        } else if (scene == Scene.FOREST_PT6 && x >= WIDTH - 120f && session.stage() == Stage.REACH_PT6) {
            session.setPosition(WIDTH - 140f, y);
            startVillainEncounter();
        } else if (scene == Scene.FOREST_PT6 && x <= EDGE) {
            changeScene(Scene.FOREST_PT5, WIDTH - 82f, 620f);
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
                if (next == Scene.CABIN_INTERIOR) {
                    openCurupiraDialogue(Scene.CABIN_APPROACH);
                }
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

        } else if (screen == Screen.CONTROLS) {

            drawControls(g);

        } else if (screen == Screen.EXTRAS) {

            drawExtras(g);

        } else if (screen == Screen.END) {

            drawEnd(g);

        } else if (screen == Screen.CONTINUE_CHOICE) {

            drawContinueChoice(g);

        } else if (screen == Screen.BATTLE) {

            drawBattle(g);

        } else if (screen == Screen.BATTLE_RESULT) {

            drawBattleResult(g);

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
        if (menuVideoFrame != null) {
            g.drawImage(menuVideoFrame, 0, 0, WIDTH, HEIGHT, this);
        } else if (menuAnimation != null) {
            g.drawImage(
                menuAnimation.getImage(),
                0,
                0,
                WIDTH,
                HEIGHT,
                this
            );
        } else if (menuFallback != null) {
            g.drawImage(
                menuFallback,
                0,
                0,
                WIDTH,
                HEIGHT,
                this
            );
        } else {
            g.setColor(c(0x244B3B));
            g.fillRect(0, 0, WIDTH, HEIGHT);
        }
    }


    private void drawMenu(Graphics2D g) {
        drawMenuBackground(g);

        g.setColor(new Color(3, 15, 16, 135));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Título permanece centralizado.
        if (title != null) {
            g.drawImage(title, WIDTH / 2 - 229, 24, 458, 228, this);
        }

        // Menu principal deslocado para a esquerda.
        String[] options = {
            "Novo jogo",
            "Continuar",
            "Configurações",
            "Sair do jogo"
        };

        for (int i = 0; i < options.length; i++) {
            BufferedImage buttonImage = null;

            if (i == 0) {
                buttonImage = novoJogo;
            } else if (i == 1) {
                buttonImage = continuar;
            } else if (i == 2) {
                buttonImage = configuracoes;
            } else if (i == 3) {
                buttonImage = sair;
            }

            drawMenuButtonImage(
                g,
                buttonImage,
                86,
                264 + i * 58,
                i == menuIndex
            );
        }

        // Botão de informações.
        drawHelpButton(g);

    }

    private void drawMenuButtonImage(
        Graphics2D g,
        BufferedImage image,
        int x,
        int y,
        boolean selected) {

        if (image == null) {
            return;
        }

        float scale = selected ? menuButtonScale : 1.0f;

        int baseWidth = 260;
        int baseHeight = 130;

        int width = Math.round(baseWidth * scale);
        int height = Math.round(baseHeight * scale);

        int drawX = x + (baseWidth - width) / 2;
        int drawY = y + (baseHeight - height) / 2;

        g.drawImage(
            image,
            drawX,
            drawY,
            width,
            height,
            this
        );
    }

    private int helpButtonX() {
        return WIDTH - HELP_BUTTON_MARGIN - HELP_BUTTON_SIZE;
    }

    private int helpButtonY() {
        return HEIGHT - HELP_BUTTON_MARGIN - HELP_BUTTON_SIZE;
    }

    private boolean insideHelpButton(double x, double y) {
    return x >= helpButtonX()
        && x <= helpButtonX() + HELP_BUTTON_SIZE
        && y >= helpButtonY()
        && y <= helpButtonY() + HELP_BUTTON_SIZE;
    
    }

    private boolean insideMenuButton(double x, double y, int index) {
        int buttonX = 86;
        int buttonY = 264 + index * 58;
        return x >= buttonX && x <= buttonX + 260 && y >= buttonY && y <= buttonY + 130;
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

    private void drawHelpButton(Graphics2D g) {

        if (helpButton == null) {
            return;
        }

        int baseSize = HELP_BUTTON_SIZE;

        float scale = helpButtonHovered
                ? helpButtonScale
                : 1.0f;

        int originalWidth = helpButton.getWidth();
        int originalHeight = helpButton.getHeight();

        float aspectRatio = (float) originalWidth / originalHeight;

        int width;
        int height;

        if (aspectRatio >= 1.0f) {
            width = Math.round(baseSize * scale);
            height = Math.round(width / aspectRatio);
        } else {
            height = Math.round(baseSize * scale);
            width = Math.round(height * aspectRatio);
        }

        int areaX = WIDTH - HELP_BUTTON_MARGIN - baseSize;
        int areaY = HEIGHT - HELP_BUTTON_MARGIN - baseSize;

        int x = areaX + (baseSize - width) / 2;
        int y = areaY + (baseSize - height) / 2;

        g.drawImage(
            helpButton,
            x,
            y,
            width,
            height,
            this
        );
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
        if (session.scene() == Scene.FOREST_PATH) drawImageScene(g, forestPath, "CRPRBLOQUE-pt1", "Encontre o Curupira.");
        else if (session.scene() == Scene.CABIN_APPROACH) drawCabinApproach(g);
        else if (session.scene() == Scene.CABIN_INTERIOR) drawCabinInterior(g);
        else if (session.scene() == Scene.UNLOCKED_FOREST_PATH) drawImageScene(g, unlockedForestPath, "CRPRDESBLO-pt1", "Siga pela clareira.");
        else if (session.scene() == Scene.FOREST_PT4) drawImageScene(g, forestPt4, "CRPR-pt4", "Continue pela floresta.");
        else if (session.scene() == Scene.FOREST_PT5) drawImageScene(g, forestPt5, "CRPR-pt5", "A ameaça está próxima.");
        else drawImageScene(g, forestPt6, "CRPR-pt6", "Encontre a origem da caça ilegal.");
        if (session.scene() == Scene.FOREST_PATH && !session.hasTalkedToCurupira()) {
            drawCurupira(g, 1000, 430, 145, 178);
        } else if (session.scene() == Scene.FOREST_PT6 && session.stage() == Stage.BATTLE_VILLAIN) {
            drawVillain(g, 1010, 390, 150, 220);
        }
        if (toastFrames > 0) drawToast(g);
    }

    private void drawImageScene(Graphics2D g, BufferedImage image, String label, String help) {
        if (image != null) g.drawImage(image, 0, 0, WIDTH, HEIGHT, this);
        else { g.setColor(c(0x274D3A)); g.fillRect(0, 0, WIDTH, HEIGHT); }
        if (session.scene() != Scene.CABIN_INTERIOR) drawGreg(g);
    }

    private void drawCabinApproach(Graphics2D g) {
        drawImageScene(g, cabinApproach, "Cenário 2/3  •  Entrada da cabana", "Vá à esquerda para voltar à trilha. Aproxime-se da porta e pressione E.");
        int doorX = 950, doorY = 440;
        if (distance(session.playerX(), session.playerY(), doorX, doorY) < 118) {
            bubble(g, doorX, doorY - 125, "E");
            g.setColor(c(0xFFF3C1)); g.setFont(normal(14)); centered(g, "Entrar na cabana", doorX, doorY - 142);
        }
    }

    private void drawCabinInterior(Graphics2D g) {
        drawImageScene(g, cabinInterior, "Cenário 3/3  •  Dentro da cabana", "");
        int curupiraX = 500, curupiraY = 430;
        drawCurupira(g, curupiraX, curupiraY, 130, 158);
    }

    private void drawGreg(Graphics2D g) {
        BufferedImage sprite = gregDirection < 0 ? gregIdleLeft : gregIdleRight;
        if (gregIsWalking) {
            boolean secondStep = (walkingFrame / 8) % 2 == 1;
            if (gregDirection < 0) {
                sprite = secondStep && gregWalkLeftPart2 != null ? gregWalkLeftPart2 : gregWalkLeftPart1;
            } else {
                sprite = secondStep && gregWalkRightPart2 != null ? gregWalkRightPart2 : gregWalkRightPart1;
            }
        }
        if (sprite == null) sprite = gregDirection < 0 ? gregIdleRight : gregIdleLeft;
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

    private void drawVillain(Graphics2D g, int x, int y, int w, int h) {
        if (villainSmall != null) g.drawImage(villainSmall, x, y, w, h, this);
    }

    private void drawDialogue(Graphics2D g) {
        shade(g, 110);
        if (dialogue.isEmpty()) return;
        DialogueLine line = dialogue.get(dialogueIndex);
        int boxX = WIDTH / 2 - 530;
        if (dialogueShowsCharacters) drawDialogueCharacters(g, line.speaker);
        boxX = WIDTH / 2 - 460;
        g.setColor(new Color(28, 57, 42, 242)); g.fillRoundRect(boxX, 438, 920, 190, 18, 18);
        g.setColor(c(0xE7C98A)); g.setStroke(new BasicStroke(4)); g.drawRoundRect(boxX, 438, 920, 190, 18, 18);
        g.setColor(selected()); g.fillRoundRect(WIDTH / 2 - 128, 420, 256, 40, 10, 10);
        g.setColor(Color.WHITE); g.setFont(font(19)); centered(g, line.speaker, WIDTH / 2, 447);
        g.setColor(c(0xFFF3C1)); g.setFont(normal(20)); drawWrapped(g, line.text, WIDTH / 2 - 390, 505, 780, 28, c(0xFFF3C1));
        g.setFont(normal(13)); centered(g, "Enter / Espaço para continuar   •   Esc para sair", WIDTH / 2, 602);
    }

    private void drawDialogueCharacters(Graphics2D g, String speaker) {
        if (speaker.toLowerCase().contains("narradora") && villainSmall != null) {
            drawDialogueCharacter(g, villainSmall, 910, 205, 230, 300, true);
            return;
        }
        if (speaker.toLowerCase().contains("caçador") || speaker.toLowerCase().contains("vilão")) {
            drawDialogueCharacter(g, villainAdult, 910, 130, 285, 470, true);
            return;
        }
        boolean curupiraSpeaking = "Curupira".equalsIgnoreCase(speaker);
        BufferedImage greg = gregDirection < 0 ? gregIdleLeft : gregIdleRight;
        drawDialogueCharacter(g, greg, 90, 205, 250, 430, !curupiraSpeaking);
        drawDialogueCharacter(g, curupira, 940, 180, 250, 455, curupiraSpeaking);
    }

    private void drawDialogueCharacter(Graphics2D g, BufferedImage image, int x, int y, int width, int height, boolean focused) {
        if (image == null) return;
        java.awt.Composite previous = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, focused ? 1.0f : 0.55f));
        g.drawImage(image, x, y, width, height, this);
        g.setComposite(previous);
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
        if (scene == Scene.FOREST_PATH) return "CRPRBLOQUE-pt1 - Início";
        if (scene == Scene.CABIN_APPROACH) return "CRPR-pt2 - Entrada da cabana";
        if (scene == Scene.CABIN_INTERIOR) return "CRPR-pt3 - Dentro da cabana";
        if (scene == Scene.UNLOCKED_FOREST_PATH) return "CRPRDESBLO-pt1 - Clareira";
        if (scene == Scene.FOREST_PT4) return "CRPR-pt4";
        if (scene == Scene.FOREST_PT5) return "CRPR-pt5";
        return "CRPR-pt6 - Confronto";
    }

    private void drawSettings(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 306, 150, 612, 410); g.setColor(ink()); g.setFont(font(28)); centered(g, "CONFIGURAÇÕES", WIDTH / 2, 210);
        String[] labels = { "Tamanho do texto", "Alto contraste", "Controles" }; String[] values = { accessibility.textSizeName(), accessibility.highContrast() ? "Ativado" : "Desativado", "Abrir tutorial" };
        for (int i = 0; i < 3; i++) { int y = 220 + i * 94; boolean active = i == settingsIndex; g.setColor(active ? selected() : c(0xDDE9BE)); g.fillRoundRect(WIDTH / 2 - 256, y, 512, 71, 8, 8); g.setColor(active && !accessibility.highContrast() ? Color.WHITE : ink()); g.setFont(font(19)); centered(g, labels[i], WIDTH / 2, y + 30); g.setFont(normal(15)); centered(g, "◀   " + values[i] + "   ▶", WIDTH / 2, y + 55); }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "↑ ↓ escolhe • ← → ou Enter altera • Esc volta", WIDTH / 2, 512);
    }

    private void drawControls(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 350, 90, 700, 540);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "CONTROLES", WIDTH / 2, 145);
        g.setFont(normal(18));
        centered(g, "WASD / Setas  —  Mover o personagem", WIDTH / 2, 220);
        centered(g, "E  —  Interagir", WIDTH / 2, 265);
        centered(g, "M  —  Abrir o mapa", WIDTH / 2, 310);
        centered(g, "F5  —  Salvar o progresso", WIDTH / 2, 355);
        centered(g, "Esc  —  Voltar / abrir o menu", WIDTH / 2, 400);
        centered(g, "Enter / Espaço  —  Confirmar / avançar", WIDTH / 2, 445);
        g.setFont(normal(14)); centered(g, "Esc para voltar", WIDTH / 2, 580);
    }

    private void drawExtras(Graphics2D g) {
        drawMenuBackground(g);
        shade(g, 140);

        card(
            g,
            WIDTH / 2 - 350,
            90,
            700,
            540
        );

        g.setColor(ink());
        g.setFont(font(29));
        centered(
            g,
            "COMO JOGAR",
            WIDTH / 2,
            145
        );

        g.setFont(normal(17));

        centered(
            g,
            "CONTROLES",
            WIDTH / 2,
            195
        );

        int startY = 235;
        int spacing = 38;

        centered(
            g,
            "WASD / Setas  —  Mover o personagem",
            WIDTH / 2,
            startY
        );

        centered(
            g,
            "E  —  Interagir",
            WIDTH / 2,
            startY + spacing
        );

        centered(
            g,
            "M  —  Abrir o mapa",
            WIDTH / 2,
            startY + spacing * 2
        );

        centered(
            g,
            "F5  —  Salvar o progresso",
            WIDTH / 2,
            startY + spacing * 3
        );

        centered(
            g,
            "Esc  —  Abrir o menu / voltar",
            WIDTH / 2,
            startY + spacing * 4
        );

        centered(
            g,
            "Enter / Espaço  —  Confirmar / avançar",
            WIDTH / 2,
            startY + spacing * 5
        );

        g.setFont(font(21));

        centered(
            g,
            "CRÉDITOS",
            WIDTH / 2,
            475
        );

        g.setFont(normal(16));

        centered(
            g,
            "Entre Mitos e Raízes",
            WIDTH / 2,
            510
        );

        centered(
            g,
            "Antonio Andson  •  Sophia Hellen",
            WIDTH / 2,
            538
        );

        g.setFont(normal(14));

        centered(
            g,
            "Fase de teste: O Chamado do Curupira",
            WIDTH / 2,
            575
        );

        centered(
            g,
            "Esc ou Enter para voltar",
            WIDTH / 2,
            605
        );
    }

    private void drawEnd(Graphics2D g) {
        drawMenuBackground(g);
        shade(g, 145);
        card(g, WIDTH / 2 - 334, 137, 668, 446);

        // Título
        g.setColor(ink());
        g.setFont(font(31));
        centered(
            g,
            "FASE DE TESTE CONCLUÍDA",
            WIDTH / 2,
            204
        );

        // Texto da conclusão
        g.setFont(normal(19));
        drawWrapped(
            g,
            "Você impediu a caça ilegal e ajudou a preservar a floresta. "
                + "Os animais estão protegidos e a Fase 1 foi concluída.",
            WIDTH / 2 - 172,
            293,
            345,
            29,
            ink()
        );

        // Curupira abaixo do texto
        if (curupira != null) {
            int curupiraWidth = 100;
            int curupiraHeight = 123;

            int curupiraX = WIDTH / 2 - curupiraWidth / 2;
            int curupiraY = 405;

            g.drawImage(
                curupira,
                curupiraX,
                curupiraY,
                curupiraWidth,
                curupiraHeight,
                this
            );
        }

        drawOption(g, "CONTINUAR", 530, endIndex == 0, WIDTH / 2 - 180, 170);
        drawOption(g, "MENU", 530, endIndex == 1, WIDTH / 2 + 10, 170);
        g.setFont(normal(14));
        centered(g, "Setas e Enter", WIDTH / 2, 570);
    }

    private void drawContinueChoice(Graphics2D g) {
        drawMenuBackground(g); shade(g, 140); card(g, WIDTH / 2 - 330, 145, 660, 430);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "COMO DESEJA CONTINUAR?", WIDTH / 2, 215);
        g.setFont(normal(16)); centered(g, "Escolha o caminho da sua aventura.", WIDTH / 2, 250);
        drawOption(g, "PRÓXIMA FASE", 300, continueIndex == 0, WIDTH / 2 - 230, 460);
        drawOption(g, "FASE 1", 390, continueIndex == 1, WIDTH / 2 - 230, 460);
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "A próxima fase será liberada quando estiver disponível.", WIDTH / 2, 490);
        centered(g, "Esc volta ao menu", WIDTH / 2, 535);
    }

    private void drawBattle(Graphics2D g) {
        drawScene(g); shade(g, 125); card(g, 74, 58, WIDTH - 148, HEIGHT - 116);
        g.setColor(ink()); g.setFont(font(29)); centered(g, "CONFRONTO DE CONHECIMENTO", WIDTH / 2, 108);
        drawHealthBar(g, 135, 135, 390, "Vida do jogador", playerHealth, MAX_PLAYER_HEALTH, c(0x3B9B62));
        drawHealthBar(g, WIDTH - 525, 135, 390, "Vida do caçador", villainHealth, MAX_VILLAIN_HEALTH, c(0xB84A3A));
        g.setColor(ink()); g.setFont(font(21));
        drawWrapped(g, "Pergunta " + (battleQuestion + 1) + " de " + BATTLE_QUESTIONS.length + ": " + BATTLE_QUESTIONS[battleQuestion], 135, 220, WIDTH - 270, 30, ink());
        for (int i = 0; i < BATTLE_OPTIONS[battleQuestion].length; i++) {
            drawOption(g, BATTLE_OPTIONS[battleQuestion][i], 290 + i * 58, false, 135, WIDTH - 270);
        }
        if (!battleMessage.isEmpty()) {
            g.setColor(selected()); g.setFont(font(18)); centered(g, battleMessage, WIDTH / 2, 550);
        }
        g.setColor(ink()); g.setFont(normal(14)); centered(g, "A / B / C / D ou 1 / 2 / 3 / 4 para responder", WIDTH / 2, 590);
    }

    private void drawHealthBar(Graphics2D g, int x, int y, int width, String label, int value, int maximum, Color fill) {
        g.setColor(ink()); g.setFont(normal(15)); centered(g, label, x + width / 2, y);
        g.setColor(c(0x321F1B)); g.fillRoundRect(x, y + 12, width, 22, 8, 8);
        g.setColor(fill); g.fillRoundRect(x, y + 12, Math.max(0, width * value / maximum), 22, 8, 8);
        g.setColor(Color.WHITE); g.setFont(normal(13)); centered(g, value + " / " + maximum, x + width / 2, y + 29);
    }

    private void drawBattleResult(Graphics2D g) {
        drawMenuBackground(g); shade(g, 145); card(g, WIDTH / 2 - 345, 130, 690, 460);
        g.setColor(ink()); g.setFont(font(30)); centered(g, "A CAÇADA CONTINUA", WIDTH / 2, 205);
        g.setFont(normal(19));
        drawWrapped(g, "O caçador escapou por enquanto e continua ameaçando os animais da floresta.", WIDTH / 2 - 255, 275, 510, 30, ink());
        drawWrapped(g, "Levante-se e tente novamente! A mata ainda precisa de você.", WIDTH / 2 - 255, 350, 510, 30, ink());
        if (villainAdult != null) g.drawImage(villainAdult, WIDTH / 2 - 72, 385, 144, 190, this);
        g.setFont(normal(14)); centered(g, "Enter para voltar ao início de CRPR-pt6", WIDTH / 2, 550);
    }

    private void drawOption(Graphics2D g, String label, int y, boolean active, int x, int width) {
        g.setColor(active ? selected() : new Color(16, 46, 47, 225)); g.fillRoundRect(x, y, width, 42, 8, 8); g.setColor(c(0xFFE5A1)); g.drawRoundRect(x, y, width, 42, 8, 8);
        g.setColor(Color.WHITE); g.setFont(font(18)); centered(g, label, x + width / 2, y + 27); if (active) { g.setColor(c(0xFFE5A1)); g.fillRect(x + 15, y + 15, 10, 10); }
    }

    private void bubble(Graphics2D g, int x, int y, String text) { g.setColor(c(0xFFF0A1)); g.fillRoundRect(x - 15, y - 12, 30, 25, 7, 7); g.setColor(c(0x333A36)); g.setFont(font(17)); centered(g, text, x, y + 7); }
    private void drawToast(Graphics2D g) { g.setColor(new Color(8, 25, 23, 220)); g.fillRoundRect(WIDTH / 2 - 231, 104, 462, 38, 8, 8); g.setColor(c(0xFFF3C1)); g.setFont(normal(14)); centered(g, toast, WIDTH / 2, 129); }
    private void shade(Graphics2D g, int alpha) { g.setColor(new Color(0, 0, 0, alpha)); g.fillRect(0, 0, WIDTH, HEIGHT); }
    private void card(Graphics2D g, int x, int y, int w, int h) { g.setColor(paper()); g.fillRoundRect(x, y, w, h, 15, 15); g.setColor(outline()); g.setStroke(new BasicStroke(4)); g.drawRoundRect(x, y, w, h, 15, 15); }

    private void drawIris(Graphics2D g) {
        float visibleRadius = transitionFrame <= 18 ? 900f * (1f - transitionFrame / 18f) : 900f * ((transitionFrame - 18) / 18f);
        Area cover = new Area(new Rectangle2D.Float(0, 0, WIDTH, HEIGHT)); cover.subtract(new Area(new Ellipse2D.Float(WIDTH / 2f - visibleRadius, HEIGHT / 2f - visibleRadius, visibleRadius * 2f, visibleRadius * 2f)));
        g.setColor(Color.BLACK); g.fill(cover);
    }

    private void interact() {
        if (session.scene() == Scene.FOREST_PATH && !session.hasTalkedToCurupira()
                && distance(session.playerX(), session.playerY(), 1080, 620) < 155) {
            openInitialCurupiraDialogue();
            return;
        }
        if (session.scene() == Scene.CABIN_APPROACH && session.stage() == Stage.ACCESS_UNLOCKED_FOREST
                && distance(session.playerX(), session.playerY(), 950, 440) < 118) {
            session.setStage(Stage.TALK_TO_CURUPIRA); changeScene(Scene.CABIN_INTERIOR, 700f, 570f); return;
        }
        if (session.scene() == Scene.CABIN_INTERIOR) showToast("O diálogo da cabana já foi iniciado.");
        else showToast("Aproxime-se da porta para interagir.");
    }

    private void startVillainEncounter() {
        if (screen != Screen.WORLD || session.stage() != Stage.REACH_PT6) return;
        session.setStage(Stage.BATTLE_VILLAIN);
        battleQuestion = 0;
        villainHealth = MAX_VILLAIN_HEALTH;
        playerHealth = MAX_PLAYER_HEALTH;
        battleMessage = "";
        battleFeedbackFrames = 0;
        finishBattleAfterFeedback = false;
        saves.save(session);
        openDialogue(
            lines(
                "Narradora", "Uma sombra surge entre as árvores. O responsável pela caça ilegal finalmente aparece.",
                "Caçador", "Então foi o Curupira quem chamou você. Eu sou o responsável pelos animais que desapareceram.",
                session.playerName(), "Você está ferindo a floresta e caçando animais ilegalmente!",
                "Caçador", "A mata não pode ser protegida apenas com coragem. Vamos ver o que você aprendeu sobre ela."
            ),
            new Runnable() {
                @Override public void run() { screen = Screen.BATTLE; }
            },
            new Runnable() {
                @Override public void run() {
                    session.setStage(Stage.REACH_PT6);
                    session.setPosition(1040f, 620f);
                    saves.save(session);
                    screen = Screen.WORLD;
                }
            }
        );
    }

    private void answerBattle(int answer) {
        if (answer < 0 || answer > 3 || screen != Screen.BATTLE) return;
        if (answer == BATTLE_CORRECT[battleQuestion]) {
            villainHealth = Math.max(0, villainHealth - VILLAIN_DAMAGE);
            battleMessage = "Acerto! A proteção da natureza é o caminho.";
        } else {
            playerHealth = Math.max(0, playerHealth - PLAYER_DAMAGE);
            battleMessage = "Resposta incorreta. O caçador aproveitou a distração.";
        }
        finishBattleAfterFeedback = battleQuestion == BATTLE_QUESTIONS.length - 1
                || playerHealth == 0 || villainHealth == 0;
        battleFeedbackFrames = 30;
    }

    private void advanceBattleRound() {
        if (playerHealth == 0) finishBattleLoss();
        else if (villainHealth == 0) finishBattleWin();
        else if (finishBattleAfterFeedback) finishBattleLoss();
        else battleQuestion++;
        finishBattleAfterFeedback = false;
    }

    private void finishBattleWin() {
        session.setStage(Stage.PHASE_COMPLETE);
        session.complete(GameSession.FLAG_PHASE_ONE_COMPLETE);
        saves.save(session);
        endIndex = 0;
        screen = Screen.END;
    }

    private void finishBattleLoss() {
        session.setStage(Stage.REACH_PT6);
        session.setScene(Scene.FOREST_PT6);
        session.setPosition(100f, 620f);
        battleQuestion = 0;
        villainHealth = MAX_VILLAIN_HEALTH;
        playerHealth = MAX_PLAYER_HEALTH;
        battleMessage = "";
        saves.save(session);
        openDialogue(
            lines("Caçador", "A floresta já é minha!", session.playerName(), "Você não conseguiu impedir a caça ilegal desta vez. Levante-se e tente novamente!"),
            new Runnable() { @Override public void run() { screen = Screen.BATTLE_RESULT; } },
            new Runnable() { @Override public void run() { screen = Screen.BATTLE_RESULT; } }
        );
    }

    private void retryBattle() {
        session.setStage(Stage.REACH_PT6);
        session.setScene(Scene.FOREST_PT6);
        session.setPosition(100f, 620f);
        battleQuestion = 0;
        villainHealth = MAX_VILLAIN_HEALTH;
        playerHealth = MAX_PLAYER_HEALTH;
        battleMessage = "";
        saves.save(session);
        screen = Screen.WORLD;
        beginTransition(null);
    }

    private void startNew() {
        session.startNew(typedName, slotIndex + 1);
        session.setPosition(960f, 620f);
        saves.save(session);
        openDialogue(lines("Narradora", "Há muito tempo, histórias são contadas sobre seres que vivem nas florestas brasileiras.", "Narradora", session.playerName() + " chega à borda da mata. Um assobio distante anuncia que o Curupira está por perto."), new Runnable() {
            @Override public void run() { screen = Screen.WORLD; beginTransition(null); }
        }, null, false, 5000L);
    }

    private void openDialogue(DialogueLine[] lines, Runnable finish) {
        openDialogue(lines, finish, null);
    }

    private void openDialogue(DialogueLine[] lines, Runnable finish, Runnable exit) {
        openDialogue(lines, finish, exit, true, 0L);
    }

    private void openDialogue(DialogueLine[] lines, Runnable finish, Runnable exit, boolean showsCharacters, long autoAdvanceMillis) {
        heldKeys.clear();
        dialogue = new ArrayList<DialogueLine>(Arrays.asList(lines));
        dialogueIndex = 0;
        dialogueFinish = finish;
        dialogueExit = exit;
        dialogueReturnScene = session.scene();
        dialogueShowsCharacters = showsCharacters;
        dialogueAutoAdvanceAt = autoAdvanceMillis > 0 ? System.currentTimeMillis() + autoAdvanceMillis : 0L;
        screen = Screen.DIALOGUE;
    }

    private void openCurupiraDialogue(final Scene previousScene) {
        openDialogue(
            lines("Curupira", "A floresta está em perigo, " + session.playerName() + ". Há um caçador misterioso na floresta que está capturando animais ilegalmente.", session.playerName(), "Quem faria isso com os animais e com a mata?", "Curupira", "Ainda não sei quem ele é, mas seus rastros levam para além da clareira. Depois de cumprir seu caminho, você poderá encontrá-lo."),
            new Runnable() {
                @Override public void run() { session.markVisitedCabin(); session.setStage(Stage.RETURN_TO_PT2); session.setScene(previousScene); session.setPosition(900f, 520f); saves.save(session); screen = Screen.WORLD; }
            },
            new Runnable() {
                @Override public void run() {
                    session.setScene(previousScene);
                    session.setPosition(900f, 520f);
                    screen = Screen.WORLD;
                }
            }
        );
    }

    private void openInitialCurupiraDialogue() {
        openDialogue(
            lines("Curupira", "Há um caçador misterioso na floresta que está capturando animais ilegalmente.", session.playerName(), "Eu vou descobrir quem está fazendo isso.", "Curupira", "Siga pela clareira e entre na cabana. O caminho agora está livre."),
            new Runnable() {
                @Override public void run() {
                    session.markTalkedToCurupira();
                    session.setStage(Stage.ACCESS_UNLOCKED_FOREST);
                    session.setScene(Scene.UNLOCKED_FOREST_PATH);
                    session.setPosition(1100f, 620f);
                    saves.save(session);
                    screen = Screen.WORLD;
                }
            },
            null
        );
    }

    private void exitDialogue() {
        Runnable exit = dialogueExit;
        dialogueAutoAdvanceAt = 0L;
        dialogueExit = null;
        dialogueFinish = null;
        dialogue.clear();
        if (exit != null) exit.run();
        else {
            session.setScene(dialogueReturnScene);
            screen = Screen.WORLD;
        }
    }
    private DialogueLine[] lines(String... source) { List<DialogueLine> result = new ArrayList<DialogueLine>(); for (int i = 0; i + 1 < source.length; i += 2) result.add(new DialogueLine(source[i], source[i + 1])); return result.toArray(new DialogueLine[result.size()]); }
    private void advanceDialogue() { dialogueAutoAdvanceAt = 0L; if (dialogueIndex + 1 < dialogue.size()) { dialogueIndex++; return; } Runnable finish = dialogueFinish; dialogueFinish = null; if (finish != null) finish.run(); else screen = Screen.WORLD; }
    private void beginTransition(Runnable action) { heldKeys.clear(); transitionFrame = 0; transitionAction = action; }
    private void showToast(String message) { toast = message; toastFrames = 100; }

    private void handleMenu(int key) {
         if (up(key)) {
            menuIndex = (menuIndex + 3) % 4;

        } else if (down(key)) {
            menuIndex = (menuIndex + 1) % 4;

        } else if (confirm(key)) {

            if (menuIndex == 0) {

                // Novo jogo.
                loadingSave = false;
                slotIndex = 0;
                screen = Screen.SLOT_SELECT;

            } else if (menuIndex == 1) {

                // Continuar jogo salvo.
                loadingSave = true;
                slotIndex = 0;
                screen = Screen.SLOT_SELECT;

            } else if (menuIndex == 2) {

                settingsReturn = Screen.MENU;
                screen = Screen.SETTINGS;

            } else {

                System.exit(0);
            }
        }
    }

    private void load(int slot) {

        GameSession loaded = saves.load(slot);

        if (loaded == null) {

            showToast(
                "O slot " + slot + " está vazio."
            );

            return;
        }

        session = loaded;
        loadingSave = false;
        continueIndex = 0;
        screen = Screen.CONTINUE_CHOICE;
    }

    private void handleContinueChoice(int key) {
        if (up(key) || down(key)) {
            continueIndex = 1 - continueIndex;
        } else if (confirm(key)) {
            if (continueIndex == 0) {
                if (session.stage() == Stage.PHASE_COMPLETE || session.isComplete("phase-1-villain")) {
                    screen = Screen.WORLD;
                    showToast("A próxima fase ainda não está disponível.");
                } else {
                    screen = Screen.WORLD;
                    beginTransition(new Runnable() {
                        @Override public void run() {
                            if (session.scene() == Scene.CABIN_INTERIOR) {
                                openCurupiraDialogue(Scene.CABIN_APPROACH);
                            } else if (session.scene() == Scene.FOREST_PT6 && session.stage() == Stage.BATTLE_VILLAIN) {
                                session.setStage(Stage.REACH_PT6);
                                startVillainEncounter();
                            }
                        }
                    });
                }
            } else {
                session.resetPhaseOne();
                saves.save(session);
                screen = Screen.WORLD;
                beginTransition(null);
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            screen = Screen.MENU;
        }
    }

    private void handleBattle(int key) {
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_NUMPAD1) answerBattle(0);
        else if (key == KeyEvent.VK_B || key == KeyEvent.VK_NUMPAD2) answerBattle(1);
        else if (key == KeyEvent.VK_C || key == KeyEvent.VK_NUMPAD3) answerBattle(2);
        else if (key == KeyEvent.VK_D || key == KeyEvent.VK_NUMPAD4) answerBattle(3);
    }
    
    private void handleSlots(int key) {

        if (up(key) || down(key)) {

            slotIndex = 1 - slotIndex;

        } else if (confirm(key)) {

            int slot = slotIndex + 1;

            if (loadingSave) {

                load(slot);

            } else {

                typedName = "";
                screen = Screen.NAME_INPUT;
            }

        } else if (key == KeyEvent.VK_ESCAPE) {

            screen = Screen.MENU;
        }
    }

    private void handleName(int key) { if (key == KeyEvent.VK_BACK_SPACE && !typedName.isEmpty()) typedName = typedName.substring(0, typedName.length() - 1); else if (confirm(key)) startNew(); else if (key == KeyEvent.VK_ESCAPE) screen = Screen.SLOT_SELECT; }
    private void handlePause(int key) { if (key == KeyEvent.VK_ESCAPE) { screen = Screen.WORLD; return; } if (up(key)) pauseIndex = (pauseIndex + 4) % 5; else if (down(key)) pauseIndex = (pauseIndex + 1) % 5; else if (confirm(key)) { if (pauseIndex == 0) screen = Screen.WORLD; else if (pauseIndex == 1) { saves.save(session); showToast("Progresso salvo."); screen = Screen.WORLD; } else if (pauseIndex == 2) { settingsReturn = Screen.PAUSE; screen = Screen.SETTINGS; } else if (pauseIndex == 3) { saves.save(session); screen = Screen.MENU; } else { saves.save(session); System.exit(0); } } }
    private void handleSettings(int key) { if (up(key) || down(key)) settingsIndex = (settingsIndex + (down(key) ? 1 : 2)) % 3; else if (settingsIndex == 2 && confirm(key)) screen = Screen.CONTROLS; else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_A) changeSetting(-1); else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_D || confirm(key)) changeSetting(1); else if (key == KeyEvent.VK_ESCAPE) screen = settingsReturn; }
    private void changeSetting(int direction) { if (settingsIndex == 0) { if (direction < 0) accessibility.decreaseText(); else accessibility.increaseText(); } else if (settingsIndex == 1) accessibility.toggleContrast(); }

    @Override public void keyPressed(KeyEvent event) {
        int key = event.getKeyCode();
        if (screen == Screen.WORLD && movement(key)) heldKeys.add(key);
        if (screen == Screen.MENU) handleMenu(key); else if (screen == Screen.SLOT_SELECT) handleSlots(key); else if (screen == Screen.NAME_INPUT) handleName(key); else if (screen == Screen.WORLD) { if (key == KeyEvent.VK_E) interact(); else if (key == KeyEvent.VK_M) screen = Screen.MAP; else if (key == KeyEvent.VK_F5) { saves.save(session); showToast("Progresso salvo."); } else if (key == KeyEvent.VK_ESCAPE) { heldKeys.clear(); pauseIndex = 0; screen = Screen.PAUSE; } } else if (screen == Screen.DIALOGUE && key == KeyEvent.VK_ESCAPE) exitDialogue(); else if (screen == Screen.DIALOGUE && confirm(key)) advanceDialogue(); else if (screen == Screen.MAP && (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_M)) screen = Screen.WORLD; else if (screen == Screen.PAUSE) handlePause(key); else if (screen == Screen.SETTINGS) handleSettings(key); else if (screen == Screen.CONTROLS && key == KeyEvent.VK_ESCAPE) screen = settingsReturn; else if (screen == Screen.EXTRAS && (confirm(key) || key == KeyEvent.VK_ESCAPE)) screen = Screen.MENU;
        else if (screen == Screen.CONTINUE_CHOICE) handleContinueChoice(key);
        else if (screen == Screen.BATTLE) handleBattle(key);
        else if (screen == Screen.BATTLE_RESULT && confirm(key)) retryBattle();
        else if (screen == Screen.END) handleEnd(key);
    }

    private void handleEnd(int key) {
        if (up(key) || down(key)) endIndex = 1 - endIndex;
        else if (confirm(key)) {
            if (endIndex == 0) {
                screen = Screen.WORLD;
                showToast("A próxima fase será adicionada em breve.");
            } else {
                screen = Screen.MENU;
            }
        } else if (key == KeyEvent.VK_ESCAPE) {
            screen = Screen.MENU;
        }
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        if (screen != Screen.MENU) return;

        double x = (event.getX() - offsetX) / scale;
        double y = (event.getY() - offsetY) / scale;

        if (insideHelpButton(x, y)) {
            screen = Screen.EXTRAS;
            repaint();
        } else {
            for (int index = 0; index < 4; index++) {
                if (insideMenuButton(x, y, index)) {
                    menuIndex = index;
                    handleMenu(KeyEvent.VK_ENTER);
                    repaint();
                    break;
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent event) {
    }

    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
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