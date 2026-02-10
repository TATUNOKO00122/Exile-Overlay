package com.example.exile_overlay.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * RenderContextのオブジェクトプール
 * 
 * 【設計思想】
 * - 毎フレームのRenderContext生成を回避
 * - ラウンドロビン方式によるスレッド安全な再利用
 * - シングルトンパターンで一元管理
 * 
 * 【使用方法】
 * RenderContext ctx = RenderContextPool.acquire(mc, width, height, ...);
 * try {
 *     pipeline.render(graphics, ctx);
 * } finally {
 *     RenderContextPool.release(ctx);
 * }
 */
public class RenderContextPool {
    
    private static final int POOL_SIZE = 4;
    private static final RenderContextPool INSTANCE = new RenderContextPool();
    
    private final PooledRenderContext[] pool = new PooledRenderContext[POOL_SIZE];
    private final boolean[] inUse = new boolean[POOL_SIZE];
    private int rotateIndex = 0;
    
    private RenderContextPool() {
        // プールを事前に初期化
        for (int i = 0; i < POOL_SIZE; i++) {
            pool[i] = new PooledRenderContext();
        }
    }
    
    public static RenderContextPool getInstance() {
        return INSTANCE;
    }
    
    /**
     * プールからRenderContextを取得
     * 
     * 【スレッド安全性】
     * - シングルスレッド（レンダースレッド）のみで呼び出し
     * - ラウンドロビンで順番に割り当て
     */
    public PooledRenderContext acquire(Minecraft minecraft, Player player,
                                       int screenWidth, int screenHeight,
                                       float partialTick, long gameTick,
                                       String elementId) {
        int idx = getNextIndex();
        PooledRenderContext ctx = pool[idx];
        
        // 使用中チェック（万が一の重複使用防止）
        if (inUse[idx]) {
            // 使用中の場合は新規作成（フォールバック）
            ctx = new PooledRenderContext();
            idx = -1; // プール外を示す
        }
        
        if (idx >= 0) {
            inUse[idx] = true;
        }
        
        ctx.update(minecraft, player, screenWidth, screenHeight,
                   partialTick, gameTick, elementId);
        
        return ctx;
    }
    
    /**
     * RenderContextをプールに戻す
     */
    public void release(PooledRenderContext ctx) {
        // プール内のオブジェクトかチェック
        for (int i = 0; i < POOL_SIZE; i++) {
            if (pool[i] == ctx) {
                inUse[i] = false;
                ctx.reset();
                return;
            }
        }
        
        // プール外のオブジェクトはGCに任せる
    }
    
    /**
     * 次のインデックスを取得（ラウンドロビン）
     */
    private int getNextIndex() {
        return (rotateIndex++) % POOL_SIZE;
    }
    
    /**
     * プールの使用状況を取得（デバッグ用）
     */
    public PoolStats getStats() {
        int used = 0;
        for (boolean used1 : inUse) {
            if (used1) used++;
        }
        return new PoolStats(POOL_SIZE, used, rotateIndex);
    }
    
    /**
     * プール統計情報
     */
    public record PoolStats(int totalSize, int inUse, long totalAcquisitions) {
        @Override
        public String toString() {
            return String.format("PoolStats{total=%d, inUse=%d, acquisitions=%d}",
                totalSize, inUse, totalAcquisitions);
        }
    }
}
