package com.example.exile_overlay.client.config;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.DayCounterConfig;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
// import com.example.exile_overlay.client.render.kill.KillCounterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 全設定セクションを一括管理するマネージャー。
 * ConfigScreen等で {@code ExileOverlayConfigManager.getInstance().saveAll()} を呼び出す。
 * プリセット適用前の状態のバックアップと復元（Undo）も対応。
 */
public final class ExileOverlayConfigManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ConfigManager");
    private static volatile ExileOverlayConfigManager instance;
    private static final Object LOCK = new Object();

    private PresetBackupSnapshot backupSnapshot = null;

    public static class PresetBackupSnapshot {
        public boolean cancelMnsRpgBars;
        public boolean cancelMnsSpellHotbar;
        public boolean cancelMnsCastBar;
        public boolean cancelMnsStatusEffects;
        public boolean cancelMnsExpActionBar;
        public boolean cancelDungeonRealmScoreboard;
        public Map<String, Boolean> hudVisibilityMap = new HashMap<>();
        public boolean trackerEnabled;
    }

    private ExileOverlayConfigManager() {
    }

    public static ExileOverlayConfigManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ExileOverlayConfigManager();
                }
            }
        }
        return instance;
    }

    public boolean hasBackupSnapshot() {
        return backupSnapshot != null;
    }

    public void clearBackupSnapshot() {
        if (this.backupSnapshot != null) {
            this.backupSnapshot = null;
            LOGGER.debug("Cleared preset backup snapshot due to config changes");
        }
    }

    public void createBackupSnapshot() {
        PresetBackupSnapshot snap = new PresetBackupSnapshot();
        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();
        snap.cancelMnsRpgBars = equipConfig.isCancelMnsRpgBars();
        snap.cancelMnsSpellHotbar = equipConfig.isCancelMnsSpellHotbar();
        snap.cancelMnsCastBar = equipConfig.isCancelMnsCastBar();
        snap.cancelMnsStatusEffects = equipConfig.isCancelMnsStatusEffects();
        snap.cancelMnsExpActionBar = equipConfig.isCancelMnsExpActionBar();
        snap.cancelDungeonRealmScoreboard = equipConfig.isCancelDungeonRealmScoreboard();

        HudPositionManager posMgr = HudPositionManager.getInstance();
        for (String key : posMgr.getAllKeys()) {
            snap.hudVisibilityMap.put(key, posMgr.getPosition(key).isVisible());
        }

        snap.trackerEnabled = TrackerConfig.getInstance().isEnabled();
        this.backupSnapshot = snap;
        LOGGER.info("Created preset backup snapshot for undo operation");
    }

    public void restoreBackupSnapshot() {
        if (backupSnapshot == null) {
            return;
        }

        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();
        equipConfig.setCancelMnsRpgBars(backupSnapshot.cancelMnsRpgBars);
        equipConfig.setCancelMnsSpellHotbar(backupSnapshot.cancelMnsSpellHotbar);
        equipConfig.setCancelMnsCastBar(backupSnapshot.cancelMnsCastBar);
        equipConfig.setCancelMnsStatusEffects(backupSnapshot.cancelMnsStatusEffects);
        equipConfig.setCancelMnsExpActionBar(backupSnapshot.cancelMnsExpActionBar);
        equipConfig.setCancelDungeonRealmScoreboard(backupSnapshot.cancelDungeonRealmScoreboard);
        equipConfig.save();

        HudPositionManager posMgr = HudPositionManager.getInstance();
        for (Map.Entry<String, Boolean> entry : backupSnapshot.hudVisibilityMap.entrySet()) {
            String key = entry.getKey();
            boolean vis = entry.getValue();
            HudPosition pos = posMgr.getPosition(key);
            if (pos.isVisible() != vis) {
                posMgr.setPosition(key, pos.withVisible(vis));
            }
        }
        posMgr.saveToFile();

        TrackerConfig trackerConfig = TrackerConfig.getInstance();
        trackerConfig.setEnabled(backupSnapshot.trackerEnabled);
        trackerConfig.save();

        saveAll();
        this.backupSnapshot = null;
        LOGGER.info("Restored settings from preset backup snapshot");
    }

    /**
     * 全設定セクションをファイルに保存する
     * 各セクションの保存エラーは独立して処理され、他のセクションの保存を妨げない
     */
    public void saveAll() {
        saveSection(EquipmentDisplayConfig.getInstance());
        saveSection(DamagePopupConfig.getInstance());
        saveSection(EntityHealthBarConfig.getInstance());
        saveSection(OrbTextConfig.getInstance());
        saveSection(BuffOverlayFilterConfig.getInstance());
        saveSection(SkillBuffFilterConfig.getInstance());
        saveSection(HudFontConfig.getInstance());
        saveSection(OrbColorConfig.getInstance());
        saveSection(OrbSmoothConfig.getInstance());
        saveSection(DayCounterConfig.getInstance());
        // saveSection(KillCounterConfig.getInstance());
        saveSection(HudPositionManager.getInstance());
        saveSection(DropSoundConfig.getInstance());
    }

    private void saveSection(IConfigSection section) {
        try {
            section.save();
        } catch (Exception e) {
            LOGGER.error("Failed to save config section '{}': {}", section.getSectionId(), e.getMessage());
        }
    }
}
