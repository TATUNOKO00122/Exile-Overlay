package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * オーブのデータプロバイダー実装
 * ModDataProviderRegistryを通じて複数MODに対応
 */
public class OrbDataProviders {

    public static final OrbDataProvider HEALTH = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getCurrentHealth(player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxHealth(player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    public static final OrbDataProvider MAGIC_SHIELD = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getCurrentMagicShield(player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxMagicShield(player);
        }

        @Override
        public boolean shouldShowValue() {
            return false;
        }
    };

    public static final OrbDataProvider ENERGY = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getCurrentEnergy(player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxEnergy(player);
        }

        @Override
        public boolean shouldShowValue() {
            return false;
        }
    };

    public static final OrbDataProvider MANA = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getCurrentMana(player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxMana(player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    public static final OrbDataProvider BLOOD = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ModDataProviderRegistry.getCurrentBlood(player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxBlood(player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };
}
