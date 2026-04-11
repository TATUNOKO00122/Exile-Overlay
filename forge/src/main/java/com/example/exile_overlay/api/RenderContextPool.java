package com.example.exile_overlay.api;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * RenderContextのオブジェクトプール
 * 
 * 【設計思想】
 * - 毎フレームのRenderContext生成を回避
 * - CAS操作によるスレッド安全な獲得・解放
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
    private final AtomicIntegerArray inUse = new AtomicIntegerArray(POOL_SIZE);
    private final AtomicInteger rotateIndex = new AtomicInteger(0);
    
    private RenderContextPool() {
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
     * - CAS操作でアトミックに獲得
     * - プール枯渇時は新規作成（フォールバック）
     */
    public PooledRenderContext acquire(Minecraft minecraft, Player player,
                                       int screenWidth, int screenHeight,
                                       float partialTick, long gameTick,
                                       String elementId) {
        int rawIdx = rotateIndex.getAndIncrement();
        int startIdx = Math.abs(rawIdx % POOL_SIZE);
        
        for (int i = 0; i < POOL_SIZE; i++) {
            int idx = (startIdx + i) % POOL_SIZE;
            
            if (inUse.compareAndSet(idx, 0, 1)) {
                PooledRenderContext ctx = pool[idx];
                ctx.update(minecraft, player, screenWidth, screenHeight,
                           partialTick, gameTick, elementId);
                ctx.setPoolIndex(idx);
                return ctx;
            }
        }
        
        PooledRenderContext fallback = new PooledRenderContext();
        fallback.update(minecraft, player, screenWidth, screenHeight,
                       partialTick, gameTick, elementId);
        fallback.setPoolIndex(-1);
        return fallback;
    }
    
    /**
     * RenderContextをプールに戻す
     */
    public void release(PooledRenderContext ctx) {
        int idx = ctx.getPoolIndex();
        
        if (idx >= 0 && idx < POOL_SIZE) {
            inUse.set(idx, 0);
            ctx.reset();
        }
    }
    
    /**
     * プールの使用状況を取得（デバッグ用）
     */
    public PoolStats getStats() {
        int used = 0;
        for (int i = 0; i < POOL_SIZE; i++) {
            if (inUse.get(i) != 0) used++;
        }
        return new PoolStats(POOL_SIZE, used, rotateIndex.get());
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
