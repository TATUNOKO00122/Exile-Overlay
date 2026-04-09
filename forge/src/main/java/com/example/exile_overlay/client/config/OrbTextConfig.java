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
 * オーブテキスト表示設定を管理する設定クラス
 *
 * 【責任】
 * - オーブテキスト表示ON/OFF
 * - POE2スタイル（Orb上表示）ON/OFF
 * - JSONファイルへの保存・読み込み
 */
public class OrbTextConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/OrbTextConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE_NAME = "exile_overlay_orb_text.json";
    private static OrbTextConfig instance;

    private boolean showOrbText = true;
    private boolean poe2StyleText = false;

    private OrbTextConfig() {
    }

    public static OrbTextConfig getInstance() {
        if (instance == null) {
            instance = new OrbTextConfig();
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

            if (obj.has("showOrbText")) {
                showOrbText = obj.get("showOrbText").getAsBoolean();
            }
            if (obj.has("poe2StyleText")) {
                poe2StyleText = obj.get("poe2StyleText").getAsBoolean();
            }

            LOGGER.info("Loaded orb text config: showOrbText={}, poe2StyleText={}", showOrbText, poe2StyleText);
        } catch (IOException e) {
            LOGGER.error("Failed to load orb text config: {}", e.getMessage());
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
        obj.addProperty("showOrbText", showOrbText);
        obj.addProperty("poe2StyleText", poe2StyleText);

        try {
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved orb text config");
        } catch (IOException e) {
            LOGGER.error("Failed to save orb text config: {}", e.getMessage());
        }
    }

    public boolean isShowOrbText() {
        return showOrbText;
    }

    public void setShowOrbText(boolean show) {
        this.showOrbText = show;
    }

    public boolean isPoe2StyleText() {
        return poe2StyleText;
    }

    public void setPoe2StyleText(boolean poe2Style) {
        this.poe2StyleText = poe2Style;
    }
}
