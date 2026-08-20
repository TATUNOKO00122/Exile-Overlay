package com.example.exile_overlay.client.config;

import com.example.exile_overlay.ExileOverlayMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge版GUIオーバーレイ設定
 * バニラの効果表示を無効化
 */
@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeGuiOverlayConfig {

    @SubscribeEvent
    public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        // バニラのポーション効果オーバーレイを無効化（最前面に設定して非表示に）
        // Note: Forgeではバニラオーバーレイを完全に削除するのは難しいため、
        // イベントハンドラでキャンセルする方式も併用します
    }
}
