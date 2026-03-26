package com.example.exile_overlay.client.render.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class EntityHealthBarConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EntityHealthBarConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_entity_healthbar.json";

    private static EntityHealthBarConfig instance;

    private boolean enabled = true;
    private int maxDistance = 24;
    private double heightAbove = 0.5;
    private int barWidth = 30;
    private int barHeight = 2;
    private float scale = 1.0f;
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

    private EntityHealthBarConfig() {}

    public static EntityHealthBarConfig getInstance() {
        if (instance == null) {
            instance = new EntityHealthBarConfig();
            instance.load();
        }
        return instance;
    }

    private Path getConfigPath() {
        String gameDir = System.getProperty("user.dir");
        return Paths.get(gameDir, "config", CONFIG_FILE_NAME);
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

    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> blacklist) { this.blacklist = new ArrayList<>(blacklist); }
}