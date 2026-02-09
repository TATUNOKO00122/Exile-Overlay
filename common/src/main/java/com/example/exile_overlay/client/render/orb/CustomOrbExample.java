package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * カスタムオーブの追加例（スロットベース設計）
 * 
 * 【重要】このクラスはスロットベース設計に更新されています。
 * 
 * 新しいオーブを追加する場合の実装例です。
 * HUDの「どのスロットに」データを表示するかを定義します。
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット（デフォルト: HP）
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ（デフォルト: Shield）
 * - ORB_2: 画面右下のメインスロット（デフォルト: Mana/Blood）
 * - ORB_3: 画面左上のサブスロット（デフォルト: Energy/Stamina）
 * 
 * 他のMOD（Iron's Spells, Ars Nouveau等）のリソースを表示する場合に使用します。
 */
public class CustomOrbExample {

    /**
     * カスタムオーブを作成して登録する例
     * 
     * ModInitializerやFMLJavaModLoadingContext内から呼び出してください
     */
    public static void registerCustomOrb() {
        // 例1: シンプルなオーブ（ORB_2をカスタムマナとして使用）
        OrbType customManaOrb = OrbType.register("custom_mana",
                OrbConfig.builder("custom_mana")
                        .position(400, 150) // 画面座標
                        .size(70) // オーブサイズ
                        .color(0xFF6600FF) // 紫色
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // Ars NouveauのAPIを使用
                                // return ArsNouveauAPI.getCurrentMana(player);
                                // スロットベース: ORB_2の値を使用
                                return ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
                            }

                            @Override
                            public float getMaxValue(Player player) {
                                // return ArsNouveauAPI.getMaxMana(player);
                                return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX);
                            }

                            @Override
                            public boolean shouldShowValue() {
                                return true;
                            }
                        })
                        .visibleWhen(player -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_2_MAX) > 0)
                        .build());

        // 例2: シールドオーブ（ORB_1_OVERLAYを使用）
        OrbType spellShieldOrb = OrbType.register("spell_shield",
                OrbConfig.builder("spell_shield")
                        .position(200, 180)
                        .size(60)
                        .color(0xFF00AAAA) // シアン色
                        .overlayColor(0x8000FFFF)
                        .showReflection(true)
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // Iron's SpellsのAPIを使用
                                // スロットベース: ORB_1_OVERLAYの値を使用
                                return ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT);
                            }

                            @Override
                            public float getMaxValue(Player player) {
                                return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX);
                            }
                        })
                        .visibleWhen(
                                player -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX) > 10)
                        .build());

        // 例3: スタミナオーブ（ORB_3を使用）
        OrbType staminaOrb = OrbType.create("stamina",
                OrbConfig.builder("stamina")
                        .position(300, 200)
                        .size(50)
                        .color(0xFFFFAA00) // オレンジ色
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // カスタムデータ取得ロジック
                                // スロットベース: ORB_3の値を使用
                                return player.isSprinting()
                                        ? ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT) * 0.5f
                                        : ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT);
                            }

                            @Override
                            public float getMaxValue(Player player) {
                                return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX);
                            }

                            @Override
                            public boolean shouldShowValue() {
                                return false;
                            }
                        })
                        .visibleWhen(player -> ModDataProviderRegistry.getMaxValue(player, DataType.ORB_3_MAX) > 0)
                        .build());

        // 手動で登録することも可能
        OrbRegistry.register(staminaOrb);
    }

    /**
     * 既存のオーブを修正・カスタマイズする例（スロットベース）
     */
    public static void customizeExistingOrbs() {
        // デフォルトのORB_2（Mana/Blood）を解除
        OrbRegistry.unregister(OrbType.ORB_2);

        // 新しい設定で再登録
        OrbType customOrb2 = OrbType.register("custom_orb_2",
                OrbConfig.builder("custom_orb_2")
                        .position(511, 199)
                        .size(85)
                        .color(0xFFFF00FF) // マゼンタ色に変更
                        .dataProvider(OrbDataProviders.ORB_2)
                        .visibleWhen(p -> !ModDataProviderRegistry.getAttribute(p, DataType.ORB_2_IS_BLOOD.getKey()))
                        .build());
    }

    /**
     * 条件付きでオーブを表示する例（スロットベース）
     */
    public static void registerConditionalOrb() {
        OrbType bossHealthOrb = OrbType.register("boss_health",
                OrbConfig.builder("boss_health")
                        .position(320, 50)
                        .size(100)
                        .color(0xFFFF0000)
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // スロットベース: ORB_1の値を使用（Health）
                                return ModDataProviderRegistry.getValue(player, DataType.ORB_1_CURRENT);
                            }

                            @Override
                            public float getMaxValue(Player player) {
                                return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_MAX);
                            }
                        })
                        .visibleWhen(player -> {
                            // ボス戦中のみ表示（例：エンダードラゴンが近くにいる場合）
                            var nearbyDragons = player.level().getEntities(
                                    player,
                                    player.getBoundingBox().inflate(100),
                                    entity -> entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon);
                            return !nearbyDragons.isEmpty();
                        })
                        .build());
    }
}
