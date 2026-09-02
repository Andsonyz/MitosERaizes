package br.com.entremitoseraizes.game;

/** Verifica o estado principal da fase sem abrir a janela Swing. */
public final class GameSessionSmokeTest {
    private GameSessionSmokeTest() { }

    public static void main(String[] args) {
        GameSession session = new GameSession();
        session.startNew("Aventureira", 2);
        if (!"Aventureira".equals(session.playerName())) throw new AssertionError("Nome não foi salvo");
        if (session.activeSlot() != 2 || session.stage() != Stage.FIND_CABIN) throw new AssertionError("Novo jogo inválido");

        session.complete("clue-footprints");
        session.complete("clue-axe");
        session.complete("clue-trap");
        if (!session.completedAll("clue-footprints", "clue-axe", "clue-trap")) throw new AssertionError("Pistas ausentes");

        session.setScene(Scene.CABIN_APPROACH);
        session.setPosition(950f, 440f);
        if (session.scene() != Scene.CABIN_APPROACH || session.playerX() != 950f) throw new AssertionError("Troca de cenário inválida");
        session.setScene(Scene.CABIN_INTERIOR);
        session.setStage(Stage.TALK_TO_CURUPIRA);
        if (session.stage() != Stage.TALK_TO_CURUPIRA) throw new AssertionError("Etapa da cabana inválida");

        System.out.println("Estado da fase do Curupira validado.");
    }
}