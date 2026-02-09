package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * カスタムオーブの追加例
 * 
 * 新しいオーブを追加する場合の実装例です。
 * 他のMOD（Iron's Spells, Ars Nouveau等）のリソースを表示する場合に使用します。
 */
public class CustomOrbExample {
    
    /**
     * カスタムオーブを作成して登録する例
     * 
     * ModInitializerやFMLJavaModLoadingContext内から呼び出してください
     */
    public static void registerCustomOrb() {
        // 例1: シンプルなマナオーブ（Ars Nouveau風）
        OrbType arsManaOrb = OrbType.register("ars_mana",
                OrbConfig.builder("ars_mana")
                        .position(400, 150)  // 画面座標
                        .size(70)            // オーブサイズ
                        .color(0xFF6600FF)   // 紫色
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // Ars NouveauのAPIを使用
                                // return ArsNouveauAPI.getCurrentMana(player);
                                return ModDataProviderRegistry.getCurrentMana(player);
                            }
                            
                            @Override
                            public float getMaxValue(Player player) {
                                // return ArsNouveauAPI.getMaxMana(player);
                                return ModDataProviderRegistry.getMaxMana(player);
                            }
                            
                            @Override
                            public boolean shouldShowValue() {
                                return true;
                            }
                        })
                        .visibleWhen(player -> ModDataProviderRegistry.getMaxMana(player) > 0)
                        .build());
        
        // 例2: シールドオーブ（Iron's Spells風）
        OrbType spellShieldOrb = OrbType.register("spell_shield",
                OrbConfig.builder("spell_shield")
                        .position(200, 180)
                        .size(60)
                        .color(0xFF00AAAA)   // シアン色
                        .overlayColor(0x8000FFFF)
                        .showReflection(true)
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // Iron's SpellsのAPIを使用
                                return ModDataProviderRegistry.getCurrentMagicShield(player);
                            }
                            
                            @Override
                            public float getMaxValue(Player player) {
                                return ModDataProviderRegistry.getMaxMagicShield(player);
                            }
                        })
                        .visibleWhen(player -> ModDataProviderRegistry.getMaxMagicShield(player) > 10)
                        .build());
        
        // 例3: スタミナオーブ（カスタムシステム）
        OrbType staminaOrb = OrbType.create("stamina",
                OrbConfig.builder("stamina")
                        .position(300, 200)
                        .size(50)
                        .color(0xFFFFAA00)   // オレンジ色
                        .dataProvider(new OrbDataProvider() {
                            @Override
                            public float getCurrentValue(Player player) {
                                // カスタムデータ取得ロジック
                                return player.isSprinting() ? 50 : 100;
                            }
                            
                            @Override
                            public float getMaxValue(Player player) {
                                return 100;
                            }
                            
                            @Override
                            public boolean shouldShowValue() {
                                return false;
                            }
                        })
                        .build());
        
        // 手動で登録することも可能
        OrbRegistry.register(staminaOrb);
    }
    
    /**
     * 既存のオーブを修正・カスタマイズする例
     */
    public static void customizeExistingOrbs() {
        // デフォルトのマナオーブを解除
        OrbRegistry.unregister(OrbType.MANA);
        
        // 新しい設定で再登録
        OrbType customMana = OrbType.register("custom_mana",
                OrbConfig.builder("custom_mana")
                        .position(511, 199)
                        .size(85)
                        .color(0xFFFF00FF)   // マゼンタ色に変更
                        .dataProvider(OrbDataProviders.MANA)
                        .visibleWhen(p -> !ModDataProviderRegistry.isBloodMagicActive(p))
                        .build());
    }
    
    /**
     * 条件付きでオーブを表示する例
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
                                return ModDataProviderRegistry.getCurrentHealth(player);
                            }
                            
                            @Override
                            public float getMaxValue(Player player) {
                                return ModDataProviderRegistry.getMaxHealth(player);
                            }
                        })
                        .visibleWhen(player -> {
                            // ボス戦中のみ表示（例：エンダードラゴンが近くにいる場合）
                            var nearbyDragons = player.level().getEntities(
                                    player, 
                                    player.getBoundingBox().inflate(100),
                                    entity -> entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                            );
                            return !nearbyDragons.isEmpty();
                        })
                        .build());
    }
}
