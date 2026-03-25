package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Dungeon Realm MOD用のデータプロバイダー
 * 
 * ダンジョン関連のデータを提供:
 * - キル達成率
 * - ルート達成率
 * - レアリティティア
 * - 各種キル数
 * 
 * 【データ取得元】
 * - DungeonMapCapability: ワールドに保存されたダンジョンデータ
 * - DungeonMapData: 現在のダンジョンの状態
 */
public class DungeonRealmDataProvider extends AbstractModDataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PRIORITY = 50;  // 低優先度：HP/Manaなどの主要データは他のプロバイダーから取得
    private static final String MOD_ID = "dungeon_realm";

    private static boolean modLoaded = false;
    private static boolean checked = false;

    public DungeonRealmDataProvider() {
    }

    @Override
    public boolean isAvailable() {
        if (!checked) {
            try {
                Class.forName("com.robertx22.dungeon_realm.main.DungeonMain");
                // Reflection初期化も成功しているか確認
                modLoaded = DungeonRealmReflection.isAvailable();
                if (modLoaded) {
                    LOGGER.info("Dungeon Realm detected, provider enabled");
                } else {
                    LOGGER.warn("Dungeon Realm detected but reflection failed, provider disabled");
                }
            } catch (ClassNotFoundException e) {
                LOGGER.debug("Dungeon Realm not found, provider disabled");
                modLoaded = false;
            }
            checked = true;
        }
        return modLoaded;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getId() {
        return "dungeon_realm";
    }

    @Override
    public float getValue(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return type.getDefaultValueTyped();
        }

        try {
            Object mapData = getDungeonMapData(player);
            if (mapData == null) {
                return type.getDefaultValueTyped();
            }

            return switch (type) {
                case DUNGEON_KILL_PERCENT -> (float) calculateKillPercent(mapData);
                case DUNGEON_LOOT_PERCENT -> (float) calculateLootPercent(mapData);
                case DUNGEON_RARITY_TIER -> (float) getRarityTier(mapData);
                case DUNGEON_MOB_KILLS -> (float) getMobKills(mapData);
                case DUNGEON_ELITE_KILLS -> (float) getEliteKills(mapData);
                case DUNGEON_MINIBOSS_KILLS -> (float) getMiniBossKills(mapData);
                case DUNGEON_MOB_SPAWN_COUNT -> (float) getMobSpawnCount(mapData);
                case DUNGEON_ELITE_SPAWN_COUNT -> (float) getEliteSpawnCount(mapData);
                case DUNGEON_MINIBOSS_SPAWN_COUNT -> (float) getMiniBossSpawnCount(mapData);
                case DUNGEON_CHESTS_LOOTED -> (float) getLootedChests(mapData);
                case DUNGEON_CHESTS_TOTAL -> (float) getTotalChests(mapData);
                case DUNGEON_IS_UBER -> isUber(mapData) ? 1.0f : 0.0f;
                case DUNGEON_IS_INSIDE -> isInsideDungeon(player) ? 1.0f : 0.0f;
                default -> type.getDefaultValueTyped();
            };
        } catch (Exception e) {
            LOGGER.debug("Error getting dungeon data for {}: {}", type.getKey(), e.getMessage());
            return type.getDefaultValueTyped();
        }
    }

    @Override
    public boolean getAttribute(Player player, String attributeKey) {
        if (!isAvailable() || player == null) {
            return false;
        }

        DataType type = DataType.fromKey(attributeKey);
        if (type == null) return false;

        try {
            return switch (type) {
                case DUNGEON_IS_UBER -> {
                    Object mapData = getDungeonMapData(player);
                    yield mapData != null && isUber(mapData);
                }
                case DUNGEON_IS_INSIDE -> isInsideDungeon(player);
                default -> false;
            };
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 文字列データを取得
     */
    public String getString(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return type.getDefaultValueTyped();
        }

        try {
            Object mapData = getDungeonMapData(player);
            if (mapData == null) {
                return type.getDefaultValueTyped();
            }

            return switch (type) {
                case DUNGEON_RARITY_NAME -> getRarityName(mapData);
                case DUNGEON_ID -> getDungeonId(mapData);
                default -> type.getDefaultValueTyped();
            };
        } catch (Exception e) {
            return type.getDefaultValueTyped();
        }
    }

    // ========== リフレクションベースのデータ取得 ==========

    private Object getDungeonMapData(Player player) {
        try {
            return DungeonRealmReflection.getDungeonMapData(player);
        } catch (Exception e) {
            LOGGER.debug("Failed to get dungeon map data: {}", e.getMessage());
            return null;
        }
    }

    private boolean isInsideDungeon(Player player) {
        try {
            return DungeonRealmReflection.isInsideDungeon(player);
        } catch (Exception e) {
            return false;
        }
    }

    private int calculateKillPercent(Object mapData) {
        try {
            return DungeonRealmReflection.calculateKillPercent(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int calculateLootPercent(Object mapData) {
        try {
            return DungeonRealmReflection.calculateLootPercent(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getRarityTier(Object mapData) {
        try {
            return DungeonRealmReflection.getRarityTier(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getRarityName(Object mapData) {
        try {
            return DungeonRealmReflection.getRarityName(mapData);
        } catch (Exception e) {
            return "common";
        }
    }

    private int getMobKills(Object mapData) {
        try {
            return DungeonRealmReflection.getMobKills(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getEliteKills(Object mapData) {
        try {
            return DungeonRealmReflection.getEliteKills(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getMiniBossKills(Object mapData) {
        try {
            return DungeonRealmReflection.getMiniBossKills(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getMobSpawnCount(Object mapData) {
        try {
            return DungeonRealmReflection.getMobSpawnCount(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getEliteSpawnCount(Object mapData) {
        try {
            return DungeonRealmReflection.getEliteSpawnCount(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getMiniBossSpawnCount(Object mapData) {
        try {
            return DungeonRealmReflection.getMiniBossSpawnCount(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getLootedChests(Object mapData) {
        try {
            return DungeonRealmReflection.getLootedChests(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private int getTotalChests(Object mapData) {
        try {
            return DungeonRealmReflection.getTotalChests(mapData);
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isUber(Object mapData) {
        try {
            return DungeonRealmReflection.isUber(mapData);
        } catch (Exception e) {
            return false;
        }
    }

    private String getDungeonId(Object mapData) {
        try {
            return DungeonRealmReflection.getDungeonId(mapData);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * プレイヤーのScoreboardからダンジョン情報をクリア
     * dungeon_realm MODがScoreboardに表示する「Map Stats」を非表示にする
     */
    public void clearDungeonScoreboard(Player player) {
        if (!isAvailable() || player == null) return;
        
        try {
            DungeonRealmReflection.clearScoreboard(player);
        } catch (Exception e) {
            LOGGER.debug("Failed to clear dungeon scoreboard: {}", e.getMessage());
        }
    }

    /**
     * プレイヤーがダンジョン内にいる場合、Scoreboardをクリア
     * このメソッドはHUD描画時などに定期的に呼び出す
     */
    public void tickClearScoreboard(Player player) {
        if (!isAvailable() || player == null) return;
        
        if (isInsideDungeon(player)) {
            clearDungeonScoreboard(player);
        }
    }
}