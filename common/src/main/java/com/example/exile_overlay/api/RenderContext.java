package com.example.exile_overlay.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * レンダリングコンテキスト
 * 
 * 【スレッド安全性】
 * - このクラスはイミュータブル（変更不可）
 * - レンダースレッド間で安全に共有可能
 */
public class RenderContext {

    protected Minecraft minecraft;
    protected Player player;
    protected int screenWidth;
    protected int screenHeight;
    protected float partialTick;
    protected long gameTick;
    protected String elementId;

    protected RenderContext() {
        // サブクラス用デフォルトコンストラクタ
        this.elementId = "unknown";
    }

    protected RenderContext(Builder builder) {
        this.minecraft = builder.minecraft;
        this.player = builder.player;
        this.screenWidth = builder.screenWidth;
        this.screenHeight = builder.screenHeight;
        this.partialTick = builder.partialTick;
        this.gameTick = builder.gameTick;
        this.elementId = builder.elementId;
    }
    
    public Minecraft getMinecraft() {
        return minecraft;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public int getScreenWidth() {
        return screenWidth;
    }
    
    public int getScreenHeight() {
        return screenHeight;
    }
    
    public float getPartialTick() {
        return partialTick;
    }
    
    public long getGameTick() {
        return gameTick;
    }
    
    public String getElementId() {
        return elementId;
    }
    
    public boolean isPlayerValid() {
        return player != null;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Minecraft minecraft;
        private Player player;
        private int screenWidth;
        private int screenHeight;
        private float partialTick;
        private long gameTick;
        private String elementId = "unknown";
        
        public Builder minecraft(Minecraft mc) {
            this.minecraft = mc;
            return this;
        }
        
        public Builder player(Player player) {
            this.player = player;
            return this;
        }
        
        public Builder screenSize(int width, int height) {
            this.screenWidth = width;
            this.screenHeight = height;
            return this;
        }
        
        public Builder partialTick(float tick) {
            this.partialTick = tick;
            return this;
        }
        
        public Builder gameTick(long tick) {
            this.gameTick = tick;
            return this;
        }
        
        public Builder elementId(String id) {
            this.elementId = id;
            return this;
        }
        
        public RenderContext build() {
            if (minecraft == null) {
                throw new IllegalStateException("Minecraft instance is required");
            }
            return new RenderContext(this);
        }
    }
}
