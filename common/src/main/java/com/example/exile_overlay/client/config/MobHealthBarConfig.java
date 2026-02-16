package com.example.exile_overlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * MOBヘルスバー設定クラス
 * 
 * 【機能】
 * - プレイヤーヘルスバー表示設定
 * - バーサイズ設定
 * - 各種表示オプション
 */
public class MobHealthBarConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/MobHealthBarConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_mob_healthbar.json";
    private static MobHealthBarConfig instance;

    // 表示設定
    private boolean showHealthBar = true;
    private boolean showPlayerHealthBar = false;
    private boolean showForAllMobs = true;
    private boolean showOnlyWhenDamaged = false;
    private boolean showHealthValue = true;
    private boolean showMaxHealth = true;
    private boolean showPercentage = false;

    // サイズ設定
    private float barWidth = 20.0f;
    private float barHeight = 2.5f;
    private float scale = 1.0f;
    private float curveAmount = 0.5f;

    // 位置設定
    private float verticalOffset = 0.5f;
    private float horizontalOffset = 0.0f;
    private boolean showThroughWalls = true;

    // 色設定
    private int backgroundColor = 0x000000;
    private int borderColor = 0x333333;
    private int healthColorHigh = 0x00FF00;
    private int healthColorMedium = 0xFFFF00;
    private int healthColorLow = 0xFF0000;
    private float mediumThreshold = 0.5f;
    private float lowThreshold = 0.25f;

    private MobHealthBarConfig() {
    }

    public static MobHealthBarConfig getInstance() {
        if (instance == null) {
            instance = new MobHealthBarConfig();
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

            if (obj.has("showHealthBar")) showHealthBar = obj.get("showHealthBar").getAsBoolean();
            if (obj.has("showPlayerHealthBar")) showPlayerHealthBar = obj.get("showPlayerHealthBar").getAsBoolean();
            if (obj.has("showForAllMobs")) showForAllMobs = obj.get("showForAllMobs").getAsBoolean();
            if (obj.has("showOnlyWhenDamaged")) showOnlyWhenDamaged = obj.get("showOnlyWhenDamaged").getAsBoolean();
            if (obj.has("showHealthValue")) showHealthValue = obj.get("showHealthValue").getAsBoolean();
            if (obj.has("showMaxHealth")) showMaxHealth = obj.get("showMaxHealth").getAsBoolean();
            if (obj.has("showPercentage")) showPercentage = obj.get("showPercentage").getAsBoolean();

            if (obj.has("barWidth")) barWidth = obj.get("barWidth").getAsFloat();
            if (obj.has("barHeight")) barHeight = obj.get("barHeight").getAsFloat();
            if (obj.has("scale")) scale = obj.get("scale").getAsFloat();
            if (obj.has("curveAmount")) curveAmount = obj.get("curveAmount").getAsFloat();

            if (obj.has("verticalOffset")) verticalOffset = obj.get("verticalOffset").getAsFloat();
            if (obj.has("horizontalOffset")) horizontalOffset = obj.get("horizontalOffset").getAsFloat();
            if (obj.has("showThroughWalls")) showThroughWalls = obj.get("showThroughWalls").getAsBoolean();

            if (obj.has("mediumThreshold")) mediumThreshold = obj.get("mediumThreshold").getAsFloat();
            if (obj.has("lowThreshold")) lowThreshold = obj.get("lowThreshold").getAsFloat();

            if (obj.has("colors")) {
                JsonObject colors = obj.getAsJsonObject("colors");
                if (colors.has("background")) backgroundColor = colors.get("background").getAsInt();
                if (colors.has("border")) borderColor = colors.get("border").getAsInt();
                if (colors.has("healthHigh")) healthColorHigh = colors.get("healthHigh").getAsInt();
                if (colors.has("healthMedium")) healthColorMedium = colors.get("healthMedium").getAsInt();
                if (colors.has("healthLow")) healthColorLow = colors.get("healthLow").getAsInt();
            }

            LOGGER.info("Loaded mob health bar config");
        } catch (IOException e) {
            LOGGER.error("Failed to load mob health bar config: {}", e.getMessage());
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
        obj.addProperty("showHealthBar", showHealthBar);
        obj.addProperty("showPlayerHealthBar", showPlayerHealthBar);
        obj.addProperty("showForAllMobs", showForAllMobs);
        obj.addProperty("showOnlyWhenDamaged", showOnlyWhenDamaged);
        obj.addProperty("showHealthValue", showHealthValue);
        obj.addProperty("showMaxHealth", showMaxHealth);
        obj.addProperty("showPercentage", showPercentage);

        obj.addProperty("barWidth", barWidth);
        obj.addProperty("barHeight", barHeight);
        obj.addProperty("scale", scale);
        obj.addProperty("curveAmount", curveAmount);

        obj.addProperty("verticalOffset", verticalOffset);
        obj.addProperty("horizontalOffset", horizontalOffset);
        obj.addProperty("showThroughWalls", showThroughWalls);

        obj.addProperty("mediumThreshold", mediumThreshold);
        obj.addProperty("lowThreshold", lowThreshold);

        JsonObject colors = new JsonObject();
        colors.addProperty("background", backgroundColor);
        colors.addProperty("border", borderColor);
        colors.addProperty("healthHigh", healthColorHigh);
        colors.addProperty("healthMedium", healthColorMedium);
        colors.addProperty("healthLow", healthColorLow);
        obj.add("colors", colors);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved mob health bar config");
        } catch (IOException e) {
            LOGGER.error("Failed to save mob health bar config: {}", e.getMessage());
        }
    }

    // Getters
    public boolean isShowHealthBar() { return showHealthBar; }
    public boolean isShowPlayerHealthBar() { return showPlayerHealthBar; }
    public boolean isShowForAllMobs() { return showForAllMobs; }
    public boolean isShowOnlyWhenDamaged() { return showOnlyWhenDamaged; }
    public boolean isShowHealthValue() { return showHealthValue; }
    public boolean isShowMaxHealth() { return showMaxHealth; }
    public boolean isShowPercentage() { return showPercentage; }

    public float getBarWidth() { return barWidth; }
    public float getBarHeight() { return barHeight; }
    public float getScale() { return scale; }
    public float getCurveAmount() { return curveAmount; }

    public float getVerticalOffset() { return verticalOffset; }
    public float getHorizontalOffset() { return horizontalOffset; }
    public boolean isShowThroughWalls() { return showThroughWalls; }

    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public int getHealthColorHigh() { return healthColorHigh; }
    public int getHealthColorMedium() { return healthColorMedium; }
    public int getHealthColorLow() { return healthColorLow; }
    public float getMediumThreshold() { return mediumThreshold; }
    public float getLowThreshold() { return lowThreshold; }

    public int getHealthColor(float healthRatio) {
        if (healthRatio <= lowThreshold) {
            return healthColorLow;
        } else if (healthRatio <= mediumThreshold) {
            return healthColorMedium;
        } else {
            return healthColorHigh;
        }
    }

    // Setters
    public void setShowHealthBar(boolean show) { this.showHealthBar = show; }
    public void setShowPlayerHealthBar(boolean show) { this.showPlayerHealthBar = show; }
    public void setShowForAllMobs(boolean show) { this.showForAllMobs = show; }
    public void setShowOnlyWhenDamaged(boolean show) { this.showOnlyWhenDamaged = show; }
    public void setShowHealthValue(boolean show) { this.showHealthValue = show; }
    public void setShowMaxHealth(boolean show) { this.showMaxHealth = show; }
    public void setShowPercentage(boolean show) { this.showPercentage = show; }

    public void setBarWidth(float width) { this.barWidth = width; }
    public void setBarHeight(float height) { this.barHeight = height; }
    public void setScale(float scale) { this.scale = scale; }
    public void setCurveAmount(float amount) { this.curveAmount = amount; }

    public void setVerticalOffset(float offset) { this.verticalOffset = offset; }
    public void setHorizontalOffset(float offset) { this.horizontalOffset = offset; }
    public void setShowThroughWalls(boolean show) { this.showThroughWalls = show; }

    public void setBackgroundColor(int color) { this.backgroundColor = color; }
    public void setBorderColor(int color) { this.borderColor = color; }
    public void setHealthColorHigh(int color) { this.healthColorHigh = color; }
    public void setHealthColorMedium(int color) { this.healthColorMedium = color; }
    public void setHealthColorLow(int color) { this.healthColorLow = color; }
    public void setMediumThreshold(float threshold) { this.mediumThreshold = threshold; }
    public void setLowThreshold(float threshold) { this.lowThreshold = threshold; }
}
