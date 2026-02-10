package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Function;

/**
 * MODデータプロバイダーの抽象基底クラス
 * UnifiedCache連携、エラーハンドリング、共通処理を提供
 *
 * 【スロットベース設計について】
 * このクラスでは、HUD上の「どのスロットに」データを表示するかを定義します。
 * 具体的なデータの「意味」（HP、Manaなど）はサブクラスが決定します。
 *
 * 【アーキテクチャ変更】
 * ModDataCache → UnifiedCacheに移行
 * - フレームベースのキャッシュ管理
 * - データ型別の更新頻度サポート
 *
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ
 * - ORB_2: 画面右下のメインスロット
 * - ORB_3: 画面左上のサブスロット
 * - ORB_4: 将来の拡張用
 */
public abstract class AbstractModDataProvider implements IModDataProvider {

    protected final Logger logger = LogUtils.getLogger();

    // UnifiedCacheへの参照（ModDataCacheは廃止予定）
    protected final UnifiedCache unifiedCache = UnifiedCache.getInstance();

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
     * float値を安全に取得（UnifiedCache連携）
     *
     * @param player   プレイヤー
     * @param dataType データタイプ
     * @param fetcher  データ取得関数
     * @return 取得した値、エラー時はデフォルト値
     */
    protected float fetchFloat(Player player, DataType dataType, Function<Player, Float> fetcher) {
        if (player == null) {
            return dataType.getDefaultValueTyped();
        }

        return unifiedCache.get(player, dataType, () -> {
            try {
                Float value = fetcher.apply(player);
                return value != null ? value : dataType.getDefaultValueTyped();
            } catch (Exception e) {
                logger.debug("Error fetching {} for player {}: {}",
                        dataType.getKey(), player.getName().getString(), e.getMessage());
                return dataType.getDefaultValueTyped();
            }
        });
    }

    /**
     * int値を安全に取得（UnifiedCache連携）
     *
     * @param player   プレイヤー
     * @param dataType データタイプ
     * @param fetcher  データ取得関数
     * @return 取得した値、エラー時はデフォルト値
     */
    protected int fetchInt(Player player, DataType dataType, Function<Player, Integer> fetcher) {
        if (player == null) {
            return dataType.getDefaultValueTyped();
        }

        // UnifiedCacheはfloatを返すので、DataTypeに応じて変換
        float cached = unifiedCache.get(player, dataType, () -> {
            try {
                Integer value = fetcher.apply(player);
                return value != null ? value.floatValue() : dataType.getDefaultValueTyped();
            } catch (Exception e) {
                logger.debug("Error fetching {} for player {}: {}",
                        dataType.getKey(), player.getName().getString(), e.getMessage());
                return dataType.getDefaultValueTyped();
            }
        });

        return (int) cached;
    }

    /**
     * boolean値を安全に取得（UnifiedCache連携）
     *
     * @param player   プレイヤー
     * @param dataType データタイプ
     * @param fetcher  データ取得関数
     * @return 取得した値、エラー時はデフォルト値
     */
    protected boolean fetchBoolean(Player player, DataType dataType, Function<Player, Boolean> fetcher) {
        if (player == null) {
            return dataType.getDefaultValueTyped();
        }

        float cached = unifiedCache.get(player, dataType, () -> {
            try {
                Boolean value = fetcher.apply(player);
                return value != null ? (value ? 1.0f : 0.0f) : dataType.getDefaultValueTyped();
            } catch (Exception e) {
                logger.debug("Error fetching {} for player {}: {}",
                        dataType.getKey(), player.getName().getString(), e.getMessage());
                return dataType.getDefaultValueTyped();
            }
        });

        return cached >= 0.5f;
    }

    /**
     * 特定のデータタイプのキャッシュを無効化
     */
    protected void invalidateCache(Player player, DataType dataType) {
        unifiedCache.invalidate(player, dataType);
    }

    /**
     * プレイヤーの全キャッシュを無効化
     */
    protected void invalidateAllCache(Player player) {
        unifiedCache.invalidateAll(player);
    }

    @Override
    public float getValue(Player player, DataType type) {
        return fetchFloat(player, type, p -> 0.0f);
    }

    @Override
    public float getMaxValue(Player player, DataType type) {
        return fetchFloat(player, type, p -> type.getDefaultValueTyped());
    }

    @Override
    public boolean getAttribute(Player player, String attributeKey) {
        DataType type = DataType.fromKey(attributeKey);
        if (type != null && type.getType() == Boolean.class) {
            return fetchBoolean(player, type, p -> (Boolean) type.getDefaultValue());
        }
        return false;
    }

    @Override
    public int getInt(Player player, DataType type) {
        return fetchInt(player, type, p -> (int) getValue(player, type));
    }
}
