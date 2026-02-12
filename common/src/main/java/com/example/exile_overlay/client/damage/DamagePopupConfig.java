package com.example.exile_overlay.client.damage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DamagePopupConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DamagePopupConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_damage.json";
    private static DamagePopupConfig instance;

    private float baseScale = 0.03f;
    private float criticalScale = 0.05f;
    private int displayDuration = 60;
    private boolean enableShadow = false;
    private float horizontalSpread = 0.8f;
    private int fadeInDuration = 5;
    private int fadeOutDuration = 15;
    private boolean enableDamageStacking = false;
    private float stackingRadius = 1.0f;
    private int comboTimeWindow = 60;
    private boolean showDamage = true;
    private int maxDamageTexts = 15;
    private float repulsionRadius = 0.5f;
    private float repulsionStrength = 0.1f;

    private int normalDamageColor = 0xFFFFFF;
    private int criticalDamageColor = 0xFFFF00;
    private int healingColor = 0x00FF00;
    private int poisonDamageColor = 0x00FF00;
    private int fireDamageColor = 0xFF6600;
    private int iceDamageColor = 0x00CCFF;
    private int lightningDamageColor = 0xFFFF66;

    private DamagePopupConfig() {
    }

    public static DamagePopupConfig getInstance() {
        if (instance == null) {
            instance = new DamagePopupConfig();
            instance.load();
        }
        return instance;
    }

    private Path getConfigPath() {
        // Minecraft.gameDirectoryの代わりにシステムプロパティを使用
        String gameDir = System.getProperty("user.dir");
        return Paths.get(gameDir).resolve("config").resolve(CONFIG_FILE_NAME);
    }

    public void load() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject jsonObj = GSON.fromJson(json, JsonObject.class);

            if (jsonObj.has("baseScale")) {
                baseScale = jsonObj.get("baseScale").getAsFloat();
            }
            if (jsonObj.has("criticalScale")) {
                criticalScale = jsonObj.get("criticalScale").getAsFloat();
            }
            if (jsonObj.has("displayDuration")) {
                displayDuration = jsonObj.get("displayDuration").getAsInt();
            }
            if (jsonObj.has("enableShadow")) {
                enableShadow = jsonObj.get("enableShadow").getAsBoolean();
            }
            if (jsonObj.has("horizontalSpread")) {
                horizontalSpread = jsonObj.get("horizontalSpread").getAsFloat();
            }
            if (jsonObj.has("fadeInDuration")) {
                fadeInDuration = jsonObj.get("fadeInDuration").getAsInt();
            }
            if (jsonObj.has("fadeOutDuration")) {
                fadeOutDuration = jsonObj.get("fadeOutDuration").getAsInt();
            }
            if (jsonObj.has("enableDamageStacking")) {
                enableDamageStacking = jsonObj.get("enableDamageStacking").getAsBoolean();
            }
            if (jsonObj.has("stackingRadius")) {
                stackingRadius = jsonObj.get("stackingRadius").getAsFloat();
            }
            if (jsonObj.has("comboTimeWindow")) {
                comboTimeWindow = jsonObj.get("comboTimeWindow").getAsInt();
            }
            if (jsonObj.has("showDamage")) {
                showDamage = jsonObj.get("showDamage").getAsBoolean();
            }
            if (jsonObj.has("maxDamageTexts")) {
                maxDamageTexts = jsonObj.get("maxDamageTexts").getAsInt();
            }
            if (jsonObj.has("repulsionRadius")) {
                repulsionRadius = jsonObj.get("repulsionRadius").getAsFloat();
            }
            if (jsonObj.has("repulsionStrength")) {
                repulsionStrength = jsonObj.get("repulsionStrength").getAsFloat();
            }

            if (jsonObj.has("colors")) {
                JsonObject colors = jsonObj.getAsJsonObject("colors");
                if (colors.has("normal")) {
                    normalDamageColor = colors.get("normal").getAsInt();
                }
                if (colors.has("critical")) {
                    criticalDamageColor = colors.get("critical").getAsInt();
                }
                if (colors.has("healing")) {
                    healingColor = colors.get("healing").getAsInt();
                }
                if (colors.has("poison")) {
                    poisonDamageColor = colors.get("poison").getAsInt();
                }
                if (colors.has("fire")) {
                    fireDamageColor = colors.get("fire").getAsInt();
                }
                if (colors.has("ice")) {
                    iceDamageColor = colors.get("ice").getAsInt();
                }
                if (colors.has("lightning")) {
                    lightningDamageColor = colors.get("lightning").getAsInt();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load damage popup config: {}", e.getMessage());
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

        JsonObject json = new JsonObject();
        json.addProperty("baseScale", baseScale);
        json.addProperty("criticalScale", criticalScale);
        json.addProperty("displayDuration", displayDuration);
        json.addProperty("enableShadow", enableShadow);
        json.addProperty("horizontalSpread", horizontalSpread);
        json.addProperty("fadeInDuration", fadeInDuration);
        json.addProperty("fadeOutDuration", fadeOutDuration);
        json.addProperty("enableDamageStacking", enableDamageStacking);
        json.addProperty("stackingRadius", stackingRadius);
        json.addProperty("comboTimeWindow", comboTimeWindow);
        json.addProperty("showDamage", showDamage);
        json.addProperty("maxDamageTexts", maxDamageTexts);
        json.addProperty("repulsionRadius", repulsionRadius);
        json.addProperty("repulsionStrength", repulsionStrength);

        JsonObject colors = new JsonObject();
        colors.addProperty("normal", normalDamageColor);
        colors.addProperty("critical", criticalDamageColor);
        colors.addProperty("healing", healingColor);
        colors.addProperty("poison", poisonDamageColor);
        colors.addProperty("fire", fireDamageColor);
        colors.addProperty("ice", iceDamageColor);
        colors.addProperty("lightning", lightningDamageColor);
        json.add("colors", colors);

        try {
            Files.writeString(configPath, GSON.toJson(json));
        } catch (IOException e) {
            LOGGER.error("Failed to save damage popup config: {}", e.getMessage());
        }
    }

    public float getBaseScale() {
        return baseScale;
    }

    public float getCriticalScale() {
        return criticalScale;
    }

    public int getDisplayDuration() {
        return displayDuration;
    }

    public boolean isEnableShadow() {
        return enableShadow;
    }

    public float getHorizontalSpread() {
        return horizontalSpread;
    }

    public int getFadeInDuration() {
        return fadeInDuration;
    }

    public int getFadeOutDuration() {
        return fadeOutDuration;
    }

    public boolean isEnableDamageStacking() {
        return enableDamageStacking;
    }

    public float getStackingRadius() {
        return stackingRadius;
    }

    public int getComboTimeWindow() {
        return comboTimeWindow;
    }

    public boolean isShowDamage() {
        return showDamage;
    }

    public int getMaxDamageTexts() {
        return maxDamageTexts;
    }

    public float getRepulsionRadius() {
        return repulsionRadius;
    }

    public float getRepulsionStrength() {
        return repulsionStrength;
    }

    public int getNormalDamageColor() {
        return normalDamageColor;
    }

    public int getCriticalDamageColor() {
        return criticalDamageColor;
    }

    public int getHealingColor() {
        return healingColor;
    }

    public int getPoisonDamageColor() {
        return poisonDamageColor;
    }

    public int getFireDamageColor() {
        return fireDamageColor;
    }

    public int getIceDamageColor() {
        return iceDamageColor;
    }

    public int getLightningDamageColor() {
        return lightningDamageColor;
    }

    public void setBaseScale(float scale) {
        this.baseScale = scale;
    }

    public void setCriticalScale(float scale) {
        this.criticalScale = scale;
    }

    public void setDisplayDuration(int duration) {
        this.displayDuration = duration;
    }

    public void setEnableShadow(boolean enable) {
        this.enableShadow = enable;
    }

    public void setHorizontalSpread(float spread) {
        this.horizontalSpread = spread;
    }

    public void setFadeInDuration(int duration) {
        this.fadeInDuration = duration;
    }

    public void setFadeOutDuration(int duration) {
        this.fadeOutDuration = duration;
    }

    public void setEnableDamageStacking(boolean enable) {
        this.enableDamageStacking = enable;
    }

    public void setStackingRadius(float radius) {
        this.stackingRadius = radius;
    }

    public void setComboTimeWindow(int window) {
        this.comboTimeWindow = window;
    }

    public void setShowDamage(boolean showDamage) {
        this.showDamage = showDamage;
    }

    public void setMaxDamageTexts(int maxDamageTexts) {
        this.maxDamageTexts = maxDamageTexts;
    }

    public void setRepulsionRadius(float repulsionRadius) {
        this.repulsionRadius = repulsionRadius;
    }

    public void setRepulsionStrength(float repulsionStrength) {
        this.repulsionStrength = repulsionStrength;
    }
}