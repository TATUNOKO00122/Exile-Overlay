package com.example.exile_overlay.client.damage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DamagePopupConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_damage_popup.json";
    private static volatile DamagePopupConfig instance;
    private static final Object LOCK = new Object();

    private float baseScale = 0.03f;
    private float criticalScale = 0.04f;
    private int displayDuration = 30;
    private boolean enableShadow = true;
    private float horizontalSpread = 0.5f;
    private int fadeInDuration = 5;
    private int fadeOutDuration = 10;
    private boolean showDamage = true;
    private int maxDamageTexts = 20;
    private float popupHeightRatio = 0.8f;
    private boolean showPlayerDamage = false;
    private boolean showHealing = true;
    private boolean showPlayerHealing = true;
    private boolean roundDamageNumbers = true;
    private boolean compactNumbers = true;

    private FontPreset fontPreset = FontPreset.LINESEED;

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
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DamagePopupConfig();
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

            if (obj.has("baseScale")) baseScale = obj.get("baseScale").getAsFloat();
            if (obj.has("criticalScale")) criticalScale = obj.get("criticalScale").getAsFloat();
            if (obj.has("displayDuration")) displayDuration = obj.get("displayDuration").getAsInt();
            if (obj.has("enableShadow")) enableShadow = obj.get("enableShadow").getAsBoolean();
            if (obj.has("horizontalSpread")) horizontalSpread = obj.get("horizontalSpread").getAsFloat();
            if (obj.has("fadeInDuration")) fadeInDuration = obj.get("fadeInDuration").getAsInt();
            if (obj.has("fadeOutDuration")) fadeOutDuration = obj.get("fadeOutDuration").getAsInt();
            if (obj.has("showDamage")) showDamage = obj.get("showDamage").getAsBoolean();
            if (obj.has("maxDamageTexts")) maxDamageTexts = obj.get("maxDamageTexts").getAsInt();
            if (obj.has("popupHeightRatio")) popupHeightRatio = obj.get("popupHeightRatio").getAsFloat();
            if (obj.has("showPlayerDamage")) showPlayerDamage = obj.get("showPlayerDamage").getAsBoolean();
            if (obj.has("showHealing")) showHealing = obj.get("showHealing").getAsBoolean();
            if (obj.has("showPlayerHealing")) showPlayerHealing = obj.get("showPlayerHealing").getAsBoolean();
            if (obj.has("roundDamageNumbers")) roundDamageNumbers = obj.get("roundDamageNumbers").getAsBoolean();
            if (obj.has("compactNumbers")) compactNumbers = obj.get("compactNumbers").getAsBoolean();

            if (obj.has("fontPreset")) {
                fontPreset = FontPreset.fromName(obj.get("fontPreset").getAsString());
            } else if (obj.has("customFontPath")) {
                fontPreset = FontPreset.fromName(obj.get("customFontPath").getAsString());
            }

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
        obj.addProperty("popupHeightRatio", popupHeightRatio);
        obj.addProperty("showPlayerDamage", showPlayerDamage);
        obj.addProperty("showHealing", showHealing);
        obj.addProperty("showPlayerHealing", showPlayerHealing);
        obj.addProperty("roundDamageNumbers", roundDamageNumbers);
        obj.addProperty("compactNumbers", compactNumbers);

        obj.addProperty("fontPreset", fontPreset.name());

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
    public float getPopupHeightRatio() { return popupHeightRatio; }
    public boolean isShowPlayerDamage() { return showPlayerDamage; }
    public boolean isShowHealing() { return showHealing; }
    public boolean isShowPlayerHealing() { return showPlayerHealing; }
    public boolean isRoundDamageNumbers() { return roundDamageNumbers; }
    public boolean isCompactNumbers() { return compactNumbers; }

    public FontPreset getFontPreset() { return fontPreset; }

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
    public void setShowPlayerHealing(boolean show) { this.showPlayerHealing = show; }
    public void setRoundDamageNumbers(boolean round) { this.roundDamageNumbers = round; }
    public void setCompactNumbers(boolean compact) { this.compactNumbers = compact; }
    public void setEnableShadow(boolean enable) { this.enableShadow = enable; }
    public void setBaseScale(float scale) { this.baseScale = scale; }
    public void setCriticalScale(float scale) { this.criticalScale = scale; }
    public void setDisplayDuration(int duration) { this.displayDuration = duration; }
    public void setFadeInDuration(int duration) { this.fadeInDuration = duration; }
    public void setFadeOutDuration(int duration) { this.fadeOutDuration = duration; }
    public void setMaxDamageTexts(int max) { this.maxDamageTexts = max; }
    public void setPopupHeightRatio(float ratio) { this.popupHeightRatio = ratio; }

    public void setFontPreset(FontPreset preset) { this.fontPreset = preset; }

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