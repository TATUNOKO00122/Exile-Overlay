package com.example.exile_overlay.client.render.entity;

import com.example.exile_overlay.mixin.AccessorRenderType;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

public final class HealthBarRenderType extends RenderStateShard {

    public static final RenderType BAR_TYPE = createBarType();

    private HealthBarRenderType(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    private static RenderType createBarType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(POSITION_COLOR_LIGHTMAP_SHADER)
            .setTextureState(NO_TEXTURE)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setDepthTestState(NO_DEPTH_TEST)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false);

        return AccessorRenderType.exileOverlay$create(
            "exile_overlay_health_bar",
            POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state
        );
    }
}