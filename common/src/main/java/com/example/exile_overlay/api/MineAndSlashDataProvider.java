package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.ref.WeakReference;

/**
 * Mine and Slash (M&S) MOD用のデータプロバイダー
 * MethodHandlesを使用した高速データ取得を実現
 * 
 * 【スロットマッピング】
 * - ORB_1: M&S Health（体力）
 * - ORB_1_OVERLAY: M&S Magic Shield（魔法シールド）
 * - ORB_2: M&S Mana（マナ）または Blood（ブラッド魔法）
 * - ORB_3: M&S Energy（エネルギー/スタミナ）
 * 
 * 【パフォーマンス最適化】
 * - MethodHandlesによる高速リフレクション（従来の10-100倍）
 * - シンプルなEntityDataキャッシュ
 * - エラー時は即座にフォールバック
 */
public class MineAndSlashDataProvider extends AbstractModDataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PRIORITY = 100;
    private static final long CACHE_DURATION_MS = 250;

    // 利用可能性フラグ（MethodHandlesUtilに委譲）
    private static final boolean AVAILABLE = MethodHandlesUtil.isAvailable();

    // シンプルなキャッシュ（HJUD方式）
    private WeakReference<Player> cachedPlayer = null;
    private Object cachedEntityData = null;
    private long cacheTime = 0;
    private boolean cacheValid = false;

    public MineAndSlashDataProvider() {
        // UnifiedCacheはDataTypeのUpdateFrequencyを自動的に使用
    }

    @Override
    public boolean isAvailable() {
        return AVAILABLE;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getId() {
        return "mine_and_slash";
    }

    /**
     * キャッシュされたEntityDataを取得
     * MethodHandlesを使用して高速化
     */
    private Object getCachedEntityData(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        Player cached = cachedPlayer != null ? cachedPlayer.get() : null;

        // キャッシュが有効かチェック
        if (cached == player && cachedEntityData != null && cacheValid 
                && (now - cacheTime) < CACHE_DURATION_MS) {
            return cachedEntityData;
        }

        // キャッシュ更新
        try {
            cachedEntityData = MethodHandlesUtil.loadUnit(player);
            cachedPlayer = new WeakReference<>(player);
            cacheTime = now;
            cacheValid = true;
            return cachedEntityData;
        } catch (Throwable t) {
            LOGGER.debug("Error getting M&S EntityData: {}", t.getMessage());
            cacheValid = false;
            return null;
        }
    }

    /**
     * ResourcesDataを取得
     */
    private Object getResources(Player player) {
        Object data = getCachedEntityData(player);
        if (data == null) return null;
        
        try {
            return MethodHandlesUtil.getResources(data);
        } catch (Throwable t) {
            LOGGER.debug("Error getting M&S Resources: {}", t.getMessage());
            return null;
        }
    }

    @Override
    public float getValue(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return type.getDefaultValueTyped();
        }

        try {
            return switch (type) {
                case ORB_1_CURRENT -> {
                    try {
                        float value = MethodHandlesUtil.getCurrentHealth(player);
                        LOGGER.debug("M&S ORB_1_CURRENT: raw value = {}", value);
                        if (value <= 0) {
                            LOGGER.debug("M&S health returned 0 or less, using vanilla fallback: {}", player.getHealth());
                            value = player.getHealth();
                        }
                        yield value;
                    } catch (Throwable t) {
                        LOGGER.debug("Failed to get M&S health, using vanilla fallback: {}", t.getMessage());
                        yield player.getHealth();
                    }
                }
                
                case ORB_1_MAX -> {
                    try {
                        float value = MethodHandlesUtil.getMaxHealth(player);
                        LOGGER.debug("M&S ORB_1_MAX: raw value = {}", value);
                        yield value;
                    } catch (Throwable t) {
                        LOGGER.debug("Failed to get M&S max health: {}", t.getMessage());
                        yield 1f;
                    }
                }
                
                case ORB_1_OVERLAY_CURRENT -> {
                    Object resources = getResources(player);
                    if (resources == null) yield 0f;
                    try {
                        yield MethodHandlesUtil.getMagicShield(resources);
                    } catch (Throwable t) {
                        yield 0f;
                    }
                }
                
                case ORB_2_CURRENT -> {
                    Object resources = getResources(player);
                    if (resources == null) yield 0f;
                    
                    boolean isBlood = getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey());
                    try {
                        yield isBlood 
                            ? MethodHandlesUtil.getBlood(resources)
                            : MethodHandlesUtil.getMana(resources);
                    } catch (Throwable t) {
                        yield 0f;
                    }
                }
                
                case ORB_3_CURRENT -> {
                    Object resources = getResources(player);
                    if (resources == null) yield 0f;
                    try {
                        yield MethodHandlesUtil.getEnergy(resources);
                    } catch (Throwable t) {
                        yield 0f;
                    }
                }
                
                case LEVEL -> {
                    Object data = getCachedEntityData(player);
                    if (data == null) yield 0f;
                    try {
                        yield MethodHandlesUtil.getLevel(data);
                    } catch (Throwable t) {
                        yield 0f;
                    }
                }
                
                case EXP -> {
                    Object data = getCachedEntityData(player);
                    if (data == null) yield 0f;
                    try {
                        yield MethodHandlesUtil.getExp(data);
                    } catch (Throwable t) {
                        yield 0f;
                    }
                }
                
                default -> type.getDefaultValueTyped();
            };
        } catch (Exception e) {
            LOGGER.debug("Error getting M&S value for {}: {}", type.getKey(), e.getMessage());
            return type.getDefaultValueTyped();
        }
    }

    @Override
    public float getMaxValue(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return 1f;
        }

        Object data = getCachedEntityData(player);
        if (data == null) {
            return 1f;
        }

        try {
            return switch (type) {
                case ORB_1_MAX -> {
                    try {
                        yield MethodHandlesUtil.getMaxHealth(player);
                    } catch (Throwable t) {
                        yield 1f;
                    }
                }
                
                case ORB_1_OVERLAY_MAX -> {
                    try {
                        yield MethodHandlesUtil.getMaximumResource(data, MethodHandlesUtil.getMagicShieldType());
                    } catch (Throwable t) {
                        yield 1f;
                    }
                }
                
                case ORB_2_MAX -> {
                    boolean isBlood = getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey());
                    Object resourceType = isBlood 
                        ? MethodHandlesUtil.getBloodType() 
                        : MethodHandlesUtil.getManaType();
                    
                    if (resourceType == null) yield 1f;
                    
                    try {
                        yield MethodHandlesUtil.getMaximumResource(data, resourceType);
                    } catch (Throwable t) {
                        yield 1f;
                    }
                }
                
                case ORB_3_MAX -> {
                    Object resourceType = MethodHandlesUtil.getEnergyType();
                    if (resourceType == null) yield 1f;
                    
                    try {
                        yield MethodHandlesUtil.getMaximumResource(data, resourceType);
                    } catch (Throwable t) {
                        yield 1f;
                    }
                }
                
                case EXP_REQUIRED -> {
                    try {
                        yield MethodHandlesUtil.getExpRequired(data);
                    } catch (Throwable t) {
                        yield 1f;
                    }
                }
                
                default -> type.getDefaultValueTyped();
            };
        } catch (Exception e) {
            LOGGER.debug("Error getting M&S max value for {}: {}", type.getKey(), e.getMessage());
            return 1f;
        }
    }

    @Override
    public boolean getAttribute(Player player, String attributeKey) {
        if (!isAvailable() || player == null) {
            return false;
        }

        if (DataType.ORB_2_IS_BLOOD.getKey().equals(attributeKey)) {
            Object data = getCachedEntityData(player);
            if (data == null) return false;
            
            try {
                Object unit = MethodHandlesUtil.getUnit(data);
                if (unit == null) return false;
                return MethodHandlesUtil.isBloodMage(unit);
            } catch (Throwable t) {
                LOGGER.debug("Error checking blood mage status: {}", t.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * M&SデータをModDataとして取得
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
    
    /**
     * キャッシュを無効化
     */
    public void invalidateCache() {
        cacheValid = false;
        cachedEntityData = null;
        cachedPlayer = null;
    }
}
