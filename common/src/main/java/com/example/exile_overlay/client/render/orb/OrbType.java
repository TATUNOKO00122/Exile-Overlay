package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.ModDataProviderRegistry;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * オーブの種類を定義するクラス
 * データパックや他MODから動的に追加可能
 */
public final class OrbType {
    
    // デフォルトのオーブ定義
    public static final OrbType HEALTH = create("health",
            OrbConfig.builder("health")
                    .position(129, 199)
                    .size(85)
                    .color(0xFFFF0000)
                    .overlayColor(0xB000FFFF) // Magic Shield
                    .dataProvider(OrbDataProviders.HEALTH)
                    .build());
    
    public static final OrbType MAGIC_SHIELD = create("magic_shield",
            OrbConfig.builder("magic_shield")
                    .position(129, 199)
                    .size(85)
                    .color(0xB000FFFF)
                    .showReflection(false)
                    .dataProvider(OrbDataProviders.MAGIC_SHIELD)
                    .visibleWhen(p -> ModDataProviderRegistry.getMaxMagicShield(p) > 0)
                    .build());
    
    public static final OrbType ENERGY = create("energy",
            OrbConfig.builder("energy")
                    .position(177, 169)
                    .size(38)
                    .color(0xFF00CC00)
                    .dataProvider(OrbDataProviders.ENERGY)
                    .visibleWhen(p -> ModDataProviderRegistry.getMaxEnergy(p) > 0)
                    .build());
    
    public static final OrbType MANA = create("mana",
            OrbConfig.builder("mana")
                    .position(511, 199)
                    .size(85)
                    .color(0xFF2222FF)
                    .dataProvider(OrbDataProviders.MANA)
                    .visibleWhen(p -> !ModDataProviderRegistry.isBloodMagicActive(p))
                    .build());
    
    public static final OrbType BLOOD = create("blood",
            OrbConfig.builder("blood")
                    .position(511, 199)
                    .size(85)
                    .color(0xFFCC0000)
                    .dataProvider(OrbDataProviders.BLOOD)
                    .visibleWhen(ModDataProviderRegistry::isBloodMagicActive)
                    .build());
    
    private final String id;
    private final OrbConfig config;
    
    private OrbType(String id, OrbConfig config) {
        this.id = id;
        this.config = config;
    }
    
    /**
     * 新しいオーブタイプを作成する
     * 
     * @param id オーブのID
     * @param config オーブの設定
     * @return 作成されたOrbType
     */
    public static OrbType create(String id, OrbConfig config) {
        return new OrbType(id, config);
    }
    
    /**
     * 新しいオーブタイプを作成し、自動的にレジストリに登録する
     * 
     * @param id オーブのID
     * @param config オーブの設定
     * @return 作成されたOrbType
     */
    public static OrbType register(String id, OrbConfig config) {
        OrbType orbType = create(id, config);
        OrbRegistry.register(orbType);
        return orbType;
    }
    
    public String getId() {
        return id;
    }
    
    public OrbConfig getConfig() {
        return config;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrbType orbType = (OrbType) o;
        return id.equals(orbType.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "OrbType{" + id + '}';
    }
}
