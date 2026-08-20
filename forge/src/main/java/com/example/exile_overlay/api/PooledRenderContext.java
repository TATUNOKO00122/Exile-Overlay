package com.example.exile_overlay.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * プール利用向けのRenderContext実装。
 * 毎フレームのオブジェクト生成を回避するためミュータブル設計。レンダースレッド専用。
 */
public class PooledRenderContext extends RenderContext {

    private int poolIndex = -1;

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

    /**
     * プールに戻す前にクリーンアップ
     */
    public void reset() {
        this.minecraft = null;
        this.player = null;
        this.elementId = "unknown";
        this.poolIndex = -1;
    }

    /**
     * プール内のインデックスを取得
     */
    public int getPoolIndex() {
        return poolIndex;
    }

    /**
     * プール内のインデックスを設定
     */
    void setPoolIndex(int index) {
        this.poolIndex = index;
    }
}
