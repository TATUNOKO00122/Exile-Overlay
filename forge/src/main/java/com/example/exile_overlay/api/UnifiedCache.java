package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 統一キャッシュ管理クラス
 * 
 * 【設計思想】
 * - 単一ソース: 全データのキャッシュを一元管理
 * - フレームベース: System.currentTimeMillis()を使用しない
 * - データ型別TTL: 各DataTypeに適した更新頻度を適用
 * - スレッド安全: ConcurrentHashMapによるロックフリーアクセス
 * 
 * 【アーキテクチャ】
 * Player UUID → DataType → CachedValue
 * 
 * 【移行元】
 * - ModDataCache（廃止予定）
 * - ThrottledDataSource（ラッパーとして維持）
 * - MineAndSlashDataProvider.getCachedEntityData()
 */
public class UnifiedCache {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UnifiedCache INSTANCE = new UnifiedCache();
    
    // プレイヤーUUID → データキャッシュマップ（UUID全体を文字列キーとして使用）
    private final Map<String, Map<DataType, CachedValue>> playerCaches = new ConcurrentHashMap<>();
    
    // グローバルフレームカウンター（外部からincrementされる）
    private volatile long globalFrameCounter = 0;
    
    private UnifiedCache() {
        // シングルトン
    }
    
    /**
     * インスタンスを取得
     */
    public static UnifiedCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * グローバルフレームカウンターをインクリメント
     * 毎フレーム（Renderイベント等）から呼び出される
     */
    public void incrementFrame() {
        globalFrameCounter++;
    }
    
    /**
     * 現在のフレームカウンターを取得
     */
    public long getCurrentFrame() {
        return globalFrameCounter;
    }
    
    /**
     * 値を取得（キャッシュ利用）
     * 
     * @param player プレイヤー
     * @param type データタイプ
     * @param fetcher データ取得関数
     * @return キャッシュされた値、または新しく取得した値
     */
    public float get(Player player, DataType type, Supplier<Float> fetcher) {
        if (player == null || type == null) {
            if (type != null) {
                Object defaultVal = type.getDefaultValue();
                if (defaultVal instanceof Number) {
                    return ((Number) defaultVal).floatValue();
                }
            }
            return 0f;
        }

        // Boolean型はキャッシュせず直接取得（型衝突回避）
        if (type.getType() == Boolean.class) {
            try {
                Float value = fetcher.get();
                return value != null ? value : 0f;
            } catch (Exception e) {
                LOGGER.debug("Error fetching {} for player {}: {}",
                        type.getKey(), player.getName().getString(), e.getMessage());
                return 0f;
            }
        }

        // UUID全体を文字列として使用（衝突回避）
        String playerKey = player.getUUID().toString();
        Map<DataType, CachedValue> playerCache = playerCaches.computeIfAbsent(
            playerKey, k -> new ConcurrentHashMap<>()
        );

        CachedValue cv = playerCache.computeIfAbsent(type, CachedValue::new);
        return cv.getOrCompute(fetcher, globalFrameCounter);
    }
    
    /**
     * 値を強制更新（キャッシュ無視）
     */
    public float forceUpdate(Player player, DataType type, Supplier<Float> fetcher) {
        if (player == null || type == null) {
            if (type != null) {
                Object defaultVal = type.getDefaultValue();
                if (defaultVal instanceof Number) {
                    return ((Number) defaultVal).floatValue();
                }
            }
            return 0f;
        }

        String playerKey = player.getUUID().toString();
        Map<DataType, CachedValue> playerCache = playerCaches.computeIfAbsent(
            playerKey, k -> new ConcurrentHashMap<>()
        );

        CachedValue cv = playerCache.computeIfAbsent(type, CachedValue::new);
        cv.invalidate();
        return cv.getOrCompute(fetcher, globalFrameCounter);
    }
    
    /**
     * 特定プレイヤーの特定データタイプのキャッシュを無効化
     */
    public void invalidate(Player player, DataType type) {
        if (player == null) return;

        String playerKey = player.getUUID().toString();
        Map<DataType, CachedValue> playerCache = playerCaches.get(playerKey);
        if (playerCache != null) {
            CachedValue cv = playerCache.get(type);
            if (cv != null) {
                cv.invalidate();
            }
        }
    }
    
    /**
     * 特定プレイヤーの全キャッシュを無効化
     */
    public void invalidateAll(Player player) {
        if (player == null) return;

        String playerKey = player.getUUID().toString();
        Map<DataType, CachedValue> playerCache = playerCaches.get(playerKey);
        if (playerCache != null) {
            playerCache.values().forEach(CachedValue::invalidate);
            LOGGER.debug("Invalidated all cache for player: {}", player.getName().getString());
        }
    }
    
    /**
     * 特定プレイヤーのキャッシュを削除
     */
    public void removePlayer(Player player) {
        if (player == null) return;

        String playerKey = player.getUUID().toString();
        playerCaches.remove(playerKey);
        LOGGER.debug("Removed cache for player: {}", player.getName().getString());
    }
    
    /**
     * 全キャッシュをクリア
     */
    public void clearAll() {
        playerCaches.clear();
        globalFrameCounter = 0;
        LOGGER.info("Cleared all unified cache");
    }
    
    /**
     * 統計情報を取得
     */
    public CacheStats getStats() {
        int playerCount = playerCaches.size();
        int totalEntries = playerCaches.values().stream()
            .mapToInt(Map::size)
            .sum();
        return new CacheStats(playerCount, totalEntries, globalFrameCounter);
    }
    
    /**
     * キャッシュされた値を保持する内部クラス
     */
    private static class CachedValue {
        private final DataType type;
        private final UpdateFrequency frequency;
        private volatile float value;
        private volatile long lastUpdateFrame = -1;
        private volatile boolean valid = false;
        
        CachedValue(DataType type) {
            this.type = type;
            // DataTypeから頻度を取得（デフォルトNORMAL）
            this.frequency = type.getUpdateFrequency();
            // デフォルト値をfloatに変換（Integer/Doubleなどに対応）
            Object defaultVal = type.getDefaultValue();
            if (defaultVal instanceof Number) {
                this.value = ((Number) defaultVal).floatValue();
            } else {
                this.value = 0.0f;
            }
        }
        
        /**
         * 値を取得（必要に応じて更新）
         */
        float getOrCompute(Supplier<Float> fetcher, long currentFrame) {
            // CRITICALは毎フレーム更新
            if (frequency == UpdateFrequency.CRITICAL) {
                float newValue = fetcher.get();
                value = newValue;
                lastUpdateFrame = currentFrame;
                valid = true;
                return newValue;
            }
            
            // キャッシュが有効かチェック
            if (valid && lastUpdateFrame >= 0) {
                long framesSinceUpdate = currentFrame - lastUpdateFrame;
                if (framesSinceUpdate < frequency.getFrameInterval()) {
                    return value;  // キャッシュ返却
                }
            }
            
            // 更新が必要
            synchronized (this) {
                // ダブルチェック
                if (valid && lastUpdateFrame >= 0) {
                    long framesSinceUpdate = currentFrame - lastUpdateFrame;
                    if (framesSinceUpdate < frequency.getFrameInterval()) {
                        return value;
                    }
                }
                
                try {
                    float newValue = fetcher.get();
                    value = newValue;
                    lastUpdateFrame = currentFrame;
                    valid = true;
                    return newValue;
                } catch (Exception e) {
                    LOGGER.debug("Failed to fetch value for {}: {}", type.getKey(), e.getMessage());
                    // 失敗しても古い値を維持（有効な場合）
                    if (valid) {
                        return value;
                    }
                    return type.getDefaultValueTyped();
                }
            }
        }
        
        /**
         * キャッシュを無効化
         */
        void invalidate() {
            valid = false;
            lastUpdateFrame = -1;
        }
    }
    
    /**
     * キャッシュ統計情報
     */
    public record CacheStats(int playerCount, int totalEntries, long frameCounter) {
        @Override
        public String toString() {
            return String.format("CacheStats{players=%d, entries=%d, frame=%d}", 
                playerCount, totalEntries, frameCounter);
        }
    }
}
