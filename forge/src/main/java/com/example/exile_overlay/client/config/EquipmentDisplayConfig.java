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

    public enum QuickLootMode {
        LOOT,
        DROP
    }

    // 設定値
    private boolean usePercentage = false;
    private boolean enableShadow = true;
    private boolean quickLootEnabled = true;
    private boolean autoQuickLootEnabled = false;
    private QuickLootMode autoQuickLootMode = QuickLootMode.LOOT;
    private boolean keyQuickLootEnabled = true;
    private QuickLootMode keyQuickLootMode = QuickLootMode.DROP;
    private boolean disableMnsHpBar = false;
    private boolean cancelDungeonRealmScoreboard = true;

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

            if (obj.has("usePercentage")) {
                usePercentage = obj.get("usePercentage").getAsBoolean();
            }
            if (obj.has("enableShadow")) {
                enableShadow = obj.get("enableShadow").getAsBoolean();
            }
            if (obj.has("quickLootEnabled")) {
                quickLootEnabled = obj.get("quickLootEnabled").getAsBoolean();
            }
            if (obj.has("autoQuickLootEnabled")) {
                autoQuickLootEnabled = obj.get("autoQuickLootEnabled").getAsBoolean();
            }
            if (obj.has("autoQuickLootMode")) {
                try {
                    autoQuickLootMode = QuickLootMode.valueOf(obj.get("autoQuickLootMode").getAsString());
                } catch (IllegalArgumentException e) {
                    autoQuickLootMode = QuickLootMode.LOOT;
                }
            }
            if (obj.has("keyQuickLootEnabled")) {
                keyQuickLootEnabled = obj.get("keyQuickLootEnabled").getAsBoolean();
            }
            if (obj.has("keyQuickLootMode")) {
                try {
                    keyQuickLootMode = QuickLootMode.valueOf(obj.get("keyQuickLootMode").getAsString());
                } catch (IllegalArgumentException e) {
                    keyQuickLootMode = QuickLootMode.DROP;
                }
            }
            if (obj.has("disableMnsHpBar")) {
                disableMnsHpBar = obj.get("disableMnsHpBar").getAsBoolean();
            }
            if (obj.has("cancelDungeonRealmScoreboard")) {
                cancelDungeonRealmScoreboard = obj.get("cancelDungeonRealmScoreboard").getAsBoolean();
            }

            LOGGER.info("Loaded equipment display config: usePercentage={}, enableShadow={}, quickLootEnabled={}, autoQuickLootEnabled={}, autoQuickLootMode={}, keyQuickLootEnabled={}, keyQuickLootMode={}, disableMnsHpBar={}, cancelDungeonRealmScoreboard={}", usePercentage, enableShadow, quickLootEnabled, autoQuickLootEnabled, autoQuickLootMode, keyQuickLootEnabled, keyQuickLootMode, disableMnsHpBar, cancelDungeonRealmScoreboard);
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
        obj.addProperty("quickLootEnabled", quickLootEnabled);
        obj.addProperty("autoQuickLootEnabled", autoQuickLootEnabled);
        obj.addProperty("autoQuickLootMode", autoQuickLootMode.name());
        obj.addProperty("keyQuickLootEnabled", keyQuickLootEnabled);
        obj.addProperty("keyQuickLootMode", keyQuickLootMode.name());
        obj.addProperty("disableMnsHpBar", disableMnsHpBar);
        obj.addProperty("cancelDungeonRealmScoreboard", cancelDungeonRealmScoreboard);

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

    public boolean isQuickLootEnabled() {
        return quickLootEnabled;
    }

    public void setQuickLootEnabled(boolean enabled) {
        this.quickLootEnabled = enabled;
    }

    public boolean isAutoQuickLootEnabled() {
        return autoQuickLootEnabled;
    }

    public void setAutoQuickLootEnabled(boolean enabled) {
        this.autoQuickLootEnabled = enabled;
    }

    public QuickLootMode getAutoQuickLootMode() {
        return autoQuickLootMode;
    }

    public void setAutoQuickLootMode(QuickLootMode mode) {
        this.autoQuickLootMode = mode;
    }

    public boolean isKeyQuickLootEnabled() {
        return keyQuickLootEnabled;
    }

    public void setKeyQuickLootEnabled(boolean enabled) {
        this.keyQuickLootEnabled = enabled;
    }

    public QuickLootMode getKeyQuickLootMode() {
        return keyQuickLootMode;
    }

    public void setKeyQuickLootMode(QuickLootMode mode) {
        this.keyQuickLootMode = mode;
    }

    public boolean isDisableMnsHpBar() {
        return disableMnsHpBar;
    }

    public void setDisableMnsHpBar(boolean disable) {
        this.disableMnsHpBar = disable;
    }

    public boolean isCancelDungeonRealmScoreboard() {
        return cancelDungeonRealmScoreboard;
    }

    public void setCancelDungeonRealmScoreboard(boolean cancel) {
        this.cancelDungeonRealmScoreboard = cancel;
    }
}
