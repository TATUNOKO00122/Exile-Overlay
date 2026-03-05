package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
// DISABLED: 3D HPBar
// import com.example.exile_overlay.client.render.EntityHpBarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge版 3D HPバーイベントハンドラ
 * 
 * HJUD MODと同じ構造で実装
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EntityHpBarForgeHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHpBarForgeHandler.class);
    
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // DISABLED: 3D HPBar
        // // エンティティ描画後のステージで実行（HJUDと同じ）
        // if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
        //     return;
        // }
        // 
        // Minecraft mc = Minecraft.getInstance();
        // if (mc.level == null || mc.player == null) {
        //     return;
        // }
        // 
        // try {
        //     PoseStack poseStack = event.getPoseStack();
        //     
        //     // HJUDと同じ：カメラのYRotを取得
        //     float cameraYaw = event.getCamera().getYRot();
        //     
        //     // 共通レンダラーを呼び出し
        //     EntityHpBarRenderer.render(
        //             poseStack,
        //             mc.renderBuffers().bufferSource(),
        //             event.getCamera().getPosition(),
        //             cameraYaw,  // HJUDと同じ
        //             event.getPartialTick()
        //     );
        //     
        // } catch (Exception e) {
        //     LOGGER.error("Failed to render entity HP bars", e);
        // }
    }
}
