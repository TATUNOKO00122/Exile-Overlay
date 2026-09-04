package com.example.exile_overlay.client.event;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.ModMenuApi;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.client.render.DayCounterRenderer;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.ailment.ClientAilmentTracker;
// import com.example.exile_overlay.client.render.kill.KillCountManager;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.example.exile_overlay.client.render.orb.OrbSmoothedValue;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;
import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.dmgtracker.network.TrackerActionC2S;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ExileOverlayForgeClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExileOverlayForgeClient.class);
    private static final EquipmentDisplayConfig EQUIP_CONFIG = EquipmentDisplayConfig.getInstance();
    private static KeyMapping hudConfigKey;
    private static KeyMapping toggleOverlayKey;
    private static KeyMapping resetTrackerKey;
    public static KeyMapping toggleItemLockKey;

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation("exile_overlay", "orb_fill"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    shader -> OrbShaderRenderer.setOrbFillShader(shader));
        } catch (IOException e) {
            LOGGER.error("Failed to register orb shader, falling back to texture rendering", e);
            OrbShaderRenderer.setOrbFillShader(null);
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // HUDレンダリングマネージャーの初期化
            HudRenderManager.getInstance().initialize();

            // HUD位置設定マネージャーの初期化
            HudPositionManager.getInstance().initialize();

            // Forgeイベントバスにクライアントティックハンドラーとログアウトハンドラーを登録
            MinecraftForge.EVENT_BUS.addListener(ExileOverlayForgeClient::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(ExileOverlayForgeClient::onLoggingOut);

            // Mine and SlashのNeat HPバー設定を適用（設定に基づく）
            if (EQUIP_CONFIG.isDisableMnsHpBar()) {
                MethodHandlesUtil.setNeatHpBarEnabled(false);
            }

            TrackerConfig.getInstance().load();

            if (ModList.get().isLoaded("botania")) {
                HudRenderManager.getInstance().registerCommand(new com.example.exile_overlay.client.render.botania.BotaniaManaBarRenderer(), 50);
            }

            if (ModList.get().isLoaded("lightmanscurrency")) {
                HudRenderManager.getInstance().registerCommand(new com.example.exile_overlay.client.render.currency.LightmansCurrencyCoinRenderer(), 60);
            }

            LOGGER.info("ExileOverlayForgeClient initialized");
        });
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        hudConfigKey = new KeyMapping(
                "key.exile_overlay.hud_config",
                GLFW.GLFW_KEY_O,
                "category.exile_overlay.general");
        event.register(hudConfigKey);

        toggleOverlayKey = new KeyMapping(
                "key.exile_overlay.toggle_dmg_tracker",
                GLFW.GLFW_KEY_UNKNOWN,
                "category.exile_overlay.general");
        event.register(toggleOverlayKey);

        resetTrackerKey = new KeyMapping(
                "key.exile_overlay.reset_dmg_tracker",
                GLFW.GLFW_KEY_UNKNOWN,
                "category.exile_overlay.general");
        event.register(resetTrackerKey);

        toggleItemLockKey = new KeyMapping(
                "key.exile_overlay.toggle_item_lock",
                GLFW.GLFW_KEY_UNKNOWN,
                "category.exile_overlay.general");
        event.register(toggleItemLockKey);

        LOGGER.info("Registered key bindings for Forge");
    }

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "exile_overlay_hud",
                (gui, graphics, partialTick, screenWidth, screenHeight) -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.player == null) {
                        return;
                    }
                    if (mc.screen != null && !(mc.screen instanceof DraggableHudConfigScreen)
                            && !(mc.screen instanceof ConfigScreen)) {
                        return;
                    }
                    HudRenderManager.getInstance().render(graphics, screenWidth, screenHeight);
                });
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        com.example.exile_overlay.api.UnifiedCache.getInstance().clearAll();
        com.example.exile_overlay.client.render.minion.MercenaryClientCache.clear();
        OrbSmoothedValue.resetAll();
        TrackerSyncS2C.ClientTrackerData.resetServerPresence();
        // KillCountManager.getInstance().reset();
        DayCounterRenderer.reset();
        LOGGER.info("Reset cache and session data on logging out");
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                OrbSmoothedValue.resetAll();
                TrackerSyncS2C.ClientTrackerData.resetServerPresence();
                return;
            }
            if (hudConfigKey != null && hudConfigKey.consumeClick()) {
                LOGGER.info("HUD config key pressed on Forge, opening config screen");
                ModMenuApi.openConfigScreen();
            }
            if (mc.player.tickCount % 40 == 0) {
                ClientAilmentTracker.getInstance().cleanup();
            }
            if (TrackerSyncS2C.ClientTrackerData.serverHasMod()) {
                while (toggleOverlayKey != null && toggleOverlayKey.consumeClick()) {
                    TrackerConfig.toggleOverlay();
                }
                while (resetTrackerKey != null && resetTrackerKey.consumeClick()) {
                    NetworkHandler.CHANNEL.sendToServer(new TrackerActionC2S(TrackerActionC2S.ACTION_RESET));
                }
            }
        }
    }

}
