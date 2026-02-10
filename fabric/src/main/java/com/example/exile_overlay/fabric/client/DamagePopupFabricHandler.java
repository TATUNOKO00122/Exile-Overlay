package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.damage.ThreadSafeDamageRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/**
 * Phase 2: スレッド安全性を確保したDamageRendererを使用
 */
public class DamagePopupFabricHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ThreadSafeDamageRenderer.getInstance().onClientTick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            ThreadSafeDamageRenderer.getInstance().onRenderWorld(context.matrixStack());
        });
    }
}
