package com.example.exile_overlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * バフオーバーレイの表示内容フィルタ設定（両オーバーレイ共用）
 *
 * 【責任】
 * - buff_overlay / skill_buff_overlay それぞれの表示内容を個別に管理
 * - JSONファイルへの保存・読み込み
 */
public class BuffOverlayFilterConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/BuffOverlayFilterConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_buff_filter.json";
    private static BuffOverlayFilterConfig instance;

    private OverlayFilter buffOverlay = OverlayFilter.createDefaultAll();
    private OverlayFilter skillBuffOverlay = OverlayFilter.createDefaultSkillOnly();

    public static class OverlayFilter {
        public boolean showVanillaBuffs = true;
        public boolean showVanillaDebuffs = true;
        public boolean showMnsBuffs = true;
        public boolean showMnsDebuffs = true;
        public boolean showAura = true;
        public boolean showSelfSkill = true;
        public boolean showFood = true;
        public boolean showCharge = true;
        public boolean showSong = true;
        public boolean showGolem = true;
        public boolean showOther = true;

        public static OverlayFilter createDefaultAll() {
            return new OverlayFilter();
        }

        public static OverlayFilter createDefaultSkillOnly() {
            OverlayFilter f = new OverlayFilter();
            f.showVanillaBuffs = false;
            f.showVanillaDebuffs = false;
            f.showMnsBuffs = true;
            f.showMnsDebuffs = false;
            f.showFood = false;
            f.showOther = false;
            return f;
        }

        public boolean isShowVanillaBuffs() { return showVanillaBuffs; }
        public boolean isShowVanillaDebuffs() { return showVanillaDebuffs; }
        public boolean isShowMnsBuffs() { return showMnsBuffs; }
        public boolean isShowMnsDebuffs() { return showMnsDebuffs; }
        public boolean isShowAura() { return showAura; }
        public boolean isShowSelfSkill() { return showSelfSkill; }
        public boolean isShowFood() { return showFood; }
        public boolean isShowCharge() { return showCharge; }
        public boolean isShowSong() { return showSong; }
        public boolean isShowGolem() { return showGolem; }
        public boolean isShowOther() { return showOther; }

        public void setShowVanillaBuffs(boolean v) { showVanillaBuffs = v; }
        public void setShowVanillaDebuffs(boolean v) { showVanillaDebuffs = v; }
        public void setShowMnsBuffs(boolean v) { showMnsBuffs = v; }
        public void setShowMnsDebuffs(boolean v) { showMnsDebuffs = v; }
        public void setShowAura(boolean v) { showAura = v; }
        public void setShowSelfSkill(boolean v) { showSelfSkill = v; }
        public void setShowFood(boolean v) { showFood = v; }
        public void setShowCharge(boolean v) { showCharge = v; }
        public void setShowSong(boolean v) { showSong = v; }
        public void setShowGolem(boolean v) { showGolem = v; }
        public void setShowOther(boolean v) { showOther = v; }

        /**
         * 全タグフィルタがtrueか（＝フィルタリング不要）
         */
        public boolean isAllTagsEnabled() {
            return showAura && showSelfSkill && showFood && showCharge && showSong && showGolem && showOther;
        }
    }

    private BuffOverlayFilterConfig() {
    }

    public static BuffOverlayFilterConfig getInstance() {
        if (instance == null) {
            instance = new BuffOverlayFilterConfig();
            instance.load();
        }
        return instance;
    }

    public OverlayFilter getFilter(String overlayId) {
        if ("skill_buff_overlay".equals(overlayId)) {
            return skillBuffOverlay;
        }
        return buffOverlay;
    }

    private Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("exile_overlay")
                .resolve(CONFIG_FILE_NAME);
    }

    public void load() {
        try {
            Path path = getConfigPath();
            if (Files.exists(path)) {
                String json = Files.readString(path);
                BuffOverlayFilterConfig loaded = GSON.fromJson(json, BuffOverlayFilterConfig.class);
                if (loaded != null) {
                    if (loaded.buffOverlay != null) this.buffOverlay = loaded.buffOverlay;
                    if (loaded.skillBuffOverlay != null) this.skillBuffOverlay = loaded.skillBuffOverlay;
                }
                LOGGER.info("Loaded buff overlay filter config");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load buff overlay filter config: {}", e.getMessage());
        }
    }

    public void save() {
        try {
            Path path = getConfigPath();
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, GSON.toJson(this));
            LOGGER.info("Saved buff overlay filter config");
        } catch (Exception e) {
            LOGGER.error("Failed to save buff overlay filter config: {}", e.getMessage());
        }
    }

    public OverlayFilter getBuffOverlay() { return buffOverlay; }
    public OverlayFilter getSkillBuffOverlay() { return skillBuffOverlay; }
}
