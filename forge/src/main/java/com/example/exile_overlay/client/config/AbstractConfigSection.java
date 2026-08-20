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
 * 設定セクションの抽象基底クラス。
 * Gson/ロガー/設定パス解決を共通化し、load/saveのテンプレートメソッドを提供する。
 * レガシーパス（config/直下）からの自動マイグレーションも対応。
 * サブクラスは {@link #deserialize(JsonObject)} と {@link #serialize(JsonObject)} を実装する。
 */
public abstract class AbstractConfigSection implements IConfigSection {

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/Config");

    private static final String CONFIG_DIR = "exile_overlay";

    private final String sectionId;
    private final String fileName;
    private final boolean legacyRootDir;

    protected AbstractConfigSection(String sectionId, String fileName) {
        this(sectionId, fileName, false);
    }

    protected AbstractConfigSection(String sectionId, String fileName, boolean legacyRootDir) {
        this.sectionId = sectionId;
        this.fileName = fileName;
        this.legacyRootDir = legacyRootDir;
    }

    @Override
    public final String getSectionId() {
        return sectionId;
    }

    /**
     * 設定ファイルの統一パスを返す（config/exile_overlay/fileName）
     */
    protected Path getConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(CONFIG_DIR)
                .resolve(fileName);
    }

    /**
     * レガシーパス（config/fileName）を返す
     * 旧バージョンからの移行用
     */
    private Path getLegacyConfigPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve(fileName);
    }

    @Override
    public void load() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath) && legacyRootDir) {
            Path legacyPath = getLegacyConfigPath();
            if (Files.exists(legacyPath)) {
                try {
                    Files.createDirectories(configPath.getParent());
                    Files.copy(legacyPath, configPath);
                    LOGGER.info("Migrated config '{}' from {} to {}", sectionId, legacyPath, configPath);
                } catch (IOException e) {
                    LOGGER.warn("Failed to migrate config '{}': {}", sectionId, e.getMessage());
                }
            }
        }

        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);
            if (obj != null) {
                deserialize(obj);
            }
            LOGGER.info("Loaded config section: {}", sectionId);
        } catch (Exception e) {
            LOGGER.error("Failed to load config section '{}': {}", sectionId, e.getMessage());
        }
    }

    @Override
    public void save() {
        Path configPath = getConfigPath();
        try {
            Path parent = configPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            JsonObject obj = new JsonObject();
            serialize(obj);
            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Saved config section: {}", sectionId);
        } catch (IOException e) {
            LOGGER.error("Failed to save config section '{}': {}", sectionId, e.getMessage());
        }
    }

    /**
     * JSONからフィールドを復元する
     *
     * @param obj 読み込んだJSONオブジェクト（null不可）
     */
    protected abstract void deserialize(JsonObject obj);

    /**
     * フィールドをJSONに書き出す
     *
     * @param obj 書き出し先のJSONオブジェクト（空の状態で渡される）
     */
    protected abstract void serialize(JsonObject obj);
}
