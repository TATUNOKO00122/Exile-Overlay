package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.config.ModMenuApi;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.client.render.effect.BuffOverlayRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabricクライアント初期化クラス（パイプライン版）
 *
 * 【改善点】
 * - HudRenderManagerによる一元管理
 * - パイプラインアーキテクチャによる柔軟な描画順序制御
 * - IRenderCommandによるモジュール化
 */
public class ExileOverlayFabricClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExileOverlayFabricClient.class);
    private static KeyMapping hudConfigKey;

    @Override
    public void onInitializeClient() {
        // シェーダーの登録
        ExileOverlayFabricShaders.register();

        // HUDレンダリングマネージャーの初期化
        HudRenderManager.getInstance().initialize();

        // HUD位置設定マネージャーの初期化
        HudPositionManager.getInstance().initialize();

        // HUDレンダリング
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();

            // パイプライン経由でレンダリング
            HudRenderManager.getInstance().render(
                graphics,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight()
            );
        });

        // バフオーバーレイの登録（設定画面を開いていても描画する）
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                BuffOverlayRenderer.render(graphics, tickDelta);
            }
        });

        // ダメージポップアップの登録
        DamagePopupFabricHandler.register();

        // キーバインディングの登録
        registerKeyBindings();
    }

    private void registerKeyBindings() {
        hudConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.exile_overlay.hud_config",
                GLFW.GLFW_KEY_O,
                "category.exile_overlay.general"
        ));

        LOGGER.info("Registered HUD config key binding: {}", hudConfigKey.getDefaultKey().getDisplayName().getString());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && hudConfigKey.consumeClick()) {
                LOGGER.info("HUD config key pressed, opening config screen");
                ModMenuApi.openConfigScreen();
            }
        });
    }
}
