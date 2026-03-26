package com.example.exile_overlay.client.render.entity;

import com.example.exile_overlay.mixin.AccessorRenderType;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import static com.mojang.blaze3d.vertex.DefaultVertexFormat.*;

public final class HealthBarRenderType extends RenderStateShard {

    public static final ResourceLocation HEALTH_BAR_TEXTURE = ResourceLocation.tryParse("exile_overlay:textures/shaders_workaround.png");
    public static final RenderType BAR_TYPE = createBarType();

    private HealthBarRenderType(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    private static RenderType createBarType() {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
            .setShaderState(POSITION_COLOR_TEX_LIGHTMAP_SHADER)
            .setTextureState(new TextureStateShard(HEALTH_BAR_TEXTURE, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setLightmapState(LIGHTMAP)
            .createCompositeState(false);

        return AccessorRenderType.exileOverlay$create(
            "exile_overlay_health_bar",
            POSITION_COLOR_TEX_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            256,
            false,
            true,
            state
        );
    }
}