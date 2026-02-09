package com.example.exile_overlay.client.render.orb;

import net.minecraft.world.entity.player.Player;

/**
 * オーブのデータを提供するインターフェース
 */
@FunctionalInterface
public interface OrbDataProvider {
    /**
     * プレイヤーから現在値を取得
     */
    float getCurrentValue(Player player);
    
    /**
     * プレイヤーから最大値を取得（デフォルトは1）
     */
    default float getMaxValue(Player player) {
        return 1.0f;
    }
    
    /**
     * 値を表示するかどうか
     */
    default boolean shouldShowValue() {
        return true;
    }
}
