package com.example.exile_overlay.forge.client;

import com.example.ExampleMod;
import com.example.exile_overlay.client.render.HotbarRenderer;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExileOverlayForgeClient {

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation("exile_overlay", "orb_liquid"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    shader -> OrbShaderRenderer.setOrbLiquidShader(shader));
        } catch (IOException e) {
            throw new RuntimeException("Failed to register exile_overlay shaders", e);
        }
    }
}

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
class ExileOverlayForgeGui {

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        // バニラのHP、経験値、ホットバー、食べ物、鎧、酸素計をキャンセル
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
                event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() ||
                event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            event.setCanceled(true);
        }

        // ホットバーのタイミングでカスタムHUDを描画
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            HotbarRenderer.render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight());
            event.setCanceled(true);
        }
    }
}
