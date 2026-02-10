package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.render.effect.BuffOverlayRenderer;
import net.minecraft.client.Minecraft;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return; // GUI画面が開いている場合は描画しない

        // バフ表示をレンダリング
        BuffOverlayRenderer.render(event.getGuiGraphics(), event.getPartialTick());
    }
}
