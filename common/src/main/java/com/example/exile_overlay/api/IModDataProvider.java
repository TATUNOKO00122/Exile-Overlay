package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * MODデータプロバイダーの基底インターフェース
 * 各MODはこのインターフェースを実装してデータを提供する
 */
public interface IModDataProvider {
    
    /**
     * このプロバイダーが利用可能かどうか
     */
    boolean isAvailable();
    
    /**
     * プロバイダーの優先度（高いほど優先）
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * プロバイダーのID
     */
    String getId();
    
    // ========== 基本ステータス ==========
    
    /**
     * 現在のHP
     */
    float getCurrentHealth(Player player);
    
    /**
     * 最大HP
     */
    float getMaxHealth(Player player);
    
    /**
     * 現在のマナ
     */
    float getCurrentMana(Player player);
    
    /**
     * 最大マナ
     */
    float getMaxMana(Player player);
    
    // ========== 拡張ステータス ==========
    
    /**
     * 現在の魔法シールド
     */
    default float getCurrentMagicShield(Player player) {
        return 0;
    }
    
    /**
     * 最大魔法シールド
     */
    default float getMaxMagicShield(Player player) {
        return 0;
    }
    
    /**
     * 現在のエネルギー
     */
    default float getCurrentEnergy(Player player) {
        return 0;
    }
    
    /**
     * 最大エネルギー
     */
    default float getMaxEnergy(Player player) {
        return 0;
    }
    
    /**
     * 現在のブラッド（血魔法等）
     */
    default float getCurrentBlood(Player player) {
        return 0;
    }
    
    /**
     * 最大ブラッド
     */
    default float getMaxBlood(Player player) {
        return 0;
    }
    
    // ========== その他ステータス ==========
    
    /**
     * レベル
     */
    default int getLevel(Player player) {
        return 1;
    }
    
    /**
     * 経験値
     */
    default float getExp(Player player) {
        return 0;
    }
    
    /**
     * 次のレベルアップに必要な経験値
     */
    default float getExpRequiredForLevelUp(Player player) {
        return 1;
    }
    
    /**
     * 血魔法がアクティブか
     */
    default boolean isBloodMagicActive(Player player) {
        return false;
    }
}
