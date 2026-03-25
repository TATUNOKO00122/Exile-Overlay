package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MODデータのキャッシュ管理クラス
 * プレイヤー単位でデータをキャッシュし、TTL（生存時間）管理を行う
 * 
 * @deprecated {@link UnifiedCache}に統合されました。
 *             このクラスは将来のバージョンで削除されます。
 *             新しいコードではUnifiedCache.getInstance()を使用してください。
 *             
 * 【移行ガイド】
 * 旧: ModDataCache.getInstance().cacheData(player, data)
 * 新: UnifiedCache.getInstance().get(player, type, fetcher)
 * 
 * 【スレッド安全性】
 * - このクラスはConcurrentHashMapを使用してスレッド安全性を確保
 * - 複数スレッドから安全に読み書き可能
 * - データ更新とレンダリングの競合を防止
 * 
 * 【アーキテクチャ】
 * - ネットワークスレッド（パケット受信）→ 書き込み
 * - レンダースレッド → 読み込み
 * - ConcurrentHashMapによりロックフリーな並行アクセスを実現
 */
@Deprecated(since = "2.0", forRemoval = true)
public class ModDataCache {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // デフォルトのキャッシュ有効期限（ミリ秒）
    public static final long DEFAULT_CACHE_DURATION_MS = 250;
    
    // シングルトンインスタンス
    private static final ModDataCache INSTANCE = new ModDataCache();
    
    // キャッシュエントリのマップ（プレイヤーUUID -> エントリ）
    // ConcurrentHashMapを使用してスレッド安全性を確保
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    /**
     * キャッシュエントリ内部クラス
     * スレッド安全性: データマップはConcurrentHashMapとして管理
     */
    private static class CacheEntry {
        final WeakReference<Player> playerRef;
        final ConcurrentHashMap<String, Object> data;
        final long timestamp;
        final long durationMs;
        
        CacheEntry(Player player, ConcurrentHashMap<String, Object> data, long durationMs) {
            this.playerRef = new WeakReference<>(player);
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.durationMs = durationMs;
        }
        
        boolean isValid() {
            return playerRef.get() != null && 
                   (System.currentTimeMillis() - timestamp) < durationMs;
        }
    }
    
    private ModDataCache() {
        // シングルトン
    }
    
    /**
     * キャッシュインスタンスを取得
     */
    public static ModDataCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * データをキャッシュに保存
     * スレッド安全性: このメソッドは複数スレッドから安全に呼び出せます
     * 
     * @param player プレイヤー
     * @param data 保存するデータマップ
     * @param durationMs キャッシュ有効期限（ミリ秒）
     */
    public void cacheData(Player player, Map<String, Object> data, long durationMs) {
        if (player == null || data == null) {
            return;
        }
        
        String key = getPlayerKey(player);
        // ConcurrentHashMapを使用してスレッド安全にデータをコピー
        ConcurrentHashMap<String, Object> threadSafeData = new ConcurrentHashMap<>(data);
        cache.put(key, new CacheEntry(player, threadSafeData, durationMs));
        
        LOGGER.debug("Cached data for player: {} ({} entries, {}ms TTL)", 
                player.getName().getString(), data.size(), durationMs);
    }
    
    /**
     * データをキャッシュに保存（デフォルトTTL使用）
     */
    public void cacheData(Player player, Map<String, Object> data) {
        cacheData(player, data, DEFAULT_CACHE_DURATION_MS);
    }
    
    /**
     * キャッシュからデータを取得
     * 
     * @param player プレイヤー
     * @param key データキー
     * @param type 期待するデータ型
     * @return キャッシュされたデータ、無効な場合はnull
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedData(Player player, String key, Class<T> type) {
        if (player == null || key == null) {
            return null;
        }
        
        String playerKey = getPlayerKey(player);
        CacheEntry entry = cache.get(playerKey);
        
        if (entry == null || !entry.isValid()) {
            if (entry != null) {
                // 期限切れなので削除
                cache.remove(playerKey);
                LOGGER.debug("Cache expired for player: {}", player.getName().getString());
            }
            return null;
        }
        
        Object value = entry.data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        
        return null;
    }
    
    /**
     * DataTypeを使用してキャッシュからデータを取得
     */
    @SuppressWarnings("unchecked")
    public <T> T getCachedData(Player player, DataType dataType) {
        Object value = getCachedData(player, dataType.getKey(), dataType.getType());
        if (value != null) {
            return (T) value;
        }
        return dataType.getDefaultValueTyped();
    }
    
    /**
     * キャッシュが有効かどうかチェック
     */
    public boolean isCacheValid(Player player) {
        if (player == null) {
            return false;
        }
        
        String key = getPlayerKey(player);
        CacheEntry entry = cache.get(key);
        return entry != null && entry.isValid();
    }
    
    /**
     * 特定プレイヤーのキャッシュをクリア
     */
    public void invalidateCache(Player player) {
        if (player == null) {
            return;
        }
        
        String key = getPlayerKey(player);
        cache.remove(key);
        LOGGER.debug("Cache invalidated for player: {}", player.getName().getString());
    }
    
    /**
     * 全キャッシュをクリア
     */
    public void clearAllCache() {
        int count = cache.size();
        cache.clear();
        LOGGER.debug("Cleared all cache ({} entries)", count);
    }
    
    /**
     * 期限切れエントリを削除
     */
    public void cleanupExpired() {
        int beforeSize = cache.size();
        cache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        int removed = beforeSize - cache.size();
        
        if (removed > 0) {
            LOGGER.debug("Cleaned up {} expired cache entries", removed);
        }
    }
    
    /**
     * プレイヤー識別用のキーを生成
     */
    private String getPlayerKey(Player player) {
        return player.getUUID().toString();
    }
    
    /**
     * 現在のキャッシュサイズを取得
     */
    public int getCacheSize() {
        return cache.size();
    }
}
