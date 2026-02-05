package com.indestructible13.better_auto_fishing.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BetterAutoFishingConfigManager {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("better-auto-fishing-config.json").toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Check whether the mod is active or not
    public static boolean getActive() {
        BetterAutoFishingConfig config = BetterAutoFishingConfigManager.load();
        return config.active;
    }

    // Set whether the mod is active or not
    public static void setActive(boolean active) {
        BetterAutoFishingConfig config = BetterAutoFishingConfigManager.load();
        config.active = active;
        save(config);
    }

    public static BetterAutoFishingConfig load() {
        if (!CONFIG_FILE.exists()) {
            return new BetterAutoFishingConfig(); // Return defaults if file doesn't exist
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, BetterAutoFishingConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return new BetterAutoFishingConfig(); // Return defaults if there's an error
        }
    }

    public static void save(BetterAutoFishingConfig config) {
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
