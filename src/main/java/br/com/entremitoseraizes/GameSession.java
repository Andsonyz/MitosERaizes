package br.com.entremitoseraizes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Estado persistente da primeira fase, sem guardar dados pessoais além do apelido escolhido. */
final class GameSession {
    static final float OUTDOOR_WIDTH = 3600f;
    static final float OUTDOOR_HEIGHT = 1900f;

    private String playerName = "Viajante";
    private int activeSlot = 1;
    private Scene scene = Scene.OUTDOOR;
    private Stage stage = Stage.FIND_CABIN;
    private float playerX = 310f;
    private float playerY = 1220f;
    private boolean animalFollowing;
    private long raceEndsAt;
    private final Set<String> completed = new HashSet<String>();

    void startNew(String name, int slot) {
        playerName = cleanName(name);
        activeSlot = slot;
        scene = Scene.OUTDOOR;
        stage = Stage.FIND_CABIN;
        playerX = 310f;
        playerY = 1220f;
        animalFollowing = false;
        raceEndsAt = 0L;
        completed.clear();
    }

    String playerName() { return playerName; }
    int activeSlot() { return activeSlot; }
    Scene scene() { return scene; }
    Stage stage() { return stage; }
    float playerX() { return playerX; }
    float playerY() { return playerY; }
    boolean animalFollowing() { return animalFollowing; }
    long raceEndsAt() { return raceEndsAt; }
    boolean raceIsActive() { return stage == Stage.RACE_TO_TREE && raceEndsAt > 0L; }
    boolean isComplete(String id) { return completed.contains(id); }
    Set<String> completedCopy() { return new HashSet<String>(completed); }

    void setScene(Scene value) { scene = value; }
    void setStage(Stage value) { stage = value; }
    void setPosition(float x, float y) { playerX = x; playerY = y; }
    void setAnimalFollowing(boolean value) { animalFollowing = value; }
    void setRaceEndsAt(long value) { raceEndsAt = value; }

    void complete(String id) { completed.add(id); }
    boolean completedAll(String... ids) { return completed.containsAll(Arrays.asList(ids)); }

    void startRace() {
        raceEndsAt = System.currentTimeMillis() + 30000L;
        stage = Stage.RACE_TO_TREE;
    }

    void stopRace() { raceEndsAt = 0L; }

    void restore(String name, int slot, int sceneOrdinal, int stageOrdinal, float x, float y,
                 boolean follows, long raceEnd, Set<String> flags) {
        playerName = cleanName(name);
        activeSlot = slot;
        scene = safeScene(sceneOrdinal);
        stage = safeStage(stageOrdinal);
        playerX = x;
        playerY = y;
        animalFollowing = follows;
        raceEndsAt = raceEnd;
        completed.clear();
        completed.addAll(flags);
    }

    private static Scene safeScene(int ordinal) {
        Scene[] values = Scene.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Scene.OUTDOOR;
    }

    private static Stage safeStage(int ordinal) {
        Stage[] values = Stage.values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : Stage.FIND_CABIN;
    }

    private static String cleanName(String value) {
        if (value == null) return "Viajante";
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return "Viajante";
        return trimmed.length() > 18 ? trimmed.substring(0, 18) : trimmed;
    }
}
