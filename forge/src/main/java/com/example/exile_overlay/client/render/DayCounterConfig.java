package com.example.exile_overlay.client.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DayCounterConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DayCounterConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_day_counter.json";
    private static volatile DayCounterConfig instance;
    private static final Object LOCK = new Object();

    private int soundVolume = 5;

    private DayCounterConfig() {
    }

    public static DayCounterConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DayCounterConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    private Path getConfigPath() {
        Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
        return gameDir.resolve("config").resolve(CONFIG_FILE_NAME);
    }

    public void load() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (obj.has("soundVolume")) {
                soundVolume = obj.get("soundVolume").getAsInt();
            }

            LOGGER.info("Loaded day counter config");
        } catch (IOException e) {
            LOGGER.error("Failed to load day counter config: {}", e.getMessage());
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            LOGGER.error("Failed to create config directory: {}", e.getMessage());
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("soundVolume", soundVolume);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved day counter config");
        } catch (IOException e) {
            LOGGER.error("Failed to save day counter config: {}", e.getMessage());
        }
    }

    public int getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(int volume) {
        this.soundVolume = Math.max(0, Math.min(100, volume));
    }

    public float getSoundVolumeFloat() {
        return soundVolume / 100.0f;
    }
}