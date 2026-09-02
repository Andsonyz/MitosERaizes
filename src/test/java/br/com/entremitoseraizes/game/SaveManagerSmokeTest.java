package br.com.entremitoseraizes.game;

import java.io.File;

/** Confere os dois slots sem tocar nos saves reais de quem joga. */
public final class SaveManagerSmokeTest {
    private SaveManagerSmokeTest() { }

    public static void main(String[] args) {
        File directory = new File("tmp-test-saves");
        System.setProperty("entremitos.saves.dir", directory.getPath());
        delete(directory);
        GameSession original = new GameSession();
        original.startNew("Lia", 2);
        original.setStage(Stage.TALK_TO_CURUPIRA);
        original.setScene(Scene.CABIN_APPROACH);
        original.setPosition(934f, 442f);
        original.setAnimalFollowing(true);
        original.complete("clue-footprints");
        SaveManager saves = new SaveManager();
        saves.save(original);

        GameSession loaded = saves.load(2);
        if (loaded == null || !"Lia".equals(loaded.playerName()) || loaded.stage() != Stage.TALK_TO_CURUPIRA
                || loaded.scene() != Scene.CABIN_APPROACH || loaded.playerX() != 934f || !loaded.animalFollowing() || !loaded.isComplete("clue-footprints")) {
            throw new AssertionError("Save não foi restaurado corretamente");
        }
        if (saves.hasSave(1)) throw new AssertionError("Slots foram misturados");
        delete(directory);
        System.out.println("Sistema de dois saves validado.");
    }

    private static void delete(File file) {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        if (!file.delete()) throw new AssertionError("Não foi possível limpar teste local");
    }
}
