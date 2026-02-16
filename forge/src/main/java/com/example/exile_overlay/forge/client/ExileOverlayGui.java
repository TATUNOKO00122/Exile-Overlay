package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.render.HudRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ForgeクライアントGUIクラス（パイプライン版）
 *
 * 【改善点】
 * - HudRenderManagerによる一元管理
 * - パイプラインアーキテクチャによる柔軟な描画順序制御
 * - IRenderCommandによるモジュール化
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ExileOverlayGui {
    private static final Logger LOGGER = LogManager.getLogger();

    // HUDレンダリングマネージャー初期化済みフラグ
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        String overlayId = event.getOverlay().id().toString();

        // 初回初期化
        if (!initialized) {
            HudRenderManager.getInstance().initialize();
            initialized = true;
            LOGGER.info("HudRenderManager initialized");
        }

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
        // Mod IDは "mine_and_slash" だが、内部では "mmorpg" も使用される
        if (overlayId.startsWith("mine_and_slash:") || overlayId.contains("mmorpg")) {
            LOGGER.debug("[ExileOverlay] Canceling Mine and Slash overlay: {}", overlayId);
            event.setCanceled(true);
            return;
        }

        // ホットバーのタイミングでカスタムHUDを描画
        // ポーズメニューなどが開いている場合は描画しない
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            LOGGER.debug("[ExileOverlay] Rendering custom HUD at hotbar overlay");

            Minecraft mc = Minecraft.getInstance();

            // ポーズメニュー/インベントリなどが開いている場合は描画しない
            // ただし、HUD設定画面の場合はプレビューのために描画を許可する
            if (mc.screen != null
                    && !(mc.screen instanceof com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen)) {
                event.setCanceled(true);
                return;
            }

            // パイプライン経由でレンダリング
            HudRenderManager.getInstance().render(
                    event.getGuiGraphics(),
                    event.getWindow().getGuiScaledWidth(),
                    event.getWindow().getGuiScaledHeight());

            event.setCanceled(true);
        }
    }
}
