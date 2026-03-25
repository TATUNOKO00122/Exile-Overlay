package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * バニラMinecraft用のデータプロバイダー
 * AbstractModDataProviderを継承し、キャッシュとエラーハンドリングを統一
 * 常に利用可能で、最も低い優先度を持つ
 *
 * 【スロットマッピング】
 * - ORB_1: Health（体力）
 * - ORB_1_OVERLAY: なし（バニラにはシールド相当がない）
 * - ORB_2: Food Level（空腹度）
 * - ORB_3: Air Supply（酸素量）
 * - ORB_4: 将来の拡張用
 */
public class VanillaDataProvider extends AbstractModDataProvider {

    public static final VanillaDataProvider INSTANCE = new VanillaDataProvider();

    private static final int PRIORITY = 0; // 最低優先度

    public VanillaDataProvider() {
        // バニラは直接取得で十分高速
    }

    @Override
    public boolean isAvailable() {
        // バニラは常に利用可能
        return true;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getId() {
        return "vanilla";
    }

    // ========== 汎用データ取得の実装 ==========
    @Override
    public float getValue(Player player, DataType type) {
        return fetchFloat(player, type, p -> {
            if (p == null)
                return 0f;
            return switch (type) {
                case ORB_1_CURRENT -> p.getHealth();
                case ORB_2_CURRENT -> (float) p.getFoodData().getFoodLevel();  // 空腹度
                case ORB_3_CURRENT -> (float) p.getAirSupply();               // 酸素量
                case LEVEL -> (float) p.experienceLevel;
                case EXP -> p.experienceProgress;
                default -> (Float) type.getDefaultValue();
            };
        });
    }

    @Override
    public float getMaxValue(Player player, DataType type) {
        return fetchFloat(player, type, p -> {
            if (p == null)
                return 1f;
            return switch (type) {
                case ORB_1_MAX -> p.getMaxHealth();
                case ORB_2_MAX -> 20.0f;  // 空腹度の最大値
                case ORB_3_MAX -> (float) p.getMaxAirSupply();  // 最大酸素量
                case EXP_REQUIRED -> 1.0f;
                default -> (Float) type.getDefaultValue();
            };
        });
    }

    /**
     * バニラプロバイダーからModDataを作成
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
}
