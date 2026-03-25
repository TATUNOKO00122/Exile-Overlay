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
     * 更新頻度: CRITICAL（毎フレーム）
     */
    ORB_1_CURRENT("orb1.current", 0.0f, Float.class, UpdateFrequency.CRITICAL),
    /**
     * ORB_1の最大値（デフォルト: 最大HP）
     * 更新頻度: STATIC（頻繁に変わらない）
     */
    ORB_1_MAX("orb1.max", 1.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== ORB 1 OVERLAY: ORB_1上のオーバーレイ ==========
    /**
     * ORB_1_OVERLAYの現在値（デフォルト: Magic Shield）
     * 更新頻度: CRITICAL（毎フレーム）
     */
    ORB_1_OVERLAY_CURRENT("orb1_overlay.current", 0.0f, Float.class, UpdateFrequency.CRITICAL),
    /**
     * ORB_1_OVERLAYの最大値（デフォルト: 最大Magic Shield）
     * 更新頻度: STATIC（頻繁に変わらない）
     */
    ORB_1_OVERLAY_MAX("orb1_overlay.max", 0.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== ORB 2: 右下メインスロット ==========
    /**
     * ORB_2の現在値（デフォルト: Mana / Blood）
     * 更新頻度: NORMAL（通常）
     */
    ORB_2_CURRENT("orb2.current", 0.0f, Float.class, UpdateFrequency.NORMAL),
    /**
     * ORB_2の最大値（デフォルト: 最大Mana / Blood）
     * 更新頻度: STATIC（頻繁に変わらない）
     */
    ORB_2_MAX("orb2.max", 1.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== ORB 3: 左上サブスロット ==========
    /**
     * ORB_3の現在値（デフォルト: Energy / Stamina）
     * 更新頻度: NORMAL（通常）
     */
    ORB_3_CURRENT("orb3.current", 0.0f, Float.class, UpdateFrequency.NORMAL),
    /**
     * ORB_3の最大値（デフォルト: 最大Energy / Stamina）
     * 更新頻度: STATIC（頻繁に変わらない）
     */
    ORB_3_MAX("orb3.max", 0.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== 追加スロット（将来的な拡張用） ==========
    /**
     * ORB_4の現在値（将来の拡張用）
     * 更新頻度: NORMAL（通常）
     */
    ORB_4_CURRENT("orb4.current", 0.0f, Float.class, UpdateFrequency.NORMAL),
    /**
     * ORB_4の最大値（将来の拡張用）
     * 更新頻度: STATIC（頻繁に変わらない）
     */
    ORB_4_MAX("orb4.max", 0.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== その他のデータ ==========
    /**
     * プレイヤーレベル
     * 更新頻度: SLOW（レベルは頻繁に変わらない）
     */
    LEVEL("level", 1, Integer.class, UpdateFrequency.SLOW),
    /**
     * 現在の経験値
     * 更新頻度: NORMAL（通常）
     */
    EXP("exp.current", 0.0f, Float.class, UpdateFrequency.NORMAL),
    /**
     * 次のレベルアップに必要な経験値
     * 更新頻度: STATIC（レベルアップ時のみ変化）
     */
    EXP_REQUIRED("exp.required", 1.0f, Float.class, UpdateFrequency.STATIC),
    
    // ========== 特殊フラグ ==========
    /**
     * ORB_2がBlood（血魔法）モードかどうか
     * 更新頻度: STATIC（クラス変更時のみ）
     */
    ORB_2_IS_BLOOD("orb2.is_blood", false, Boolean.class, UpdateFrequency.STATIC),
    
    // ========== Dungeon Realm データ ==========
    DUNGEON_KILL_PERCENT("dungeon.kill_percent", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_LOOT_PERCENT("dungeon.loot_percent", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_RARITY_TIER("dungeon.rarity_tier", 0, Integer.class, UpdateFrequency.SLOW),
    DUNGEON_RARITY_NAME("dungeon.rarity_name", "", String.class, UpdateFrequency.SLOW),
    DUNGEON_MOB_KILLS("dungeon.mob_kills", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_ELITE_KILLS("dungeon.elite_kills", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_MINIBOSS_KILLS("dungeon.miniboss_kills", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_MOB_SPAWN_COUNT("dungeon.mob_spawn_count", 0, Integer.class, UpdateFrequency.SLOW),
    DUNGEON_ELITE_SPAWN_COUNT("dungeon.elite_spawn_count", 0, Integer.class, UpdateFrequency.SLOW),
    DUNGEON_MINIBOSS_SPAWN_COUNT("dungeon.miniboss_spawn_count", 0, Integer.class, UpdateFrequency.SLOW),
    DUNGEON_CHESTS_LOOTED("dungeon.chests_looted", 0, Integer.class, UpdateFrequency.NORMAL),
    DUNGEON_CHESTS_TOTAL("dungeon.chests_total", 0, Integer.class, UpdateFrequency.SLOW),
    DUNGEON_IS_UBER("dungeon.is_uber", false, Boolean.class, UpdateFrequency.STATIC),
    DUNGEON_ID("dungeon.id", "", String.class, UpdateFrequency.STATIC),
    DUNGEON_IS_INSIDE("dungeon.is_inside", false, Boolean.class, UpdateFrequency.NORMAL);
    
    private final String key;
    private final Object defaultValue;
    private final Class<?> type;
    private final UpdateFrequency updateFrequency;
    
    DataType(String key, Object defaultValue, Class<?> type, UpdateFrequency updateFrequency) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.type = type;
        this.updateFrequency = updateFrequency;
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

    /**
     * 更新頻度を取得
     *
     * @return このデータタイプの更新頻度
     */
    public UpdateFrequency getUpdateFrequency() {
        return updateFrequency;
    }

    /**
     * このデータタイプが数値型かどうか
     *
     * @return Float, Integer, Doubleなどの数値型の場合true
     */
    public boolean isNumeric() {
        return type == Float.class || type == Integer.class || type == Double.class;
    }

    /**
     * 現在値に対応する最大値のデータタイプを取得
     * 例: ORB_1_CURRENT → ORB_1_MAX
     *
     * @return 対応する最大値のデータタイプ、存在しない場合はnull
     */
    public DataType getMaxValueType() {
        return switch (this) {
            case ORB_1_CURRENT -> ORB_1_MAX;
            case ORB_1_OVERLAY_CURRENT -> ORB_1_OVERLAY_MAX;
            case ORB_2_CURRENT -> ORB_2_MAX;
            case ORB_3_CURRENT -> ORB_3_MAX;
            case ORB_4_CURRENT -> ORB_4_MAX;
            default -> null;
        };
    }
}
