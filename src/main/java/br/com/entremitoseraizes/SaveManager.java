package br.com.entremitoseraizes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Dois slots locais usando as preferências do sistema, fora do repositório público. */
final class SaveManager {
    private static final String NODE = "saves";
    private final Preferences preferences = Preferences.userNodeForPackage(SaveManager.class).node(NODE);

    boolean hasSave(int slot) { return preferences.getBoolean(key(slot, "exists"), false); }

    void save(GameSession session) {
        int slot = session.activeSlot();
        preferences.putBoolean(key(slot, "exists"), true);
        preferences.put(key(slot, "name"), session.playerName());
        preferences.putInt(key(slot, "scene"), session.scene().ordinal());
        preferences.putInt(key(slot, "stage"), session.stage().ordinal());
        preferences.putFloat(key(slot, "x"), session.playerX());
        preferences.putFloat(key(slot, "y"), session.playerY());
        preferences.putBoolean(key(slot, "animal"), session.animalFollowing());
        preferences.putLong(key(slot, "race"), session.raceEndsAt());
        preferences.put(key(slot, "flags"), join(session.completedCopy()));
        flushQuietly();
    }

    GameSession load(int slot) {
        if (!hasSave(slot)) return null;
        GameSession session = new GameSession();
        String name = preferences.get(key(slot, "name"), "Viajante");
        int scene = preferences.getInt(key(slot, "scene"), Scene.OUTDOOR.ordinal());
        int stage = preferences.getInt(key(slot, "stage"), Stage.FIND_CABIN.ordinal());
        float x = preferences.getFloat(key(slot, "x"), 310f);
        float y = preferences.getFloat(key(slot, "y"), 1220f);
        boolean animal = preferences.getBoolean(key(slot, "animal"), false);
        long race = preferences.getLong(key(slot, "race"), 0L);
        if (race > 0L && race < System.currentTimeMillis()) race = 0L;
        session.restore(name, slot, scene, stage, x, y, animal, race, split(preferences.get(key(slot, "flags"), "")));
        return session;
    }

    String summary(int slot) {
        if (!hasSave(slot)) return "Vazio";
        String name = preferences.get(key(slot, "name"), "Viajante");
        int stage = preferences.getInt(key(slot, "stage"), 0);
        Stage[] stages = Stage.values();
        String objective = stage >= 0 && stage < stages.length ? stages[stage].objective() : "Aventura em andamento";
        return name + " - " + objective;
    }

    private String key(int slot, String field) { return "slot" + slot + "." + field; }

    private static String join(Set<String> flags) {
        StringBuilder result = new StringBuilder();
        for (String flag : flags) {
            if (result.length() > 0) result.append(',');
            result.append(flag.replace(",", ""));
        }
        return result.toString();
    }

    private static Set<String> split(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<String>();
        for (String part : value.split(",")) if (!part.trim().isEmpty()) result.add(part.trim());
        return result;
    }

    private void flushQuietly() {
        try { preferences.flush(); }
        catch (BackingStoreException ignored) { }
    }
}
