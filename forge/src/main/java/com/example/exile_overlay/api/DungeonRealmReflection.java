package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Dungeon Realm MODへのリフレクションアクセス
 * 
 * MethodHandlesを使用して高速かつ安全にアクセス
 * クラスが存在しない場合はエラーを返さずデフォルト値を使用
 */
public final class DungeonRealmReflection {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // クラスのキャッシュ
    private static Class<?> dungeonMainClass;
    private static Class<?> dungeonMapCapabilityClass;
    private static Class<?> dungeonMapDataClass;
    private static Class<?> dungeonItemMapDataClass;

    // MethodHandleのキャッシュ
    private static MethodHandle mhGetData;
    private static MethodHandle mhGetMapData;
    private static MethodHandle mhGetFinishRarity;
    private static MethodHandle mhGetRarityTier;
    private static MethodHandle mhGetCurrentMobKillRarity;
    private static MethodHandle mhGetMobKills;
    private static MethodHandle mhGetEliteKills;
    private static MethodHandle mhGetMiniBossKills;
    private static MethodHandle mhGetMobSpawnCount;
    private static MethodHandle mhGetEliteSpawnCount;
    private static MethodHandle mhGetMiniBossSpawnCount;
    private static MethodHandle mhGetLootedChests;
    private static MethodHandle mhGetTotalChests;
    private static MethodHandle mhIsUber;
    private static MethodHandle mhGetDungeonId;

    private static boolean initialized = false;
    private static boolean available = false;

    static {
        try {
            initClasses();
            initMethodHandles();
            initialized = true;
            available = true;
            LOGGER.info("Dungeon Realm reflection initialized successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Dungeon Realm classes not found: {}", e.getMessage());
            initialized = true;
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Dungeon Realm reflection: {}", e.getMessage());
            initialized = true;
            available = false;
        }
    }

    private static void initClasses() throws ClassNotFoundException {
        dungeonMainClass = Class.forName("com.robertx22.dungeon_realm.main.DungeonMain");
        dungeonMapCapabilityClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonMapCapability");
        dungeonMapDataClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonMapData");
        dungeonItemMapDataClass = Class.forName("com.robertx22.dungeon_realm.item.DungeonItemMapData");
    }

    private static void initMethodHandles() throws Exception {
        // DungeonMapCapability.get(Level) -> DungeonMapCapability
        mhGetData = LOOKUP.findStatic(dungeonMapCapabilityClass, "get",
                MethodType.methodType(dungeonMapCapabilityClass, Level.class));

        // DungeonMapCapability.data -> DungeonWorldData
        var worldDataClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonWorldData");
        var dataField = LOOKUP.findGetter(dungeonMapCapabilityClass, "data", worldDataClass);

        // DungeonWorldData.data -> DungeonPlayerData
        var playerDataClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonPlayerData");
        var playerDataField = LOOKUP.findGetter(worldDataClass, "data", playerDataClass);

        // DungeonPlayerData.getData(Player) -> Object (ジェネリック型消去)
        // 親クラスからメソッドを取得
        var mapPlayerDataSaverClass = Class.forName("com.robertx22.library_of_exile.dimension.worlddata.MapPlayerDataSaver");
        java.lang.reflect.Method getDataMethod = mapPlayerDataSaverClass.getMethod("getData", Player.class);
        mhGetMapData = LOOKUP.unreflect(getDataMethod);

        // DungeonMapData fields
        mhGetMobKills = LOOKUP.findGetter(dungeonMapDataClass, "mobKills", int.class);
        mhGetEliteKills = LOOKUP.findGetter(dungeonMapDataClass, "eliteKills", int.class);
        mhGetMiniBossKills = LOOKUP.findGetter(dungeonMapDataClass, "miniBossKills", int.class);
        mhGetMobSpawnCount = LOOKUP.findGetter(dungeonMapDataClass, "mobSpawnCount", int.class);
        mhGetEliteSpawnCount = LOOKUP.findGetter(dungeonMapDataClass, "eliteSpawnCount", int.class);
        mhGetMiniBossSpawnCount = LOOKUP.findGetter(dungeonMapDataClass, "miniBossSpawnCount", int.class);
        mhGetLootedChests = LOOKUP.findGetter(dungeonMapDataClass, "lootedChests", int.class);
        mhGetTotalChests = LOOKUP.findGetter(dungeonMapDataClass, "totalChests", int.class);
        mhGetCurrentMobKillRarity = LOOKUP.findGetter(dungeonMapDataClass, "current_mob_kill_rarity", String.class);
        mhGetDungeonId = LOOKUP.findGetter(dungeonMapDataClass, "dungeon", String.class);

        // MapFinishRarity.getTier() - 先にクラスを取得
        var mapFinishRarityClass = Class.forName("com.robertx22.library_of_exile.database.map_finish_rarity.MapFinishRarity");
        mhGetRarityTier = LOOKUP.findVirtual(mapFinishRarityClass, "getTier",
                MethodType.methodType(int.class));

        // DungeonMapData.getFinishRarity() -> MapFinishRarity
        mhGetFinishRarity = LOOKUP.findVirtual(dungeonMapDataClass, "getFinishRarity",
                MethodType.methodType(mapFinishRarityClass));

        // DungeonItemMapData.uber
        mhIsUber = LOOKUP.findGetter(dungeonItemMapDataClass, "uber", boolean.class);
    }

    public static boolean isAvailable() {
        return available;
    }

    /**
     * プレイヤーがダンジョン内にいるかチェック
     */
    public static boolean isInsideDungeon(Player player) {
        if (!available || player == null) return false;

        try {
            Level level = player.level();
            if (level == null || level.isClientSide) return false;

            Object capability = mhGetData.invoke(level);
            if (capability == null) return false;

            Object worldData = LOOKUP.findGetter(dungeonMapCapabilityClass, "data",
                    Class.forName("com.robertx22.dungeon_realm.structure.DungeonWorldData")).invoke(capability);
            if (worldData == null) return false;

            Object playerData = LOOKUP.findGetter(worldData.getClass(), "data",
                    Class.forName("com.robertx22.dungeon_realm.structure.DungeonPlayerData")).invoke(worldData);
            if (playerData == null) return false;

            Object mapData = mhGetMapData.invoke(playerData, player);
            return mapData != null;
        } catch (Throwable t) {
            LOGGER.debug("Error checking if inside dungeon: {}", t.getMessage());
            return false;
        }
    }

    /**
     * DungeonMapDataを取得
     */
    public static Object getDungeonMapData(Player player) {
        if (!available || player == null) return null;

        try {
            Level level = player.level();
            if (level == null || level.isClientSide) return null;

            Object capability = mhGetData.invoke(level);
            if (capability == null) return null;

            var worldDataClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonWorldData");
            var dataField = LOOKUP.findGetter(dungeonMapCapabilityClass, "data", worldDataClass);
            Object worldData = dataField.invoke(capability);
            if (worldData == null) return null;

            var playerDataClass = Class.forName("com.robertx22.dungeon_realm.structure.DungeonPlayerData");
            var playerDataField = LOOKUP.findGetter(worldDataClass, "data", playerDataClass);
            Object playerData = playerDataField.invoke(worldData);
            if (playerData == null) return null;

            return mhGetMapData.invoke(playerData, player);
        } catch (Throwable t) {
            LOGGER.debug("Error getting dungeon map data: {}", t.getMessage());
            return null;
        }
    }

    public static int calculateKillPercent(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            int mobKills = (int) mhGetMobKills.invoke(mapData);
            int eliteKills = (int) mhGetEliteKills.invoke(mapData);
            int miniBossKills = (int) mhGetMiniBossKills.invoke(mapData);
            int mobSpawn = (int) mhGetMobSpawnCount.invoke(mapData);
            int eliteSpawn = (int) mhGetEliteSpawnCount.invoke(mapData);
            int miniBossSpawn = (int) mhGetMiniBossSpawnCount.invoke(mapData);
            
            int totalKills = mobKills + eliteKills + miniBossKills;
            int totalSpawns = mobSpawn + eliteSpawn + miniBossSpawn;
            
            if (totalSpawns == 0) return 0;
            return Math.min(100, Math.round((float) totalKills / totalSpawns * 100));
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int calculateLootPercent(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            int looted = (int) mhGetLootedChests.invoke(mapData);
            int total = (int) mhGetTotalChests.invoke(mapData);
            
            if (total == 0) return 0;
            return Math.min(100, Math.round((float) looted / total * 100));
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getRarityTier(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            Object rarity = mhGetFinishRarity.invoke(mapData);
            if (rarity == null) return 0;
            return (int) mhGetRarityTier.invoke(rarity);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String getRarityName(Object mapData) {
        if (!available || mapData == null) return "common";
        try {
            return (String) mhGetCurrentMobKillRarity.invoke(mapData);
        } catch (Throwable t) {
            return "common";
        }
    }

    public static int getMobKills(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetMobKills.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getEliteKills(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetEliteKills.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getMiniBossKills(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetMiniBossKills.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getMobSpawnCount(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetMobSpawnCount.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getEliteSpawnCount(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetEliteSpawnCount.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getMiniBossSpawnCount(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetMiniBossSpawnCount.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getLootedChests(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetLootedChests.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getTotalChests(Object mapData) {
        if (!available || mapData == null) return 0;
        try {
            return (int) mhGetTotalChests.invoke(mapData);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean isUber(Object mapData) {
        if (!available || mapData == null) return false;
        try {
            var itemField = LOOKUP.findGetter(dungeonMapDataClass, "item", dungeonItemMapDataClass);
            Object itemData = itemField.invoke(mapData);
            if (itemData == null) return false;
            return (boolean) mhIsUber.invoke(itemData);
        } catch (Throwable t) {
            return false;
        }
    }

    public static String getDungeonId(Object mapData) {
        if (!available || mapData == null) return "";
        try {
            String id = (String) mhGetDungeonId.invoke(mapData);
            return id != null ? id : "";
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * プレイヤーのScoreboardからダンジョン情報をクリア
     * dungeon_realm MODが作成した「completion_percent」Objectiveを削除
     */
    public static void clearScoreboard(Player player) {
        if (!available || player == null) return;
        
        try {
            var scoreboard = player.getScoreboard();
            if (scoreboard == null) return;
            
            var objective = scoreboard.getObjective("completion_percent");
            if (objective != null) {
                // display slot 1 (sidebar)から削除
                scoreboard.setDisplayObjective(1, null);
                scoreboard.removeObjective(objective);
                LOGGER.debug("Cleared dungeon_realm scoreboard for player: {}", player.getName().getString());
            }
        } catch (Throwable t) {
            LOGGER.debug("Error clearing scoreboard: {}", t.getMessage());
        }
    }
}