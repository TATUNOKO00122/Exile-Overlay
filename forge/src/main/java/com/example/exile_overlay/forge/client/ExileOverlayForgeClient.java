package com.example.exile_overlay.forge.client;

import com.example.ExampleMod;
import com.example.exile_overlay.client.render.HotbarRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class ExileOverlayForgeClient {

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
