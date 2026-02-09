package com.indestructible13.better_auto_fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("better-auto-fishing-config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Check whether the mod is active or not
    public static boolean getActive() {
        Config config = ConfigManager.load();
        return config.active;
    }

    // Set whether the mod is active or not
    public static void setActive(boolean active) {
        Config config = ConfigManager.load();
        config.active = active;
        save(config);
    }

    public static int getReelDelay() {
        Config config = ConfigManager.load();
        return config.reelDelay;
    }

    public static void setReelDelay(int reelDelay) {
        Config config = ConfigManager.load();
        config.reelDelay = reelDelay;
        save(config);
    }

    public static int getCastDelay() {
        Config config = ConfigManager.load();
        return config.castDelay;
    }

    public static void setCastDelay(int castDelay) {
        Config config = ConfigManager.load();
        config.castDelay = castDelay;
        save(config);
    }

    public static Config load() {
        if (!CONFIG_FILE.exists()) {
            return new Config(); // Return defaults if file doesn't exist
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Config(); // Return defaults if there's an error
        }
    }

    public static void save(Config config) {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("better-auto-fishing-config.json").toString();
    }
}
