package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * オーブのデータプロバイダー実装
 * 
 * 【スロットベース設計】
 * このクラスは、ModDataProviderRegistryの汎用メソッドを
 * OrbDataProviderインターフェースに適合させるブリッジ層です。
 */
public class OrbDataProviders {

    // ========== ORB 1: 左下メインスロット（Health）==========
    public static final OrbDataProvider ORB_1 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getValue(player, DataType.ORB_1_CURRENT);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_MAX);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    // ========== ORB 1 OVERLAY: ORB_1上のオーバーレイ（Shield）==========
    public static final OrbDataProvider ORB_1_OVERLAY = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX);
        }

        @Override
        public boolean shouldShowValue() {
            return false;
        }
    };

    // ========== ORB 2: 右下メインスロット（Mana）==========
    public static final OrbDataProvider ORB_2 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    // ========== ORB 2 BLOOD MODE: Blood魔法時のORB_2 ==========
    public static final OrbDataProvider ORB_2_BLOOD = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    // ========== ORB 3: 左上サブスロット（Energy）==========
    public static final OrbDataProvider ORB_3 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX);
        }

        @Override
        public boolean shouldShowValue() {
            return false;
        }
    };
}
