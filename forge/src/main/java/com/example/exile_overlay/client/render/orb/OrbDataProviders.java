package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.config.OrbColorConfig;
import com.example.exile_overlay.client.render.resource.ResourceCandidate;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.OrbTextConfig;

/**
 * オーブのデータプロバイダー実装。
 * ModDataProviderRegistryの汎用メソッドをOrbDataProviderインターフェースに適合させるブリッジ層。
 * ResourceSlotManagerで複数リソース候補から自動選択する動的なデータ提供。
 */
public class OrbDataProviders {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;

    /**
     * オーブの入れ替え状態（マナとエネルギー）を判定
     */
    public static boolean isSwapped(Player player) {
        OrbTextConfig.OrbResourceSwapMode mode = OrbTextConfig.getInstance().getOrbSwapMode();
        if (mode == OrbTextConfig.OrbResourceSwapMode.SWAPPED) {
            return true;
        }
        if (mode == OrbTextConfig.OrbResourceSwapMode.AUTO && player != null) {
            float manaMax = ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
            float energyMax = ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX);
            if (energyMax > manaMax) {
                return true;
            } else if (energyMax < manaMax) {
                return false;
            } else {
                float manaCur = ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                float energyCur = ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT);
                return energyCur > manaCur;
            }
        }
        if (mode == OrbTextConfig.OrbResourceSwapMode.SKILL_COST && player != null) {
            MethodHandlesUtil.HotbarSkillCosts costs = MethodHandlesUtil.getTotalHotbarSkillCosts(player);
            return costs.totalEnergy() > costs.totalMana();
        }
        return false;
    }

    // ========== ORB 1: 左下メインスロット（Health）==========
    public static final OrbDataProvider ORB_1 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            return ResourceSlotManager.getInstance().getCurrentValue("orb1", player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ResourceSlotManager.getInstance().getMaxValue("orb1", player);
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
            return ResourceSlotManager.getInstance().getCurrentValue("orb1_overlay", player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ResourceSlotManager.getInstance().getMaxValue("orb1_overlay", player);
        }

        @Override
        public boolean shouldShowValue() {
            return false;
        }
    };

    // ========== ORB 2: 右下メインスロット（動的リソース）==========
    public static final OrbDataProvider ORB_2 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            String slotId = isSwapped(player) ? "orb3" : "orb2";
            return ResourceSlotManager.getInstance().getCurrentValue(slotId, player);
        }

        @Override
        public float getMaxValue(Player player) {
            String slotId = isSwapped(player) ? "orb3" : "orb2";
            return ResourceSlotManager.getInstance().getMaxValue(slotId, player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }
    };

    // ========== ORB 2 BLOOD MODE: Blood魔法時のORB_2（廃止予定）==========
    // ResourceSlotManagerによる自動切り替えに移行
    @Deprecated
    public static final OrbDataProvider ORB_2_BLOOD = ORB_2;

    // ========== ORB 3: 左上サブスロット（動的リソース）==========
    public static final OrbDataProvider ORB_3 = new OrbDataProvider() {
        @Override
        public float getCurrentValue(Player player) {
            String slotId = isSwapped(player) ? "orb2" : "orb3";
            return ResourceSlotManager.getInstance().getCurrentValue(slotId, player);
        }

        @Override
        public float getMaxValue(Player player) {
            String slotId = isSwapped(player) ? "orb2" : "orb3";
            return ResourceSlotManager.getInstance().getMaxValue(slotId, player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }

        @Override
        public float getTextScale() {
            return 0.5f;
        }
    };

    /**
     * デフォルトのリソース候補を登録
     * Mod初期化時に呼び出してください
     */
    public static void initializeDefaults() {
        if (initialized) {
            return;
        }

        LOGGER.info("Initializing default resource candidates");
        ResourceSlotManager manager = ResourceSlotManager.getInstance();
        OrbColorConfig colorConfig = OrbColorConfig.getInstance();

        // ORB_1: Health（バニラ固定）
        manager.registerCandidate("orb1", new ResourceCandidate(
                "health",
                "Health",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_1_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_MAX),
                colorConfig::getHealthColor,
                true
        ));

        // ORB_1_OVERLAY: Shield
        manager.registerCandidate("orb1_overlay", new ResourceCandidate(
                "shield",
                "Shield",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX),
                colorConfig::getShieldColor,
                false
        ));

        // ORB_2: マナ（デフォルト）→ 血 → その他
        manager.registerCandidate("orb2", new ResourceCandidate(
                "mana",
                "Mana",
                (player, type) -> {
                    if (!net.minecraftforge.fml.ModList.get().isLoaded("mmorpg")) {
                        return 0.0f;
                    }
                    // Bloodモードでない場合のみ有効
                    if (ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                },
                (player, type) -> {
                    if (!net.minecraftforge.fml.ModList.get().isLoaded("mmorpg")) {
                        return 0.0f;
                    }
                    if (ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
                },
                colorConfig::getManaColor,
                true
        ));

        manager.registerCandidate("orb2", new ResourceCandidate(
                "blood",
                "Blood",
                (player, type) -> {
                    if (!net.minecraftforge.fml.ModList.get().isLoaded("mmorpg")) {
                        return 0.0f;
                    }
                    // Bloodモードの場合のみ有効
                    if (!ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                },
                (player, type) -> {
                    if (!net.minecraftforge.fml.ModList.get().isLoaded("mmorpg")) {
                        return 0.0f;
                    }
                    if (!ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
                },
                colorConfig::getBloodColor,
                true
        ));

        if (net.minecraftforge.fml.ModList.get().isLoaded("irons_spellbooks")) {
            manager.registerCandidate("orb2", new ResourceCandidate(
                    "irons_mana",
                    "Mana",
                    (player, type) -> com.example.exile_overlay.compat.IronsSpellbooksCompat.getCurrentMana(player),
                    (player, type) -> com.example.exile_overlay.compat.IronsSpellbooksCompat.getMaxMana(player),
                    colorConfig::getManaColor,
                    true
            ));
        }

        // バニラ満腹度（フォールバック）
        manager.registerCandidate("orb2", new ResourceCandidate(
                "food",
                "Food",
                (player, type) -> player != null ? (float) player.getFoodData().getFoodLevel() : 0.0f,
                (player, type) -> 20.0f,
                colorConfig::getFoodColor,
                true
        ));

        // ORB_3: エネルギー → スタミナ → その他
        manager.registerCandidate("orb3", new ResourceCandidate(
                "energy",
                "Energy",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX),
                colorConfig::getEnergyColor,
                false
        ));

        initialized = true;
        LOGGER.info("Default resource candidates initialized");
    }

    /**
     * カスタムリソース候補を登録
     * 
     * @param slotId スロットID（"orb1", "orb2", "orb3"など）
     * @param candidate 登録するリソース候補
     */
    public static void registerCandidate(String slotId, ResourceCandidate candidate) {
        ResourceSlotManager.getInstance().registerCandidate(slotId, candidate);
        LOGGER.debug("Registered custom candidate '{}' for slot {}", candidate.getId(), slotId);
    }

    /**
     * 初期化済みかどうか
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
