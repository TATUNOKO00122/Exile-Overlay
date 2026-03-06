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
 * 装備表示設定を管理する設定クラス
 * 
 * 【責任】
 * - アイコンサイズ設定の管理
 * - パーセント表示設定の管理
 * - JSONファイルへの保存・読み込み
 */
public class EquipmentDisplayConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EquipmentDisplayConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_equipment_display.json";
    private static EquipmentDisplayConfig instance;

    // 設定値
    private boolean usePercentage = false;  // デフォルトfalse（実数値表示）
    private boolean enableShadow = true;    // デフォルトtrue（影有効）

    private EquipmentDisplayConfig() {
    }

    public static EquipmentDisplayConfig getInstance() {
        if (instance == null) {
            instance = new EquipmentDisplayConfig();
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

            if (obj.has("usePercentage")) {
                usePercentage = obj.get("usePercentage").getAsBoolean();
            }
            if (obj.has("enableShadow")) {
                enableShadow = obj.get("enableShadow").getAsBoolean();
            }

            LOGGER.info("Loaded equipment display config: usePercentage={}, enableShadow={}", usePercentage, enableShadow);
        } catch (IOException e) {
            LOGGER.error("Failed to load equipment display config: {}", e.getMessage());
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
        obj.addProperty("usePercentage", usePercentage);
        obj.addProperty("enableShadow", enableShadow);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved equipment display config");
        } catch (IOException e) {
            LOGGER.error("Failed to save equipment display config: {}", e.getMessage());
        }
    }

    // Getter
    public boolean isUsePercentage() {
        return usePercentage;
    }

    public boolean isEnableShadow() {
        return enableShadow;
    }

    // Setter
    public void setUsePercentage(boolean use) {
        this.usePercentage = use;
    }

    public void setEnableShadow(boolean enable) {
        this.enableShadow = enable;
    }
}
