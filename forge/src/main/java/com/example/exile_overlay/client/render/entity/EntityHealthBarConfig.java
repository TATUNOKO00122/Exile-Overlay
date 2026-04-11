package com.example.exile_overlay.client.render.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EntityHealthBarConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EntityHealthBarConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_entity_healthbar.json";

    private static volatile EntityHealthBarConfig instance;
    private static final Object LOCK = new Object();

    private boolean enabled = false;
    private int maxDistance = 24;
    private double heightAbove = 0.5;
    private int barWidth = 30;
    private int barHeight = 2;
    private float scale = 1.0f;
    private int displayDuration = 5;
    private String healthBarColor = "8B0000";
    private List<String> blacklist = new ArrayList<>(DEFAULT_BLACKLIST);

    public static final List<String> DEFAULT_BLACKLIST = List.of(
        "minecraft:shulker",
        "minecraft:armor_stand",
        "minecraft:item_frame",
        "minecraft:glow_item_frame",
        "minecraft:painting",
        "minecraft:end_crystal",
        "minecraft:experience_orb"
    );

    public enum ColorPreset {
        DARK_RED("8B0000", "exile_overlay.config.hp_color.dark_red"),
        BRICK_RED("B22222", "exile_overlay.config.hp_color.brick_red"),
        PINK_RED("FF4040", "exile_overlay.config.hp_color.pink_red"),
        CRIMSON("DC143C", "exile_overlay.config.hp_color.crimson"),
        PURE_RED("FF0000", "exile_overlay.config.hp_color.pure_red");

        private final String hex;
        private final String translationKey;

        ColorPreset(String hex, String translationKey) {
            this.hex = hex;
            this.translationKey = translationKey;
        }

        public String getHex() { return hex; }
        public String getTranslationKey() { return translationKey; }
        public int getColorValue() { return 0xFF000000 | Integer.parseInt(hex, 16); }

        public static ColorPreset fromHex(String hex) {
            for (ColorPreset preset : values()) {
                if (preset.hex.equalsIgnoreCase(hex)) {
                    return preset;
                }
            }
            return DARK_RED;
        }
    }

    private EntityHealthBarConfig() {}

    public static EntityHealthBarConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EntityHealthBarConfig();
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

            if (obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
            if (obj.has("maxDistance")) maxDistance = obj.get("maxDistance").getAsInt();
            if (obj.has("heightAbove")) heightAbove = obj.get("heightAbove").getAsDouble();
            if (obj.has("barWidth")) barWidth = obj.get("barWidth").getAsInt();
            if (obj.has("barHeight")) barHeight = obj.get("barHeight").getAsInt();
            if (obj.has("scale")) scale = obj.get("scale").getAsFloat();
            if (obj.has("displayDuration")) displayDuration = obj.get("displayDuration").getAsInt();
            if (obj.has("healthBarColor")) healthBarColor = obj.get("healthBarColor").getAsString();

            if (obj.has("blacklist")) {
                blacklist.clear();
                for (var element : obj.getAsJsonArray("blacklist")) {
                    blacklist.add(element.getAsString());
                }
            }

            LOGGER.info("Loaded entity health bar config");
        } catch (Exception e) {
            LOGGER.error("Failed to load entity health bar config: {}", e.getMessage());
        }
    }

    public void save() {
        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
        } catch (Exception e) {
            LOGGER.error("Failed to create config directory: {}", e.getMessage());
            return;
        }

        JsonObject obj = new JsonObject();
        obj.addProperty("enabled", enabled);
        obj.addProperty("maxDistance", maxDistance);
        obj.addProperty("heightAbove", heightAbove);
        obj.addProperty("barWidth", barWidth);
        obj.addProperty("barHeight", barHeight);
        obj.addProperty("scale", scale);
        obj.addProperty("displayDuration", displayDuration);
        obj.addProperty("healthBarColor", healthBarColor);

        var blacklistArray = new com.google.gson.JsonArray();
        for (String entry : blacklist) {
            blacklistArray.add(entry);
        }
        obj.add("blacklist", blacklistArray);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved entity health bar config");
        } catch (Exception e) {
            LOGGER.error("Failed to save entity health bar config: {}", e.getMessage());
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getMaxDistance() { return maxDistance; }
    public void setMaxDistance(int maxDistance) { this.maxDistance = maxDistance; }

    public double getHeightAbove() { return heightAbove; }
    public void setHeightAbove(double heightAbove) { this.heightAbove = heightAbove; }

    public int getBarWidth() { return barWidth; }
    public void setBarWidth(int barWidth) { this.barWidth = barWidth; }

    public int getBarHeight() { return barHeight; }
    public void setBarHeight(int barHeight) { this.barHeight = barHeight; }

    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public int getDisplayDuration() { return displayDuration; }
    public void setDisplayDuration(int displayDuration) { this.displayDuration = displayDuration; }

    public String getHealthBarColor() { return healthBarColor; }
    public void setHealthBarColor(String healthBarColor) { this.healthBarColor = healthBarColor; }

    public int getHealthBarColorHex() {
        try {
            return 0xFF000000 | Integer.parseInt(healthBarColor, 16);
        } catch (NumberFormatException e) {
            return 0xFF8B0000;
        }
    }

    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> blacklist) { this.blacklist = new ArrayList<>(blacklist); }
}