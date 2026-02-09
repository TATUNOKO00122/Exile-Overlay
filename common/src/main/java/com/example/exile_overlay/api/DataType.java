package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import java.util.function.BiFunction;

/**
 * HUDスロットに表示されるデータの種類を定義するenum
 * 
 * このenumはHUD上の「どの位置に」データを表示するかを定義します。
 * 具体的な「何を」表示するかは、各IModDataProvider実装が決定します。
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット（デフォルト: HP）
 * - ORB_1_OVERLAY: ORB_1に重なる属性（デフォルト: Shield）
 * - ORB_2: 画面右下のメインスロット（デフォルト: Mana/Blood）
 * - ORB_3: 画面左上のサブスロット（デフォルト: Stamina/Energy）
 */
public enum DataType {
    // ========== ORB 1: 左下メインスロット ==========
    /**
     * ORB_1の現在値（デフォルト: HP）
     */
    ORB_1_CURRENT("orb1.current", 0.0f, Float.class),
    /**
     * ORB_1の最大値（デフォルト: 最大HP）
     */
    ORB_1_MAX("orb1.max", 1.0f, Float.class),
    
    // ========== ORB 1 OVERLAY: ORB_1上のオーバーレイ ==========
    /**
     * ORB_1_OVERLAYの現在値（デフォルト: Magic Shield）
     */
    ORB_1_OVERLAY_CURRENT("orb1_overlay.current", 0.0f, Float.class),
    /**
     * ORB_1_OVERLAYの最大値（デフォルト: 最大Magic Shield）
     */
    ORB_1_OVERLAY_MAX("orb1_overlay.max", 0.0f, Float.class),
    
    // ========== ORB 2: 右下メインスロット ==========
    /**
     * ORB_2の現在値（デフォルト: Mana / Blood）
     */
    ORB_2_CURRENT("orb2.current", 0.0f, Float.class),
    /**
     * ORB_2の最大値（デフォルト: 最大Mana / Blood）
     */
    ORB_2_MAX("orb2.max", 1.0f, Float.class),
    
    // ========== ORB 3: 左上サブスロット ==========
    /**
     * ORB_3の現在値（デフォルト: Energy / Stamina）
     */
    ORB_3_CURRENT("orb3.current", 0.0f, Float.class),
    /**
     * ORB_3の最大値（デフォルト: 最大Energy / Stamina）
     */
    ORB_3_MAX("orb3.max", 0.0f, Float.class),
    
    // ========== 追加スロット（将来的な拡張用） ==========
    /**
     * ORB_4の現在値（将来の拡張用）
     */
    ORB_4_CURRENT("orb4.current", 0.0f, Float.class),
    /**
     * ORB_4の最大値（将来の拡張用）
     */
    ORB_4_MAX("orb4.max", 0.0f, Float.class),
    
    // ========== その他のデータ ==========
    /**
     * プレイヤーレベル
     */
    LEVEL("level", 1, Integer.class),
    /**
     * 現在の経験値
     */
    EXP("exp.current", 0.0f, Float.class),
    /**
     * 次のレベルアップに必要な経験値
     */
    EXP_REQUIRED("exp.required", 1.0f, Float.class),
    
    // ========== 特殊フラグ ==========
    /**
     * ORB_2がBlood（血魔法）モードかどうか
     */
    ORB_2_IS_BLOOD("orb2.is_blood", false, Boolean.class);
    
    private final String key;
    private final Object defaultValue;
    private final Class<?> type;
    
    DataType(String key, Object defaultValue, Class<?> type) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
    }
    
    /**
     * データのキー文字列を取得
     */
    public String getKey() {
        return key;
    }
    
    /**
     * デフォルト値を取得
     */
    public Object getDefaultValue() {
        return defaultValue;
    }
    
    /**
     * データ型を取得
     */
    public Class<?> getType() {
        return type;
    }
    
    /**
     * キー文字列からDataTypeを検索
     */
    public static DataType fromKey(String key) {
        for (DataType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * 型に応じたデフォルト値を取得
     */
    @SuppressWarnings("unchecked")
    public <T> T getDefaultValueTyped() {
        return (T) defaultValue;
    }
}
