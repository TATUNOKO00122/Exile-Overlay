package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import net.minecraft.world.entity.player.Player;

import java.util.function.Predicate;

/**
 * HUDオーブスロットの種類を定義するクラス
 * 
 * 【スロットベース設計】
 * このクラスでは、HUD上の物理的なスロット位置に基づいてオーブを定義します。
 * 各スロットが「何を」表示するかは、データプロバイダーが決定します。
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ
 * - ORB_2: 画面右下のメインスロット
 * - ORB_3: 画面左上のサブスロット
 */
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * HUDオーブスロットの種類を定義するクラス
 * 
 * 【スロットベース設計】
 * このクラスでは、HUD上の物理的なスロット位置に基づいてオーブを定義します。
 * 各スロットが「何を」表示するかは、データプロバイダーが決定します。
 * 
 * 【パフォーマンス最適化】
 * - values()で変更不可リストを返し、反復処理を高速化
 * - static finalインスタンスでメモリ効率を最適化
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ
 * - ORB_2: 画面右下のメインスロット
 * - ORB_3: 画面左上のサブスロット
 */
public final class OrbType {
    
    // 全オーブタイプのリスト（反復処理用）
    private static final List<OrbType> ALL_TYPES;

    // ========== ORB 1: 左下メインスロット（デフォルト: Health）==========
    // 常に表示（データがなくても背景として表示する）
    public static final OrbType ORB_1 = create("orb_1",
            OrbConfig.builder("orb_1")
                    .position(129, 199)
                    .size(85)
                    .color(0xFFFF0000) // 赤（デフォルト: HP）
                    .withOverlay(0xB000FFFF) // シアン（シールドの色）
                    .dataProvider(OrbDataProviders.ORB_1)
                    .visibleWhen(p -> true) // 常に表示
                    .build());

    // ========== ORB 1 OVERLAY: ORB_1上のオーバーレイ（デフォルト: Shield）==========
    // シールドデータがある場合のみ表示
    public static final OrbType ORB_1_OVERLAY = create("orb_1_overlay",
            OrbConfig.builder("orb_1_overlay")
                    .position(129, 199)
                    .size(85)
                    .color(0xB000FFFF) // シアン（シールド）
                    .showReflection(false)
                    .dataProvider(OrbDataProviders.ORB_1_OVERLAY)
                    .asOverlayFor("orb_1", OrbDataProviders.ORB_1_OVERLAY)
                    .visibleWhen(p -> ModDataProviderRegistry.getMaxValue(p, DataType.ORB_1_OVERLAY_MAX) > 0)
                    .build());

    // ========== ORB 2: 右下メインスロット（マナ/血/空腹度）==========
    // 常に表示（データがなくても背景として表示する）
    public static final OrbType ORB_2 = create("orb_2",
            OrbConfig.builder("orb_2")
                    .position(511, 199)
                    .size(85)
                    .color(0xFF0000FF) // 青（マナの色）
                    .dataProvider(OrbDataProviders.ORB_2)
                    .visibleWhen(p -> true) // 常に表示
                    .build());

    // ========== ORB 3: 左上サブスロット（エネルギー/酸素）==========
    // 常に表示（データがなくても背景として表示する）
    public static final OrbType ORB_3 = create("orb_3",
            OrbConfig.builder("orb_3")
                    .position(197, 188)
                    .size(36)
                    .color(0xFFFFFF00) // 黄（エネルギーの色）
                    .dataProvider(OrbDataProviders.ORB_3)
                    .visibleWhen(p -> true) // 常に表示
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
