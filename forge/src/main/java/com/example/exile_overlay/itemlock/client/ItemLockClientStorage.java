package com.example.exile_overlay.itemlock.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * クライアント側でのアイテムロック設定をローカルファイルに永続化するストレージ。
 * サーバー側MODの有無にかかわらず、プレイヤーごとのロックスロット状態を維持する。
 */
public final class ItemLockClientStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemLockClientStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("exile_overlay/item_locks.json");
    private static final Map<String, Long> LOCK_CACHE = new HashMap<>();
    private static boolean loaded = false;

    private ItemLockClientStorage() {}

    public static synchronized void load() {
        if (!Files.exists(CONFIG_FILE)) {
            loaded = true;
            return;
        }
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
            Map<String, Long> data = GSON.fromJson(reader, MAP_TYPE);
            if (data != null) {
                LOCK_CACHE.clear();
                LOCK_CACHE.putAll(data);
            }
            loaded = true;
        } catch (Exception e) {
            LOGGER.error("Failed to load item locks config: {}", e.getMessage());
        }
    }

    public static synchronized void save() {
        try {
            Path parent = CONFIG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(LOCK_CACHE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save item locks config: {}", e.getMessage());
        }
    }

    public static synchronized long getLockMask(String playerUuid) {
        if (!loaded) {
            load();
        }
        if (playerUuid == null) return 0L;
        return LOCK_CACHE.getOrDefault(playerUuid, 0L);
    }

    public static synchronized void setLockMask(String playerUuid, long mask) {
        if (!loaded) {
            load();
        }
        if (playerUuid == null) return;
        LOCK_CACHE.put(playerUuid, mask);
        save();
    }
}
