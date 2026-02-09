package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.render.HotbarRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExileOverlayGui {
    private static final Logger LOGGER = LogManager.getLogger();

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        String overlayId = event.getOverlay().id().toString();

        // バニラのHP、経験値、ホットバー、食べ物、鎧、酸素計をキャンセル
        if (event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
                event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type() ||
                event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            LOGGER.debug("[ExileOverlay] Canceling vanilla overlay: {}", overlayId);
            event.setCanceled(true);
            return;
        }

        // Mine and Slash のオーバーレイをキャンセル
        if (overlayId.startsWith("mine_and_slash:")) {
            LOGGER.debug("[ExileOverlay] Canceling Mine and Slash overlay: {}", overlayId);
            event.setCanceled(true);
            return;
        }

        // ホットバーのタイミングでカスタムHUDを描画
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            LOGGER.debug("[ExileOverlay] Rendering custom HUD at hotbar overlay");
            HotbarRenderer.render(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight());
            event.setCanceled(true);
        }
    }
}
