package com.example.exile_overlay.client.render.orb;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * オーブの設定を保持するクラス
 * 
 * 【スロットベース設計】
 * このクラスはHUD上の物理的なスロットに表示されるオーブの設定を管理します。
 * 
 * 【オーバーレイ機能】
 * overlayFor: このオーブが「どのオーブの上に」重なるかを指定
 * overlayProvider: オーバーレイとして表示するデータプロバイダー
 * 
 * 使用例:
 * - ORB_1 (メインオーブ): overlayFor=null, overlayProvider=null
 * - ORB_1_OVERLAY (オーバーレイ): overlayFor="orb_1", overlayProvider=データプロバイダー
 */
public class OrbConfig {
    
    private final String id;
    private final int x;
    private final int y;
    private final int size;
    private final int color;
    private final int overlayColor;
    private final boolean showReflection;
    private final OrbDataProvider dataProvider;
    private final Predicate<Player> visibilityPredicate;
    
    // オーバーレイ関連
    private final String overlayFor;              // 重なる先のオーブID
    private final OrbDataProvider overlayProvider; // オーバーレイのデータプロバイダー
    
    private OrbConfig(Builder builder) {
        this.id = builder.id;
        this.x = builder.x;
        this.y = builder.y;
        this.size = builder.size;
        this.color = builder.color;
        this.overlayColor = builder.overlayColor;
        this.showReflection = builder.showReflection;
        this.dataProvider = builder.dataProvider;
        this.visibilityPredicate = builder.visibilityPredicate;
        this.overlayFor = builder.overlayFor;
        this.overlayProvider = builder.overlayProvider;
    }
    
    public String getId() {
        return id;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getSize() {
        return size;
    }
    
    public int getColor() {
        return color;
    }
    
    public int getOverlayColor() {
        return overlayColor;
    }
    
    public boolean shouldShowReflection() {
        return showReflection;
    }
    
    public OrbDataProvider getDataProvider() {
        return dataProvider;
    }
    
    public boolean isVisible(Player player) {
        return visibilityPredicate.test(player);
    }
    
    /**
     * このオーブがオーバーレイかどうか
     * @return 他のオーブの上に重なる場合はtrue
     */
    public boolean isOverlay() {
        return overlayFor != null;
    }
    
    /**
     * 重なる先のオーブIDを取得
     * @return 重なる先のオーブID、オーバーレイでない場合はnull
     */
    public String getOverlayFor() {
        return overlayFor;
    }
    
    /**
     * オーバーレイのデータプロバイダーを取得
     * @return オーバーレイのデータプロバイダー、設定されていない場合はnull
     */
    public OrbDataProvider getOverlayProvider() {
        return overlayProvider;
    }
    
    /**
     * このオーブにオーバーレイが定義されているか
     * @return overlayColorが設定されている場合はtrue
     */
    public boolean hasOverlayColor() {
        return overlayColor != 0x00FFFFFF;
    }
    
    /**
     * 円の中心X座標を取得
     */
    public int getCenterX() {
        return x - (size / 2);
    }
    
    /**
     * 円の中心Y座標を取得
     */
    public int getCenterY() {
        return y - (size / 2);
    }
    
    public static Builder builder(String id) {
        return new Builder(id);
    }
    
    public static class Builder {
        private final String id;
        private int x;
        private int y;
        private int size = 85;
        private int color = 0xFFFFFFFF;
        private int overlayColor = 0x00FFFFFF;
        private boolean showReflection = true;
        private OrbDataProvider dataProvider;
        private Predicate<Player> visibilityPredicate = p -> true;
        
        // オーバーレイ関連
        private String overlayFor = null;
        private OrbDataProvider overlayProvider = null;
        
        private Builder(String id) {
            this.id = id;
        }
        
        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }
        
        public Builder size(int size) {
            this.size = size;
            return this;
        }
        
        public Builder color(int color) {
            this.color = color;
            return this;
        }
        
        public Builder overlayColor(int overlayColor) {
            this.overlayColor = overlayColor;
            return this;
        }
        
        public Builder showReflection(boolean show) {
            this.showReflection = show;
            return this;
        }
        
        public Builder dataProvider(OrbDataProvider provider) {
            this.dataProvider = provider;
            return this;
        }
        
        public Builder visibleWhen(Predicate<Player> predicate) {
            this.visibilityPredicate = predicate;
            return this;
        }
        
        /**
         * このオーブがオーバーレイとして動作するよう設定
         * 
         * @param targetOrbId 重なる先のオーブID
         * @param overlayProvider オーバーレイのデータプロバイダー
         */
        public Builder asOverlayFor(String targetOrbId, OrbDataProvider overlayProvider) {
            this.overlayFor = targetOrbId;
            this.overlayProvider = overlayProvider;
            return this;
        }
        
        /**
         * このオーブにオーバーレイを定義（ORB_1のようなメインオーブ用）
         * 
         * @param overlayColor オーバーレイの色
         */
        public Builder withOverlay(int overlayColor) {
            this.overlayColor = overlayColor;
            return this;
        }
        
        public OrbConfig build() {
            if (dataProvider == null) {
                throw new IllegalStateException("DataProvider must be set for orb: " + id);
            }
            return new OrbConfig(this);
        }
    }
}
