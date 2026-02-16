package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.damage.DamagePopupManager;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class DamagePopupFabricHandler {

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            DamagePopupManager.getInstance().onClientTick();
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            DamagePopupManager.getInstance().onRenderWorld(context.matrixStack());
        });
    }
}
