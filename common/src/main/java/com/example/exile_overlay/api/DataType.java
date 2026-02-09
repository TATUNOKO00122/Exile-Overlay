package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import java.util.function.BiFunction;

/**
 * MODデータの種類を定義するenum
 * 新しいデータタイプを追加する場合はここに定義
 */
public enum DataType {
    // 基本ステータス
    CURRENT_HEALTH("health.current", 0.0f, Float.class),
    MAX_HEALTH("health.max", 1.0f, Float.class),
    CURRENT_MANA("mana.current", 0.0f, Float.class),
    MAX_MANA("mana.max", 1.0f, Float.class),
    
    // 拡張ステータス
    CURRENT_MAGIC_SHIELD("magic_shield.current", 0.0f, Float.class),
    MAX_MAGIC_SHIELD("magic_shield.max", 0.0f, Float.class),
    CURRENT_ENERGY("energy.current", 0.0f, Float.class),
    MAX_ENERGY("energy.max", 0.0f, Float.class),
    CURRENT_BLOOD("blood.current", 0.0f, Float.class),
    MAX_BLOOD("blood.max", 0.0f, Float.class),
    
    // 経験値・レベル
    LEVEL("level", 1, Integer.class),
    EXP("exp.current", 0.0f, Float.class),
    EXP_REQUIRED("exp.required", 1.0f, Float.class),
    
    // 特殊フラグ
    BLOOD_MAGIC_ACTIVE("blood_magic.active", false, Boolean.class);
    
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
