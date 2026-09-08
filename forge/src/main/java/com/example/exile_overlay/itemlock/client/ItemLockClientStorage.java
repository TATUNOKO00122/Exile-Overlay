package com.example.exile_overlay.itemlock.client;

import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.itemlock.LockManager;
import com.example.exile_overlay.itemlock.network.LockSlotC2S;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * クライアント側でのアイテムロック設定をローカルファイルに永続化するストレージ。
 * 接続先（シングルプレイのワールド、専用サーバーのIP等）ごとにスコープを分離して管理する。
 */
@OnlyIn(Dist.CLIENT)
public final class ItemLockClientStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemLockClientStorage.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();
    private static final Path CONFIG_FILE = FMLPaths.CONFIGDIR.get().resolve("exile_overlay/item_locks.json");
    private static final Map<String, Long> LOCK_CACHE = new HashMap<>();
    private static boolean loaded = false;
    private static volatile String activeStorageKey = null;

    private ItemLockClientStorage() {}

    /**
     * 現在の接続先とプレイヤーに応じた一意なストレージキーを生成
     */
    public static String getCurrentStorageKey() {
        return getCurrentStorageKey(null);
    }

    /**
     * 指定プレイヤーおよび現在の接続先に応じた一意なストレージキーを生成
     */
    public static String getCurrentStorageKey(Player player) {
        Minecraft mc = Minecraft.getInstance();
        Player targetPlayer = player != null ? player : mc.player;
        if (targetPlayer == null) {
            return activeStorageKey;
        }
        String key = getStorageKey(mc, targetPlayer.getStringUUID());
        if (key != null && !key.startsWith("unknown")) {
            activeStorageKey = key;
            return key;
        }
        return activeStorageKey != null ? activeStorageKey : key;
    }

    public static void clearActiveKey() {
        activeStorageKey = null;
    }

    /**
     * 接続先コンテキストから一意キーを生成（sp:<folder>:<uuid> または mp:<ip>:<uuid>）
     */
    public static String getStorageKey(Minecraft mc, String playerUuid) {
        if (playerUuid == null || playerUuid.isEmpty()) {
            return "unknown";
        }

        // 1. シングルプレイ
        if (mc.isSingleplayer() && mc.getSingleplayerServer() != null) {
            try {
                Path worldPath = mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT);
                if (worldPath != null && worldPath.getFileName() != null) {
                    return "sp:" + worldPath.getFileName().toString() + ":" + playerUuid;
                }
            } catch (Exception ignored) {
            }
            try {
                String levelName = mc.getSingleplayerServer().getWorldData().getLevelName();
                if (levelName != null && !levelName.isEmpty()) {
                    return "sp:" + levelName + ":" + playerUuid;
                }
            } catch (Exception ignored) {
            }
            return "sp:default:" + playerUuid;
        }

        // 2. 専用サーバー
        ServerData serverData = mc.getCurrentServer();
        if (serverData != null && serverData.ip != null && !serverData.ip.trim().isEmpty()) {
            return "mp:" + serverData.ip.trim().toLowerCase(Locale.ROOT) + ":" + playerUuid;
        }

        // 3. LAN参加等（ServerDataが存在しないリモート接続）
        if (mc.getConnection() != null && mc.getConnection().getConnection() != null) {
            SocketAddress address = mc.getConnection().getConnection().getRemoteAddress();
            if (address != null) {
                String addrStr = address.toString().trim().toLowerCase(Locale.ROOT);
                if (addrStr.startsWith("/")) {
                    addrStr = addrStr.substring(1);
                }
                return "mp:" + addrStr + ":" + playerUuid;
            }
        }

        return "unknown:" + playerUuid;
    }

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

    /**
     * ストレージキーに対応するロックマスクを取得。
     * シングルプレイ時のみ旧フォーマット（UUID単体キー）からの移行をサポートする。
     */
    public static synchronized long getLockMask(String storageKey) {
        if (!loaded) {
            load();
        }
        if (storageKey == null) return 0L;

        Long cached = LOCK_CACHE.get(storageKey);
        if (cached != null) {
            return cached;
        }

        // 後方互換: シングルプレイのみ旧UUIDキーからの移行を許可（マルチプレイへの漏洩を防止）
        if (storageKey.startsWith("sp:")) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                String uuid = mc.player.getStringUUID();
                Long oldMask = LOCK_CACHE.get(uuid);
                if (oldMask != null) {
                    LOCK_CACHE.put(storageKey, oldMask);
                    save();
                    return oldMask;
                }
            }
        }

        return 0L;
    }

    public static synchronized void setLockMask(String storageKey, long mask) {
        if (!loaded) {
            load();
        }
        if (storageKey == null) return;
        LOCK_CACHE.put(storageKey, mask);
        save();
    }

    /**
     * 接続先サーバーに本MOD（ネットワークチャンネル）が存在するか判定
     */
    public static boolean isServerModPresent() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.isSingleplayer()) return true;
        if (mc.getConnection() == null) return false;
        Connection connection = mc.getConnection().getConnection();
        if (connection == null) return false;
        try {
            return NetworkHandler.CHANNEL.isRemotePresent(connection);
        } catch (Exception ignored) {
            return false;
        }
    }

    public static void handleSync(long lockedMask) {
        String storageKey = getCurrentStorageKey();
        if (lockedMask != 0L) {
            LockManager.setClientLockedMask(lockedMask);
            if (storageKey != null) {
                setLockMask(storageKey, lockedMask);
                save();
            }
        } else {
            // サーバー側マスクが0の場合、クライアント側の保存済みマスクがあればサーバーへ反映
            if (storageKey != null) {
                long localMask = getLockMask(storageKey);
                if (localMask != 0L) {
                    LockManager.setClientLockedMask(localMask);
                    if (isServerModPresent()) {
                        NetworkHandler.CHANNEL.sendToServer(new LockSlotC2S(localMask));
                    }
                    return;
                }
            }
            LockManager.setClientLockedMask(0L);
        }
    }
}
