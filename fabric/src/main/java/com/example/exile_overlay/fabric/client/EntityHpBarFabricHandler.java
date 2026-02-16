package com.example.exile_overlay.fabric.client;

import com.example.exile_overlay.client.render.EntityHpBarRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric版 3D HPバーイベントハンドラ
 * 
 * HJUD MODと同じ構造で実装
 */
public class EntityHpBarFabricHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHpBarFabricHandler.class);
    
    /**
     * イベントハンドラを登録
     */
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) {
                return;
            }
            
            try {
                // HJUDと同じ：カメラのYRotを取得
                float cameraYaw = context.camera().getYRot();
                
                // 共通レンダラーを呼び出し
                EntityHpBarRenderer.render(
                        context.matrixStack(),
                        mc.renderBuffers().bufferSource(),
                        context.camera().getPosition(),
                        cameraYaw,  // HJUDと同じ
                        context.tickDelta()
                );
                
            } catch (Exception e) {
                LOGGER.error("Failed to render entity HP bars", e);
            }
        });
        
        LOGGER.info("EntityHpBarFabricHandler registered");
    }
}
