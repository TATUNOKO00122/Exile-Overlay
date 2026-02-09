package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.render.resource.ResourceCandidate;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * オーブのデータプロバイダー実装
 * 
 * 【スロットベース設計】
 * このクラスは、ModDataProviderRegistryの汎用メソッドを
 * OrbDataProviderインターフェースに適合させるブリッジ層です。
 * 
 * 【ResourceSlotManager統合】
 * ResourceSlotManagerを使用して、複数のリソース候補から自動的に
 * 有効なリソースを選択する動的なデータ提供を実現します。
 */
public class OrbDataProviders {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean initialized = false;

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
            return ResourceSlotManager.getInstance().getCurrentValue("orb2", player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ResourceSlotManager.getInstance().getMaxValue("orb2", player);
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
            return ResourceSlotManager.getInstance().getCurrentValue("orb3", player);
        }

        @Override
        public float getMaxValue(Player player) {
            return ResourceSlotManager.getInstance().getMaxValue("orb3", player);
        }

        @Override
        public boolean shouldShowValue() {
            return true;
        }

        @Override
        public float getTextScale() {
            return 0.7f;
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

        // ORB_1: Health（バニラ固定）
        manager.registerCandidate("orb1", new ResourceCandidate(
                "health",
                "Health",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_1_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_MAX),
                0xFFFF0000, // 赤
                true
        ));

        // ORB_1_OVERLAY: Shield
        manager.registerCandidate("orb1_overlay", new ResourceCandidate(
                "shield",
                "Shield",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX),
                0xB000FFFF, // シアン
                false
        ));

        // ORB_2: マナ（デフォルト）→ 血 → その他
        manager.registerCandidate("orb2", new ResourceCandidate(
                "mana",
                "Mana",
                (player, type) -> {
                    // Bloodモードでない場合のみ有効
                    if (ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                },
                (player, type) -> {
                    if (ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
                },
                0xFF2222FF, // 青
                true
        ));

        manager.registerCandidate("orb2", new ResourceCandidate(
                "blood",
                "Blood",
                (player, type) -> {
                    // Bloodモードの場合のみ有効
                    if (!ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                },
                (player, type) -> {
                    if (!ModDataProviderRegistry.getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey())) {
                        return 0.0f;
                    }
                    return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
                },
                0xFFCC0000, // 赤
                true
        ));

        // ORB_3: エネルギー → スタミナ → その他
        manager.registerCandidate("orb3", new ResourceCandidate(
                "energy",
                "Energy",
                (player, type) -> ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT),
                (player, type) -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX),
                0xFF00CC00, // 緑
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
