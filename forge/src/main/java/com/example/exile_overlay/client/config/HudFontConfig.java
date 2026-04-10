package com.example.exile_overlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * HUDフォント設定を管理する設定クラス
 * 
 * 【責任】
 * - カスタムフォント使用設定の管理
 * - JSONファイルへの保存・読み込み
 */
public class HudFontConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/HudFontConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_hud_font.json";
    private static HudFontConfig instance;

    private boolean useCustomFont = false;
    private HudFontPreset fontPreset = HudFontPreset.GOOGLE_SANS;

    private HudFontConfig() {
    }

    public static HudFontConfig getInstance() {
        if (instance == null) {
            instance = new HudFontConfig();
            instance.load();
        }
        return instance;
    }

    private Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(CONFIG_FILE_NAME);
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

            if (obj.has("useCustomFont")) {
                useCustomFont = obj.get("useCustomFont").getAsBoolean();
            }
            if (obj.has("fontPreset")) {
                fontPreset = HudFontPreset.fromName(obj.get("fontPreset").getAsString());
            } else if (obj.has("useCustomFont") && !useCustomFont) {
                fontPreset = HudFontPreset.MINECRAFT;
            }

            LOGGER.info("Loaded HUD font config: useCustomFont={}, fontPreset={}", useCustomFont, fontPreset.name());
        } catch (IOException e) {
            LOGGER.error("Failed to load HUD font config: {}", e.getMessage());
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
        obj.addProperty("useCustomFont", useCustomFont);
        obj.addProperty("fontPreset", fontPreset.name());

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved HUD font config");
        } catch (IOException e) {
            LOGGER.error("Failed to save HUD font config: {}", e.getMessage());
        }
    }

    public boolean isUseCustomFont() {
        return useCustomFont;
    }

    public void setUseCustomFont(boolean use) {
        this.useCustomFont = use;
    }

    public HudFontPreset getFontPreset() {
        return fontPreset;
    }

    public void setFontPreset(HudFontPreset preset) {
        this.fontPreset = preset;
        this.useCustomFont = preset.isCustomFont();
    }
}