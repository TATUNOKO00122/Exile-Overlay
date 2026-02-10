package com.example.exile_overlay.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * プール可能なRenderContext実装
 * 
 * 【設計思想】
 * - オブジェクトプール用にミュータブル設計
 * - 毎フレームの新規オブジェクト生成を回避
 * - スレッド制約: レンダースレッドのみで使用
 */
public class PooledRenderContext extends RenderContext {
    
    private Minecraft minecraft;
    private Player player;
    private int screenWidth;
    private int screenHeight;
    private float partialTick;
    private long gameTick;
    private String elementId;
    
    /**
     * デフォルトコンストラクタ（プール用）
     */
    public PooledRenderContext() {
        super();
    }
    
    /**
     * コンテキストを更新（プール再利用時に呼び出し）
     */
    public void update(Minecraft minecraft, Player player, 
                      int screenWidth, int screenHeight,
                      float partialTick, long gameTick, String elementId) {
        this.minecraft = minecraft;
        this.player = player;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.partialTick = partialTick;
        this.gameTick = gameTick;
        this.elementId = elementId != null ? elementId : "unknown";
    }
    
    @Override
    public Minecraft getMinecraft() {
        return minecraft;
    }
    
    @Override
    public Player getPlayer() {
        return player;
    }
    
    @Override
    public int getScreenWidth() {
        return screenWidth;
    }
    
    @Override
    public int getScreenHeight() {
        return screenHeight;
    }
    
    @Override
    public float getPartialTick() {
        return partialTick;
    }
    
    @Override
    public long getGameTick() {
        return gameTick;
    }
    
    @Override
    public String getElementId() {
        return elementId;
    }
    
    @Override
    public boolean isPlayerValid() {
        return player != null;
    }
    
    /**
     * プールに戻す前にクリーンアップ
     */
    public void reset() {
        this.minecraft = null;
        this.player = null;
        this.elementId = "unknown";
    }
}
