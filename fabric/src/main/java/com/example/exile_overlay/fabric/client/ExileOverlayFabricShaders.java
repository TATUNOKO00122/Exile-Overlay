package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * Fabricにおけるカスタムシェーダー登録クラス
 */
public class ExileOverlayFabricShaders {

    public static void register() {
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            try {
                context.register(new ResourceLocation("exile_overlay", "orb_liquid"),
                        DefaultVertexFormat.POSITION_TEX_COLOR,
                        shader -> OrbShaderRenderer.setOrbLiquidShader((ShaderInstance) shader));
            } catch (IOException e) {
                throw new RuntimeException("Failed to register exile_overlay shaders", e);
            }
        });
    }
}
