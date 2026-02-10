package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.damage.DamageRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class DamagePopupFabricHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DamageRenderer.getInstance().onClientTick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            DamageRenderer.getInstance().onRenderWorld(context.matrixStack());
        });
    }
}
