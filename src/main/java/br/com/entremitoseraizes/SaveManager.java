package br.com.entremitoseraizes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/** Dois saves locais em arquivos ignorados pelo Git, sem usar o registro do Windows. */
final class SaveManager {
    private static final File SAVE_DIRECTORY = new File(System.getProperty("entremitos.saves.dir", ".saves"));

    boolean hasSave(int slot) { return Boolean.parseBoolean(read(slot).getProperty("exists", "false")); }

    void save(GameSession session) {
        Properties properties = new Properties();
        properties.setProperty("exists", "true");
        properties.setProperty("name", session.playerName());
        properties.setProperty("scene", String.valueOf(session.scene().ordinal()));
        properties.setProperty("stage", String.valueOf(session.stage().ordinal()));
        properties.setProperty("x", String.valueOf(session.playerX()));
        properties.setProperty("y", String.valueOf(session.playerY()));
        properties.setProperty("animal", String.valueOf(session.animalFollowing()));
        properties.setProperty("race", String.valueOf(session.raceEndsAt()));
        properties.setProperty("flags", join(session.completedCopy()));
        write(slot(session.activeSlot()), properties);
    }

    GameSession load(int slot) {
        Properties properties = read(slot);
        if (!Boolean.parseBoolean(properties.getProperty("exists", "false"))) return null;
        GameSession session = new GameSession();
        long race = readLong(properties, "race", 0L);
        if (race > 0L && race < System.currentTimeMillis()) race = 0L;
        session.restore(properties.getProperty("name", "Viajante"), slot,
            readInt(properties, "scene", Scene.FOREST_PATH.ordinal()), readInt(properties, "stage", Stage.FIND_CABIN.ordinal()),
            readFloat(properties, "x", 100f), readFloat(properties, "y", 540f),
            Boolean.parseBoolean(properties.getProperty("animal", "false")), race, split(properties.getProperty("flags", "")));
        return session;
    }

    String summary(int slot) {
        Properties properties = read(slot);
        if (!Boolean.parseBoolean(properties.getProperty("exists", "false"))) return "Vazio";
        int ordinal = readInt(properties, "stage", 0);
        Stage[] stages = Stage.values();
        String objective = ordinal >= 0 && ordinal < stages.length ? stages[ordinal].objective() : "Aventura em andamento";
        return properties.getProperty("name", "Viajante") + " - " + objective;
    }

    private Properties read(int slot) {
        Properties properties = new Properties();
        File file = slot(slot);
        if (!file.isFile()) return properties;
        try {
            FileInputStream input = new FileInputStream(file);
            try { properties.load(input); } finally { input.close(); }
        } catch (IOException ignored) { }
        return properties;
    }

    private void write(File file, Properties properties) {
        if (!SAVE_DIRECTORY.isDirectory() && !SAVE_DIRECTORY.mkdirs()) return;
        try {
            FileOutputStream output = new FileOutputStream(file);
            try { properties.store(output, "Entre Mitos e Raizes - save local"); } finally { output.close(); }
        } catch (IOException ignored) { }
    }

    private File slot(int slot) { return new File(SAVE_DIRECTORY, "slot-" + slot + ".properties"); }
    private int readInt(Properties properties, String key, int fallback) { try { return Integer.parseInt(properties.getProperty(key)); } catch (NumberFormatException ignored) { return fallback; } }
    private long readLong(Properties properties, String key, long fallback) { try { return Long.parseLong(properties.getProperty(key)); } catch (NumberFormatException ignored) { return fallback; } }
    private float readFloat(Properties properties, String key, float fallback) { try { return Float.parseFloat(properties.getProperty(key)); } catch (NumberFormatException ignored) { return fallback; } }

    private static String join(Set<String> flags) {
        StringBuilder result = new StringBuilder();
        for (String flag : flags) { if (result.length() > 0) result.append(','); result.append(flag.replace(",", "")); }
        return result.toString();
    }

    private static Set<String> split(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<String>();
        for (String part : value.split(",")) if (!part.trim().isEmpty()) result.add(part.trim());
        return result;
    }
}
