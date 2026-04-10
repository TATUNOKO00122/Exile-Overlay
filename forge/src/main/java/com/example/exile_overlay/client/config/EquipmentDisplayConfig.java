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

    /**
     * レベル数値の表示モード
     *
     * 【設計思想】
     * - BOTH: バニラ(緑) / MS(黄) の両方を表示
     * - MS_ONLY: MS Levelのみ(黄)表示
     * - VANILLA_ONLY: バニラLevelのみ(緑)表示
     */
    public enum LevelDisplayMode {
        BOTH,
        MS_ONLY,
        VANILLA_ONLY
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
    private boolean cancelMnsRpgBars = true;
    private boolean cancelMnsSpellHotbar = true;
    private boolean cancelMnsCastBar = true;
    private boolean cancelMnsStatusEffects = true;
    private boolean cancelDungeonRealmScoreboard = true;
    private boolean autoSortLootrChest = true;
    private LevelDisplayMode levelDisplayMode = LevelDisplayMode.BOTH;
    private boolean showTargetAffixStats = true;
    private boolean showTargetMobEffects = true;

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
            if (obj.has("cancelMnsRpgBars")) {
                cancelMnsRpgBars = obj.get("cancelMnsRpgBars").getAsBoolean();
            }
            if (obj.has("cancelMnsSpellHotbar")) {
                cancelMnsSpellHotbar = obj.get("cancelMnsSpellHotbar").getAsBoolean();
            }
            if (obj.has("cancelMnsCastBar")) {
                cancelMnsCastBar = obj.get("cancelMnsCastBar").getAsBoolean();
            }
            if (obj.has("cancelMnsStatusEffects")) {
                cancelMnsStatusEffects = obj.get("cancelMnsStatusEffects").getAsBoolean();
            }
            if (obj.has("cancelDungeonRealmScoreboard")) {
                cancelDungeonRealmScoreboard = obj.get("cancelDungeonRealmScoreboard").getAsBoolean();
            }
            if (obj.has("autoSortLootrChest")) {
                autoSortLootrChest = obj.get("autoSortLootrChest").getAsBoolean();
            }
            if (obj.has("levelDisplayMode")) {
                try {
                    levelDisplayMode = LevelDisplayMode.valueOf(obj.get("levelDisplayMode").getAsString());
                } catch (IllegalArgumentException e) {
                    levelDisplayMode = LevelDisplayMode.BOTH;
                }
            }
            if (obj.has("showTargetAffixStats")) {
                showTargetAffixStats = obj.get("showTargetAffixStats").getAsBoolean();
            }
            if (obj.has("showTargetMobEffects")) {
                showTargetMobEffects = obj.get("showTargetMobEffects").getAsBoolean();
            }

            LOGGER.info("Loaded equipment display config: usePercentage={}, enableShadow={}, quickLootEnabled={}, autoQuickLootEnabled={}, autoQuickLootMode={}, keyQuickLootEnabled={}, keyQuickLootMode={}, disableMnsHpBar={}, cancelMnsRpgBars={}, cancelMnsSpellHotbar={}, cancelMnsCastBar={}, cancelMnsStatusEffects={}, cancelDungeonRealmScoreboard={}, autoSortLootrChest={}, showTargetAffixStats={}, showTargetMobEffects={}", usePercentage, enableShadow, quickLootEnabled, autoQuickLootEnabled, autoQuickLootMode, keyQuickLootEnabled, keyQuickLootMode, disableMnsHpBar, cancelMnsRpgBars, cancelMnsSpellHotbar, cancelMnsCastBar, cancelMnsStatusEffects, cancelDungeonRealmScoreboard, autoSortLootrChest, showTargetAffixStats, showTargetMobEffects);
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
        obj.addProperty("cancelMnsRpgBars", cancelMnsRpgBars);
        obj.addProperty("cancelMnsSpellHotbar", cancelMnsSpellHotbar);
        obj.addProperty("cancelMnsCastBar", cancelMnsCastBar);
        obj.addProperty("cancelMnsStatusEffects", cancelMnsStatusEffects);
        obj.addProperty("cancelDungeonRealmScoreboard", cancelDungeonRealmScoreboard);
        obj.addProperty("autoSortLootrChest", autoSortLootrChest);
        obj.addProperty("levelDisplayMode", levelDisplayMode.name());
        obj.addProperty("showTargetAffixStats", showTargetAffixStats);
        obj.addProperty("showTargetMobEffects", showTargetMobEffects);

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

    public boolean isCancelMnsRpgBars() {
        return cancelMnsRpgBars;
    }

    public void setCancelMnsRpgBars(boolean cancel) {
        this.cancelMnsRpgBars = cancel;
    }

    public boolean isCancelMnsSpellHotbar() {
        return cancelMnsSpellHotbar;
    }

    public void setCancelMnsSpellHotbar(boolean cancel) {
        this.cancelMnsSpellHotbar = cancel;
    }

    public boolean isCancelMnsCastBar() {
        return cancelMnsCastBar;
    }

    public void setCancelMnsCastBar(boolean cancel) {
        this.cancelMnsCastBar = cancel;
    }

    public boolean isCancelMnsStatusEffects() {
        return cancelMnsStatusEffects;
    }

    public void setCancelMnsStatusEffects(boolean cancel) {
        this.cancelMnsStatusEffects = cancel;
    }

    public boolean isCancelDungeonRealmScoreboard() {
        return cancelDungeonRealmScoreboard;
    }

    public void setCancelDungeonRealmScoreboard(boolean cancel) {
        this.cancelDungeonRealmScoreboard = cancel;
    }

    public boolean isAutoSortLootrChest() {
        return autoSortLootrChest;
    }

    public void setAutoSortLootrChest(boolean enabled) {
        this.autoSortLootrChest = enabled;
    }

    public LevelDisplayMode getLevelDisplayMode() {
        return levelDisplayMode;
    }

    public void setLevelDisplayMode(LevelDisplayMode mode) {
        this.levelDisplayMode = mode;
    }

    public boolean isShowTargetAffixStats() {
        return showTargetAffixStats;
    }

    public void setShowTargetAffixStats(boolean show) {
        this.showTargetAffixStats = show;
    }

    public boolean isShowTargetMobEffects() {
        return showTargetMobEffects;
    }

    public void setShowTargetMobEffects(boolean show) {
        this.showTargetMobEffects = show;
    }
}
