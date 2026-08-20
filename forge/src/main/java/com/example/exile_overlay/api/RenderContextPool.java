package com.example.exile_overlay.api;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * RenderContextのオブジェクトプール。
 * レンダースレッドは単一スレッドなので同期不要。
 */
public class RenderContextPool {

    private static final RenderContextPool INSTANCE = new RenderContextPool();

    private final PooledRenderContext[] pool = new PooledRenderContext[2];
    private int nextIndex = 0;

    private RenderContextPool() {
        for (int i = 0; i < pool.length; i++) {
            pool[i] = new PooledRenderContext();
        }
    }

    public static RenderContextPool getInstance() {
        return INSTANCE;
    }

    /**
     * プールからRenderContextを取得
     * レンダースレッドからのみ呼び出される前提
     */
    public PooledRenderContext acquire(Minecraft minecraft, Player player,
                                       int screenWidth, int screenHeight,
                                       float partialTick, long gameTick,
                                       String elementId) {
        PooledRenderContext ctx = pool[nextIndex % pool.length];
        nextIndex++;
        ctx.update(minecraft, player, screenWidth, screenHeight,
                   partialTick, gameTick, elementId);
        ctx.setPoolIndex(0);
        return ctx;
    }

    /**
     * RenderContextをプールに戻す
     */
    public void release(PooledRenderContext ctx) {
        ctx.reset();
    }
}
