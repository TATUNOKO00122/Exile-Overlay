package com.example.exile_overlay.client.render.resource;

import com.example.exile_overlay.api.DataType;
import net.minecraft.world.entity.player.Player;

import java.util.function.BiFunction;

/**
 * スロットに表示されるリソース候補
 * 
 * 登録順が優先度となり、先に登録されたものが優先的に選択される
 */
public class ResourceCandidate {
    
    private final String id;
    private final String displayName;
    private final BiFunction<Player, DataType, Float> valueGetter;
    private final BiFunction<Player, DataType, Float> maxValueGetter;
    private final int color;
    private final boolean showValue;
    
    public ResourceCandidate(String id, String displayName, 
                            BiFunction<Player, DataType, Float> valueGetter,
                            BiFunction<Player, DataType, Float> maxValueGetter,
                            int color, boolean showValue) {
        this.id = id;
        this.displayName = displayName;
        this.valueGetter = valueGetter;
        this.maxValueGetter = maxValueGetter;
        this.color = color;
        this.showValue = showValue;
    }
    
    /**
     * リソースIDを取得
     */
    public String getId() {
        return id;
    }
    
    /**
     * 表示名を取得
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 現在値を取得
     */
    public float getCurrentValue(Player player, DataType dataType) {
        if (valueGetter != null) {
            return valueGetter.apply(player, dataType);
        }
        return 0.0f;
    }
    
    /**
     * 最大値を取得
     */
    public float getMaxValue(Player player, DataType dataType) {
        if (maxValueGetter != null) {
            return maxValueGetter.apply(player, dataType);
        }
        return 0.0f;
    }
    
    /**
     * 色を取得
     */
    public int getColor() {
        return color;
    }
    
    /**
     * 値を表示するかどうか
     */
    public boolean shouldShowValue() {
        return showValue;
    }
    
    /**
     * このリソースが有効かどうかを判定
     * 最大値 > 0 で有効とする（現在値は0でも可）
     */
    public boolean isValid(Player player, DataType dataType) {
        float max = getMaxValue(player, dataType);
        return max > 0;
    }
    
    /**
     * このリソースが利用可能かどうかを判定（最大値のみチェック）
     */
    public boolean isAvailable(Player player, DataType dataType) {
        return getMaxValue(player, dataType) > 0;
    }
    
    @Override
    public String toString() {
        return "ResourceCandidate{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
