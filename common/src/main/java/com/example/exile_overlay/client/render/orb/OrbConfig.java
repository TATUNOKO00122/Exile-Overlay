package com.example.exile_overlay.client.render.orb;

import java.util.function.Predicate;
import net.minecraft.world.entity.player.Player;

/**
 * オーブの設定を保持するクラス
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
        
        public OrbConfig build() {
            if (dataProvider == null) {
                throw new IllegalStateException("DataProvider must be set for orb: " + id);
            }
            return new OrbConfig(this);
        }
    }
}
