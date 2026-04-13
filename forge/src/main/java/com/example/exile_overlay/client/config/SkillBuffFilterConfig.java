package com.example.exile_overlay.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * スキルバフオーバーレイのフィルタ設定
 *
 * 【責任】
 * - スキルバフ表示の各カテゴリON/OFF管理
 * - JSONファイルへの保存・読み込み
 */
public class SkillBuffFilterConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/SkillBuffFilterConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_skill_buff_filter.json";
    private static SkillBuffFilterConfig instance;

    private boolean showAura = true;
    private boolean showSelfSkill = true;
    private boolean showFood = false;
    private boolean showCharge = true;
    private boolean showSong = true;
    private boolean showGolem = true;
    private boolean showOther = false;

    private SkillBuffFilterConfig() {
    }

    public static SkillBuffFilterConfig getInstance() {
        if (instance == null) {
            instance = new SkillBuffFilterConfig();
            instance.load();
        }
        return instance;
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
                SkillBuffFilterConfig loaded = GSON.fromJson(json, SkillBuffFilterConfig.class);
                if (loaded != null) {
                    this.showAura = loaded.showAura;
                    this.showSelfSkill = loaded.showSelfSkill;
                    this.showFood = loaded.showFood;
                    this.showCharge = loaded.showCharge;
                    this.showSong = loaded.showSong;
                    this.showGolem = loaded.showGolem;
                    this.showOther = loaded.showOther;
                }
                LOGGER.info("Loaded skill buff filter config");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load skill buff filter config: {}", e.getMessage());
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
            LOGGER.info("Saved skill buff filter config");
        } catch (Exception e) {
            LOGGER.error("Failed to save skill buff filter config: {}", e.getMessage());
        }
    }

    public boolean isShowAura() { return showAura; }
    public boolean isShowSelfSkill() { return showSelfSkill; }
    public boolean isShowFood() { return showFood; }
    public boolean isShowCharge() { return showCharge; }
    public boolean isShowSong() { return showSong; }
    public boolean isShowGolem() { return showGolem; }
    public boolean isShowOther() { return showOther; }

    public void setShowAura(boolean v) { showAura = v; }
    public void setShowSelfSkill(boolean v) { showSelfSkill = v; }
    public void setShowFood(boolean v) { showFood = v; }
    public void setShowCharge(boolean v) { showCharge = v; }
    public void setShowSong(boolean v) { showSong = v; }
    public void setShowGolem(boolean v) { showGolem = v; }
    public void setShowOther(boolean v) { showOther = v; }
}
