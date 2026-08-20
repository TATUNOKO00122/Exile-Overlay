package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.config.OrbColorConfig;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

import java.util.function.Predicate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HUDオーブスロットの種類を定義するクラス。
 * 各スロットが「何を」表示するかはデータプロバイダーが決定する。
 * ORB_1: 左下メイン、ORB_1_OVERLAY: ORB_1重なり、ORB_2: 右下メイン、ORB_3: 左上サブ。
 * values()で変更不可リストを返し、static finalインスタンスでメモリ効率を最適化。
 */
public final class OrbType {
    
    // 全オーブタイプのリスト（反復処理用）
    private static final List<OrbType> ALL_TYPES;

    // ========== ORB 1: 左下メインスロット（デフォルト: Health）==========
    // 常に表示（データがなくても背景として表示する）
    public static final OrbType ORB_1 = create("orb_1",
            OrbConfig.builder("orb_1")
                    .position(99, 196)
                    .size(84)
                    .color(OrbColorConfig.DEFAULT_HEALTH_COLOR) // デフォルト: HP
                    .withOverlay(OrbColorConfig.DEFAULT_SHIELD_COLOR) // デフォルト: シールド
                    .dataProvider(OrbDataProviders.ORB_1)
                    .visibleWhen(p -> true) // 常に表示
                    .build());

    // ========== ORB 1 OVERLAY: ORB_1上のオーバーレイ（デフォルト: Shield）==========
    // シールドデータがある場合のみ表示
    public static final OrbType ORB_1_OVERLAY = create("orb_1_overlay",
            OrbConfig.builder("orb_1_overlay")
                    .position(99, 196)
                    .size(84)
                    .color(OrbColorConfig.DEFAULT_SHIELD_COLOR) // シールド
                    .showReflection(false)
                    .dataProvider(OrbDataProviders.ORB_1_OVERLAY)
                    .asOverlayFor("orb_1", OrbDataProviders.ORB_1_OVERLAY)
                    .visibleWhen(p -> ModDataProviderRegistry.getMaxValue(p, DataType.ORB_1_OVERLAY_MAX) > 0)
                    .build());

    // ========== ORB 2: 右下メインスロット（マナ/血/空腹度）==========
    // 常に表示（データがなくても背景として表示する）
    public static final OrbType ORB_2 = create("orb_2",
            OrbConfig.builder("orb_2")
                    .position(541, 196)
                    .size(84)
                    .color(OrbColorConfig.DEFAULT_MANA_COLOR) // マナの色
                    .dataProvider(OrbDataProviders.ORB_2)
                    .visibleWhen(p -> true) // 常に表示
                    .build());

    // ========== ORB 3: 左上サブスロット（エネルギー/酸素）==========
    // Iron's Spells&Spellbooks導入時は不要。かつ、最大値が0より大きい場合のみ表示
    public static final OrbType ORB_3 = create("orb_3",
            OrbConfig.builder("orb_3")
                    .position(167, 186)
                    .size(36)
                    .renderOffset(-1.3f, 0.0f)
                    .reflectionBounds(146.5f, 166.5f, 38.0f, 38.0f)
                    .color(OrbColorConfig.DEFAULT_ENERGY_COLOR) // エネルギーの色
                    .dataProvider(OrbDataProviders.ORB_3)
                    .reflectionTexture(new ResourceLocation("exile_overlay", "textures/gui/orb_reflection_3.png"))
                    .visibleWhen(p -> !ModList.get().isLoaded("irons_spellbooks") && ModDataProviderRegistry.getMaxValue(p, DataType.ORB_3_MAX) > 0)
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
     * @param id     オーブのID
     * @param config オーブの設定
     * @return 作成されたOrbType
     */
    public static OrbType create(String id, OrbConfig config) {
        return new OrbType(id, config);
    }

    /**
     * 新しいオーブタイプを作成し、自動的にレジストリに登録する
     * 
     * @param id     オーブのID
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
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
    
    /**
     * 全てのオーブタイプを取得
     * 
     * @return 変更不可のオーブタイプリスト
     */
    public static List<OrbType> values() {
        return ALL_TYPES;
    }
    
    // 静的初期化ブロック
    static {
        ALL_TYPES = Collections.unmodifiableList(Arrays.asList(
            ORB_1,
            ORB_1_OVERLAY,
            ORB_2,
            ORB_3
        ));
    }
}
