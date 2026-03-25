package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.render.entity.EntityHealthBarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private EntityRenderDispatcher entityRenderDispatcher;

    @Inject(
        method = "renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V",
        at = @At("TAIL")
    )
    private void exileOverlay$renderHealthBar(Entity entity, double camX, double camY, double camZ,
            float partialTick, PoseStack poseStack, MultiBufferSource buffers, CallbackInfo ci) {
        try {
            double d0 = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double d1 = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double d2 = Mth.lerp(partialTick, entity.zOld, entity.getZ());

            EntityHealthBarRenderer.hookRender(
                entity,
                poseStack,
                buffers,
                entityRenderDispatcher.camera,
                entityRenderDispatcher.getRenderer(entity),
                partialTick,
                d0 - camX,
                d1 - camY,
                d2 - camZ
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("exile_overlay/LevelRendererMixin")
                .error("Failed to render health bar", e);
        }
    }
}