package com.example.exile_overlay.client.event;

import com.example.exile_overlay.ExileOverlayMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.example.exile_overlay.client.config.position.HudPositionManager;

/**
 * Forge版バフ表示ハンドラ
 * RenderGuiEventを使用してバフオーバーレイを描画
 */
@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT)
public class BuffOverlayForgeHandler {

    /**
     * バニラのポーション効果オーバーレイをキャンセル
     */
    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        // POTION_ICONSオーバーレイ（バニラの効果表示）をキャンセル
        // ただし、Exile Overlayのバフ表示が両方とも非表示の場合はバニラの効果表示を維持する
        if (event.getOverlay() == VanillaGuiOverlay.POTION_ICONS.type()) {
            HudPositionManager pm = HudPositionManager.getInstance();
            boolean buffVisible = pm.getPosition("buff_overlay").isVisible();
            boolean skillBuffVisible = pm.getPosition("skill_buff_overlay").isVisible();
            if (buffVisible || skillBuffVisible) {
                event.setCanceled(true);
            }
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
