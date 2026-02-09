package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.render.HotbarRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class ExileOverlayFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // シェーダーの登録
        ExileOverlayFabricShaders.register();

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            var mc = net.minecraft.client.Minecraft.getInstance();
            HotbarRenderer.render(graphics, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        });
    }
}
