package name.modid.horror.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class HorrorConfigManager {

    private static final Gson GSON =
            new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance()
                    .getConfigDir()
                    .resolve("horroroperator.json");

    private static HorrorConfig config;

    private HorrorConfigManager() {}

    public static void load() {

        try {

            if (!Files.exists(CONFIG_PATH)) {

                config = new HorrorConfig();

                save();

                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {

                config = GSON.fromJson(reader, HorrorConfig.class);

                if (config == null) {
                    config = new HorrorConfig();
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            config = new HorrorConfig();
        }
    }

    public static void save() {

        try {

            Files.createDirectories(CONFIG_PATH.getParent());

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {

                GSON.toJson(config, writer);
            }

        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static HorrorConfig getConfig() {

        if (config == null) {
            load();
        }

        return config;
    }
}