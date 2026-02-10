package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.render.HudRenderManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

/**
 * Fabricクライアント初期化クラス（パイプライン版）
 *
 * 【改善点】
 * - HudRenderManagerによる一元管理
 * - パイプラインアーキテクチャによる柔軟な描画順序制御
 * - IRenderCommandによるモジュール化
 */
public class ExileOverlayFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // シェーダーの登録
        ExileOverlayFabricShaders.register();

        // HUDレンダリングマネージャーの初期化
        HudRenderManager.getInstance().initialize();

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

        // ダメージポップアップの登録
        DamagePopupFabricHandler.register();
    }
}
