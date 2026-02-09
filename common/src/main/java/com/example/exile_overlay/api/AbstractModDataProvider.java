package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * MODデータプロバイダーの抽象基底クラス
 * キャッシュ管理、エラーハンドリング、共通処理を提供
 * 新しいMODプロバイダーを作成する際はこのクラスを継承すること
 */
public abstract class AbstractModDataProvider implements IModDataProvider {
    
    protected final Logger logger = LogUtils.getLogger();
    protected final ModDataCache cache = ModDataCache.getInstance();
    
    // キャッシュ有効期限（ミリ秒）- デフォルト250ms
    protected long cacheDurationMs = 250;
    
    // 利用可能性のキャッシュ（パフォーマンス最適化）
    private Boolean availabilityCache = null;
    
    /**
     * このプロバイダーが利用可能かどうか
     * 実装クラスではMODがロードされているかなどをチェック
     */
    @Override
    public abstract boolean isAvailable();
    
    /**
     * 利用可能性をチェックし、結果をキャッシュする
     * 頻繁に呼ばれるため、初回のみ実際にチェック
     */
    protected final boolean checkAvailability() {
        if (availabilityCache == null) {
            try {
                availabilityCache = isAvailable();
                if (availabilityCache) {
                    logger.info("Data provider '{}' is available (priority: {})", 
                            getId(), getPriority());
                } else {
                    logger.debug("Data provider '{}' is not available", getId());
                }
            } catch (Exception e) {
                logger.debug("Error checking availability for '{}': {}", 
                        getId(), e.getMessage());
                availabilityCache = false;
            }
        }
        return availabilityCache;
    }
    
    /**
     * キャッシュ有効期限を設定
     */
    public void setCacheDuration(long durationMs) {
        this.cacheDurationMs = durationMs;
    }
    
    /**
     * キャッシュ有効期限を取得
     */
    public long getCacheDuration() {
        return cacheDurationMs;
    }
    
    /**
     * データを安全に取得（エラーハンドリング付き）
     * 
     * @param player プレイヤー
     * @param dataType データタイプ
     * @param fetcher データ取得関数
     * @return 取得したデータ、エラー時はデフォルト値
     */
    protected <T> T fetchSafely(Player player, DataType dataType, DataFetcher<T> fetcher) {
        if (player == null) {
            return dataType.getDefaultValueTyped();
        }
        
        try {
            // キャッシュチェック
            T cached = cache.getCachedData(player, dataType);
            if (cached != null && !cached.equals(dataType.getDefaultValue())) {
                return cached;
            }
            
            // データ取得
            T value = fetcher.fetch(player);
            
            // キャッシュに保存
            if (value != null) {
                cacheValue(player, dataType.getKey(), value);
            }
            
            return value != null ? value : dataType.getDefaultValueTyped();
            
        } catch (Exception e) {
            logger.debug("Error fetching {} for player {}: {}", 
                    dataType.getKey(), player.getName().getString(), e.getMessage());
            return dataType.getDefaultValueTyped();
        }
    }
    
    /**
     * バニラの値を安全に取得（エラーハンドリング付き）
     */
    protected <T> T fetchVanillaSafely(Player player, DataType dataType, DataFetcher<T> fetcher) {
        if (player == null) {
            return dataType.getDefaultValueTyped();
        }
        
        try {
            T value = fetcher.fetch(player);
            return value != null ? value : dataType.getDefaultValueTyped();
        } catch (Exception e) {
            logger.debug("Error fetching vanilla {}: {}", dataType.getKey(), e.getMessage());
            return dataType.getDefaultValueTyped();
        }
    }
    
    /**
     * 値をキャッシュに保存
     */
    protected void cacheValue(Player player, String key, Object value) {
        Map<String, Object> data = new HashMap<>();
        data.put(key, value);
        cache.cacheData(player, data, cacheDurationMs);
    }
    
    /**
     * 複数の値をキャッシュに保存
     */
    protected void cacheValues(Player player, Map<String, Object> data) {
        cache.cacheData(player, data, cacheDurationMs);
    }
    
    /**
     * キャッシュを無効化
     */
    protected void invalidateCache(Player player) {
        cache.invalidateCache(player);
    }
    
    /**
     * データ取得関数のインターフェース
     */
    @FunctionalInterface
    protected interface DataFetcher<T> {
        T fetch(Player player) throws Exception;
    }
    
    // ========== IModDataProvider のデフォルト実装 ==========
    
    @Override
    public float getCurrentHealth(Player player) {
        return fetchVanillaSafely(player, DataType.CURRENT_HEALTH, Player::getHealth);
    }
    
    @Override
    public float getMaxHealth(Player player) {
        return fetchVanillaSafely(player, DataType.MAX_HEALTH, Player::getMaxHealth);
    }
    
    @Override
    public float getCurrentMana(Player player) {
        return fetchSafely(player, DataType.CURRENT_MANA, p -> 0.0f);
    }
    
    @Override
    public float getMaxMana(Player player) {
        return fetchSafely(player, DataType.MAX_MANA, p -> 1.0f);
    }
    
    @Override
    public float getCurrentMagicShield(Player player) {
        return fetchSafely(player, DataType.CURRENT_MAGIC_SHIELD, p -> 0.0f);
    }
    
    @Override
    public float getMaxMagicShield(Player player) {
        return fetchSafely(player, DataType.MAX_MAGIC_SHIELD, p -> 0.0f);
    }
    
    @Override
    public float getCurrentEnergy(Player player) {
        return fetchSafely(player, DataType.CURRENT_ENERGY, p -> 0.0f);
    }
    
    @Override
    public float getMaxEnergy(Player player) {
        return fetchSafely(player, DataType.MAX_ENERGY, p -> 0.0f);
    }
    
    @Override
    public float getCurrentBlood(Player player) {
        return fetchSafely(player, DataType.CURRENT_BLOOD, p -> 0.0f);
    }
    
    @Override
    public float getMaxBlood(Player player) {
        return fetchSafely(player, DataType.MAX_BLOOD, p -> 0.0f);
    }
    
    @Override
    public int getLevel(Player player) {
        return fetchVanillaSafely(player, DataType.LEVEL, p -> p.experienceLevel);
    }
    
    @Override
    public float getExp(Player player) {
        return fetchVanillaSafely(player, DataType.EXP, p -> p.experienceProgress);
    }
    
    @Override
    public float getExpRequiredForLevelUp(Player player) {
        return fetchVanillaSafely(player, DataType.EXP_REQUIRED, p -> 1.0f);
    }
    
    @Override
    public boolean isBloodMagicActive(Player player) {
        return fetchSafely(player, DataType.BLOOD_MAGIC_ACTIVE, p -> false);
    }
}
