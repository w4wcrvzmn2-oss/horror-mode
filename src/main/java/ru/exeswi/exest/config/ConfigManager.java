package ru.exeswi.exest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.exeswi.exest.Exest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tiny JSON config loader. No external dependencies, Gson ships with Minecraft.
 */
public final class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "exest-horror.json";

    private static HorrorConfig config = new HorrorConfig();

    private ConfigManager() {
    }

    public static HorrorConfig get() {
        return config;
    }

    public static void load() {
        Path path = path();
        if (Files.exists(path)) {
            try {
                config = GSON.fromJson(Files.readString(path), HorrorConfig.class);
                if (config == null) {
                    config = new HorrorConfig();
                }
            } catch (Exception e) {
                Exest.LOGGER.error("Failed to read {}, falling back to defaults", FILE_NAME, e);
                config = new HorrorConfig();
            }
        }
        config.sanitize();
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(config));
        } catch (IOException e) {
            Exest.LOGGER.error("Failed to write {}", FILE_NAME, e);
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }
}
