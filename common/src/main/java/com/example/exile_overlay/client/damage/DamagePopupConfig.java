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
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_damage_popup.json";
    private static DamagePopupConfig instance;

    private float baseScale = 0.08f;
    private float criticalScale = 0.12f;
    private int displayDuration = 60;
    private boolean enableShadow = true;
    private float horizontalSpread = 0.5f;
    private int fadeInDuration = 5;
    private int fadeOutDuration = 15;
    private boolean showDamage = true;
    private int maxDamageTexts = 20;
    private float repulsionRadius = 0.5f;
    private float repulsionStrength = 0.05f;
    private float verticalSpeed = 0.05f;
    private boolean showPlayerDamage = false;
    private boolean showHealing = true;
    
    // HJUD機能: ダメージスタッキング設定
    private boolean enableDamageStacking = true;
    private float stackingRadius = 0.8f;
    
    // フォントプリセット設定
    private FontPreset fontPreset = FontPreset.JERSEY20;
    private int customFontSize = 64;

    private int normalDamageColor = 0xFFFFFF;
    private int criticalDamageColor = 0xFFFF00;
    private int healingColor = 0x00FF00;
    private int fireDamageColor = 0xFF6600;
    private int iceDamageColor = 0x00CCFF;
    private int lightningDamageColor = 0xFFFF66;
    private int poisonDamageColor = 0x00FF00;
    private int magicDamageColor = 0x800080;
    private int witherDamageColor = 0x2F2F2F;

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

            if (obj.has("baseScale")) baseScale = obj.get("baseScale").getAsFloat();
            if (obj.has("criticalScale")) criticalScale = obj.get("criticalScale").getAsFloat();
            if (obj.has("displayDuration")) displayDuration = obj.get("displayDuration").getAsInt();
            if (obj.has("enableShadow")) enableShadow = obj.get("enableShadow").getAsBoolean();
            if (obj.has("horizontalSpread")) horizontalSpread = obj.get("horizontalSpread").getAsFloat();
            if (obj.has("fadeInDuration")) fadeInDuration = obj.get("fadeInDuration").getAsInt();
            if (obj.has("fadeOutDuration")) fadeOutDuration = obj.get("fadeOutDuration").getAsInt();
            if (obj.has("showDamage")) showDamage = obj.get("showDamage").getAsBoolean();
            if (obj.has("maxDamageTexts")) maxDamageTexts = obj.get("maxDamageTexts").getAsInt();
            if (obj.has("repulsionRadius")) repulsionRadius = obj.get("repulsionRadius").getAsFloat();
            if (obj.has("repulsionStrength")) repulsionStrength = obj.get("repulsionStrength").getAsFloat();
            if (obj.has("verticalSpeed")) verticalSpeed = obj.get("verticalSpeed").getAsFloat();
            if (obj.has("showPlayerDamage")) showPlayerDamage = obj.get("showPlayerDamage").getAsBoolean();
            if (obj.has("showHealing")) showHealing = obj.get("showHealing").getAsBoolean();
            
            // HJUD機能: ダメージスタッキング設定の読み込み
            if (obj.has("enableDamageStacking")) enableDamageStacking = obj.get("enableDamageStacking").getAsBoolean();
            if (obj.has("stackingRadius")) stackingRadius = obj.get("stackingRadius").getAsFloat();
            
            // フォントプリセット設定の読み込み
            if (obj.has("fontPreset")) {
                fontPreset = FontPreset.fromName(obj.get("fontPreset").getAsString());
            } else if (obj.has("customFontPath")) {
                fontPreset = FontPreset.fromPath(obj.get("customFontPath").getAsString());
            }
            if (obj.has("customFontSize")) customFontSize = obj.get("customFontSize").getAsInt();

            if (obj.has("colors")) {
                JsonObject colors = obj.getAsJsonObject("colors");
                if (colors.has("normal")) normalDamageColor = colors.get("normal").getAsInt();
                if (colors.has("critical")) criticalDamageColor = colors.get("critical").getAsInt();
                if (colors.has("healing")) healingColor = colors.get("healing").getAsInt();
                if (colors.has("fire")) fireDamageColor = colors.get("fire").getAsInt();
                if (colors.has("ice")) iceDamageColor = colors.get("ice").getAsInt();
                if (colors.has("lightning")) lightningDamageColor = colors.get("lightning").getAsInt();
                if (colors.has("poison")) poisonDamageColor = colors.get("poison").getAsInt();
                if (colors.has("magic")) magicDamageColor = colors.get("magic").getAsInt();
                if (colors.has("wither")) witherDamageColor = colors.get("wither").getAsInt();
            }

            LOGGER.info("Loaded damage popup config");
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

        JsonObject obj = new JsonObject();
        obj.addProperty("baseScale", baseScale);
        obj.addProperty("criticalScale", criticalScale);
        obj.addProperty("displayDuration", displayDuration);
        obj.addProperty("enableShadow", enableShadow);
        obj.addProperty("horizontalSpread", horizontalSpread);
        obj.addProperty("fadeInDuration", fadeInDuration);
        obj.addProperty("fadeOutDuration", fadeOutDuration);
        obj.addProperty("showDamage", showDamage);
        obj.addProperty("maxDamageTexts", maxDamageTexts);
        obj.addProperty("repulsionRadius", repulsionRadius);
        obj.addProperty("repulsionStrength", repulsionStrength);
        obj.addProperty("verticalSpeed", verticalSpeed);
        obj.addProperty("showPlayerDamage", showPlayerDamage);
        obj.addProperty("showHealing", showHealing);
        
        // HJUD機能: ダメージスタッキング設定の保存
        obj.addProperty("enableDamageStacking", enableDamageStacking);
        obj.addProperty("stackingRadius", stackingRadius);
        
        // フォントプリセット設定の保存
        obj.addProperty("fontPreset", fontPreset.name());
        obj.addProperty("customFontSize", customFontSize);

        JsonObject colors = new JsonObject();
        colors.addProperty("normal", normalDamageColor);
        colors.addProperty("critical", criticalDamageColor);
        colors.addProperty("healing", healingColor);
        colors.addProperty("fire", fireDamageColor);
        colors.addProperty("ice", iceDamageColor);
        colors.addProperty("lightning", lightningDamageColor);
        colors.addProperty("poison", poisonDamageColor);
        colors.addProperty("magic", magicDamageColor);
        colors.addProperty("wither", witherDamageColor);
        obj.add("colors", colors);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved damage popup config");
        } catch (IOException e) {
            LOGGER.error("Failed to save damage popup config: {}", e.getMessage());
        }
    }

    public float getBaseScale() { return baseScale; }
    public float getCriticalScale() { return criticalScale; }
    public int getDisplayDuration() { return displayDuration; }
    public boolean isEnableShadow() { return enableShadow; }
    public float getHorizontalSpread() { return horizontalSpread; }
    public int getFadeInDuration() { return fadeInDuration; }
    public int getFadeOutDuration() { return fadeOutDuration; }
    public boolean isShowDamage() { return showDamage; }
    public int getMaxDamageTexts() { return maxDamageTexts; }
    public float getRepulsionRadius() { return repulsionRadius; }
    public float getRepulsionStrength() { return repulsionStrength; }
    public float getVerticalSpeed() { return verticalSpeed; }
    public boolean isShowPlayerDamage() { return showPlayerDamage; }
    public boolean isShowHealing() { return showHealing; }
    
    // HJUD機能: ダメージスタッキング設定のゲッター
    public boolean isEnableDamageStacking() { return enableDamageStacking; }
    public float getStackingRadius() { return stackingRadius; }
    
    // フォントプリセット設定のゲッター
    public FontPreset getFontPreset() { return fontPreset; }
    public boolean isUseCustomFont() { return fontPreset.isCustomFont(); }
    public String getCustomFontPath() { return fontPreset.getResourcePath(); }
    public int getCustomFontSize() { return customFontSize; }

    public int getNormalDamageColor() { return normalDamageColor; }
    public int getCriticalDamageColor() { return criticalDamageColor; }
    public int getHealingColor() { return healingColor; }
    public int getFireDamageColor() { return fireDamageColor; }
    public int getIceDamageColor() { return iceDamageColor; }
    public int getLightningDamageColor() { return lightningDamageColor; }
    public int getPoisonDamageColor() { return poisonDamageColor; }
    public int getMagicDamageColor() { return magicDamageColor; }
    public int getWitherDamageColor() { return witherDamageColor; }

    public int getColorForType(DamageType type) {
        return switch (type) {
            case FIRE -> fireDamageColor;
            case ICE -> iceDamageColor;
            case LIGHTNING -> lightningDamageColor;
            case POISON -> poisonDamageColor;
            case MAGIC -> magicDamageColor;
            case WITHER -> witherDamageColor;
            case HEALING -> healingColor;
            default -> normalDamageColor;
        };
    }

    public void setShowDamage(boolean show) { this.showDamage = show; }
    public void setShowHealing(boolean show) { this.showHealing = show; }
    public void setShowPlayerDamage(boolean show) { this.showPlayerDamage = show; }
    public void setEnableShadow(boolean enable) { this.enableShadow = enable; }
    public void setBaseScale(float scale) { this.baseScale = scale; }
    public void setCriticalScale(float scale) { this.criticalScale = scale; }
    public void setDisplayDuration(int duration) { this.displayDuration = duration; }
    public void setFadeInDuration(int duration) { this.fadeInDuration = duration; }
    public void setFadeOutDuration(int duration) { this.fadeOutDuration = duration; }
    public void setMaxDamageTexts(int max) { this.maxDamageTexts = max; }

    // HJUD機能: ダメージスタッキング設定のセッター
    public void setEnableDamageStacking(boolean enable) { this.enableDamageStacking = enable; }
    public void setStackingRadius(float radius) { this.stackingRadius = radius; }

    // フォントプリセット設定のセッター
    public void setFontPreset(FontPreset preset) { this.fontPreset = preset; }
    public void setCustomFontSize(int size) { this.customFontSize = size; }

    // 色設定のセッター
    public void setNormalDamageColor(int color) { this.normalDamageColor = color; }
    public void setCriticalDamageColor(int color) { this.criticalDamageColor = color; }
    public void setHealingColor(int color) { this.healingColor = color; }
    public void setFireDamageColor(int color) { this.fireDamageColor = color; }
    public void setIceDamageColor(int color) { this.iceDamageColor = color; }
    public void setLightningDamageColor(int color) { this.lightningDamageColor = color; }
    public void setPoisonDamageColor(int color) { this.poisonDamageColor = color; }
    public void setMagicDamageColor(int color) { this.magicDamageColor = color; }
    public void setWitherDamageColor(int color) { this.witherDamageColor = color; }
}
