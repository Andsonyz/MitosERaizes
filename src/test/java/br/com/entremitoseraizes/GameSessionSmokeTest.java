package br.com.entremitoseraizes;

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

        session.startRace();
        if (!session.raceIsActive() || session.stage() != Stage.RACE_TO_TREE) throw new AssertionError("Corrida não iniciada");
        session.stopRace();
        if (session.raceIsActive()) throw new AssertionError("Corrida não encerrada");

        System.out.println("Estado da fase do Curupira validado.");
    }
}
