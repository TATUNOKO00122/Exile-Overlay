package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge版バフ表示ハンドラ
 * RenderGuiEventを使用してバフオーバーレイを描画
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class BuffOverlayForgeHandler {

    /**
     * バニラのポーション効果オーバーレイをキャンセル
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        // POTION_ICONSオーバーレイ（バニラの効果表示）をキャンセル
        if (event.getOverlay() == VanillaGuiOverlay.POTION_ICONS.type()) {
            event.setCanceled(true);
        }
    }

    /**
     * カスタムバフ表示をレンダリング
     */
    @SubscribeEvent
    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        // バフ表示は現在 HudRenderManager のパイプライン経由で描画されるため、ここでは何もしない
    }
}
