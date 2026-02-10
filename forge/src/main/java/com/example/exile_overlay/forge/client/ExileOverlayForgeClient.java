package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.config.ModMenuApi;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExileOverlayForgeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExileOverlayForgeClient.class);
    private static KeyMapping hudConfigKey;

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

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // HUDレンダリングマネージャーの初期化
            HudRenderManager.getInstance().initialize();

            // HUD位置設定マネージャーの初期化
            HudPositionManager.getInstance().initialize();

            // Forgeイベントバスにクライアントティックハンドラーを登録
            MinecraftForge.EVENT_BUS.addListener(ExileOverlayForgeClient::onClientTick);

            LOGGER.info("ExileOverlayForgeClient initialized");
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        hudConfigKey = new KeyMapping(
                "key.exile_overlay.hud_config",
                GLFW.GLFW_KEY_O,
                "category.exile_overlay.general"
        );
        event.register(hudConfigKey);
        LOGGER.info("Registered HUD config key binding for Forge");
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // HUDオーバーレイを登録
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "exile_overlay_hud", (gui, graphics, partialTick, screenWidth, screenHeight) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                HudRenderManager.getInstance().render(graphics, screenWidth, screenHeight);
            }
        });
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getInstance().player != null) {
            if (hudConfigKey != null && hudConfigKey.consumeClick()) {
                LOGGER.info("HUD config key pressed on Forge, opening config screen");
                ModMenuApi.openConfigScreen();
            }
        }
    }
}
