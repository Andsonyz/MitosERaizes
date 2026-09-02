package br.com.entremitoseraizes.game;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Gerencia os dois saves locais do jogo.
 *
 * Os arquivos ficam fora do código-fonte e não dependem do registro do Windows.
 */
public final class SaveManager {

    /*
     * Permite definir uma pasta diferente através da propriedade:
     *
     * -Dentremitos.saves.dir="caminho"
     *
     * Caso não seja definida, utiliza uma pasta dentro do usuário.
     */
    private static final File SAVE_DIRECTORY = createSaveDirectory();

    private static File createSaveDirectory() {

        String customDirectory = System.getProperty("entremitos.saves.dir");

        if (customDirectory != null && !customDirectory.trim().isEmpty()) {
            return new File(customDirectory);
        }

        return new File(
            System.getProperty("user.home"),
            ".entremitoseraizes" + File.separator + "saves"
        );
    }

    boolean hasSave(int slot) {
        return Boolean.parseBoolean(
            read(slot).getProperty("exists", "false")
        );
    }

    void save(GameSession session) {

        if (session == null) {
            System.err.println("Não foi possível salvar: sessão inexistente.");
            return;
        }

        int slot = session.activeSlot();

        if (slot < 1 || slot > 2) {
            System.err.println(
                "Não foi possível salvar: slot inválido (" + slot + ")."
            );
            return;
        }

        Properties properties = new Properties();

        properties.setProperty("exists", "true");
        properties.setProperty("name", session.playerName());
        properties.setProperty("slot", String.valueOf(slot));

        properties.setProperty(
            "scene",
            String.valueOf(session.scene().ordinal())
        );

        properties.setProperty(
            "stage",
            String.valueOf(session.stage().ordinal())
        );

        properties.setProperty(
            "x",
            String.valueOf(session.playerX())
        );

        properties.setProperty(
            "y",
            String.valueOf(session.playerY())
        );

        properties.setProperty(
            "animal",
            String.valueOf(session.animalFollowing())
        );

        properties.setProperty(
            "race",
            String.valueOf(session.raceEndsAt())
        );

        properties.setProperty(
            "flags",
            join(session.completedCopy())
        );

        File saveFile = slot(slot);

        if (write(saveFile, properties)) {
            System.out.println(
                "Jogo salvo com sucesso no Slot " + slot + ":"
            );
            System.out.println(saveFile.getAbsolutePath());
        }
    }

    GameSession load(int slot) {

        if (slot < 1 || slot > 2) {
            return null;
        }

        Properties properties = read(slot);

        if (!Boolean.parseBoolean(
                properties.getProperty("exists", "false"))) {
            return null;
        }

        GameSession session = new GameSession();

        long race = readLong(
            properties,
            "race",
            0L
        );

        if (race > 0L && race < System.currentTimeMillis()) {
            race = 0L;
        }

        session.restore(
            properties.getProperty("name", "Viajante"),
            slot,

            readInt(
                properties,
                "scene",
                Scene.FOREST_PATH.ordinal()
            ),

            readInt(
                properties,
                "stage",
                Stage.FIND_CABIN.ordinal()
            ),

            readFloat(
                properties,
                "x",
                100f
            ),

            readFloat(
                properties,
                "y",
                540f
            ),

            Boolean.parseBoolean(
                properties.getProperty("animal", "false")
            ),

            race,

            split(
                properties.getProperty("flags", "")
            )
        );

        System.out.println(
            "Jogo carregado do Slot " + slot + "."
        );

        return session;
    }

    String summary(int slot) {

        Properties properties = read(slot);

        if (!Boolean.parseBoolean(
                properties.getProperty("exists", "false"))) {
            return "Vazio";
        }

        int ordinal = readInt(
            properties,
            "stage",
            0
        );

        Stage[] stages = Stage.values();

        String objective =
            ordinal >= 0 && ordinal < stages.length
                ? stages[ordinal].objective()
                : "Aventura em andamento";

        String name =
            properties.getProperty(
                "name",
                "Viajante"
            );

        return name + " - " + objective;
    }

    private Properties read(int slot) {

        Properties properties = new Properties();

        File file = slot(slot);

        if (!file.isFile()) {
            return properties;
        }

        try (FileInputStream input = new FileInputStream(file)) {

            properties.load(input);

        } catch (IOException exception) {

            System.err.println(
                "Erro ao ler o save do Slot "
                + slot
                + ": "
                + exception.getMessage()
            );
        }

        return properties;
    }

    private boolean write(
        File file,
        Properties properties) {

        if (!SAVE_DIRECTORY.isDirectory()) {

            if (!SAVE_DIRECTORY.mkdirs()
                    && !SAVE_DIRECTORY.isDirectory()) {

                System.err.println(
                    "Não foi possível criar a pasta de saves:"
                );

                System.err.println(
                    SAVE_DIRECTORY.getAbsolutePath()
                );

                return false;
            }
        }

        try (FileOutputStream output =
                 new FileOutputStream(file)) {

            properties.store(
                output,
                "Entre Mitos e Raizes - save local"
            );

            output.flush();

            return true;

        } catch (IOException exception) {

            System.err.println(
                "Erro ao salvar o jogo:"
            );

            System.err.println(
                exception.getMessage()
            );

            System.err.println(
                "Arquivo:"
            );

            System.err.println(
                file.getAbsolutePath()
            );

            return false;
        }
    }

    private File slot(int slot) {

        return new File(
            SAVE_DIRECTORY,
            "slot-" + slot + ".properties"
        );
    }

    private int readInt(
        Properties properties,
        String key,
        int fallback) {

        try {

            return Integer.parseInt(
                properties.getProperty(key)
            );

        } catch (NumberFormatException exception) {

            return fallback;
        }
    }

    private long readLong(
        Properties properties,
        String key,
        long fallback) {

        try {

            return Long.parseLong(
                properties.getProperty(key)
            );

        } catch (NumberFormatException exception) {

            return fallback;
        }
    }

    private float readFloat(
        Properties properties,
        String key,
        float fallback) {

        try {

            return Float.parseFloat(
                properties.getProperty(key)
            );

        } catch (NumberFormatException exception) {

            return fallback;
        }
    }

    private static String join(Set<String> flags) {

        StringBuilder result =
            new StringBuilder();

        for (String flag : flags) {

            if (result.length() > 0) {
                result.append(',');
            }

            result.append(
                flag.replace(",", "")
            );
        }

        return result.toString();
    }

    private static Set<String> split(String value) {

        if (value == null
                || value.trim().isEmpty()) {

            return Collections.emptySet();
        }

        Set<String> result =
            new HashSet<String>();

        for (String part : value.split(",")) {

            if (!part.trim().isEmpty()) {
                result.add(part.trim());
            }
        }

        return result;
    }
}