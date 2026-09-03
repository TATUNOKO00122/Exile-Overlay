package com.example.exile_overlay.mixin.compat.lootjournal;

import com.example.exile_overlay.client.compat.lootjournal.LootJournalScaleHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

@Pseudo
@Mixin(targets = "dev.obscuria.lootjournal.client.renderer.PickupRenderUtils", remap = false)
public abstract class LootJournalRendererMixin {

    @Unique
    private static final ThreadLocal<Float> exileOverlay$currentScale = new ThreadLocal<>();

    @Unique
    private static MethodHandle exileOverlay$eventHandle;
    @Unique
    private static MethodHandle exileOverlay$stackHandle;
    @Unique
    private static boolean exileOverlay$handlesInitialized = false;

    @Unique
    private static void exileOverlay$initHandles(Object renderer) {
        if (exileOverlay$handlesInitialized) return;
        exileOverlay$handlesInitialized = true;
        try {
            Method eventMethod = renderer.getClass().getMethod("event");
            eventMethod.setAccessible(true);
            exileOverlay$eventHandle = MethodHandles.lookup().unreflect(eventMethod);
        } catch (Throwable ignored) {
        }
    }

    @Unique
    private static ItemStack exileOverlay$extractItemStack(Object renderer) {
        if (renderer == null) return ItemStack.EMPTY;
        try {
            exileOverlay$initHandles(renderer);
            if (exileOverlay$eventHandle != null) {
                Object event = exileOverlay$eventHandle.invoke(renderer);
                if (event != null) {
                    if (exileOverlay$stackHandle == null) {
                        Method stackMethod = event.getClass().getMethod("stack");
                        stackMethod.setAccessible(true);
                        exileOverlay$stackHandle = MethodHandles.lookup().unreflect(stackMethod);
                    }
                    if (exileOverlay$stackHandle != null) {
                        return (ItemStack) exileOverlay$stackHandle.invoke(event);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Ldev/obscuria/lootjournal/client/renderer/PickupRenderer;)V", at = @At("HEAD"), remap = false)
    private static void exileOverlay$preRender(GuiGraphics graphics, Object renderer, CallbackInfo ci) {
        try {
            ItemStack stack = exileOverlay$extractItemStack(renderer);
            float scale = LootJournalScaleHelper.calculateScale(stack);
            if (scale > 1.001f) {
                exileOverlay$currentScale.set(scale);
                graphics.pose().pushPose();

                // 行の中心(Y=7)を基準に拡大して軸ズレを防止
                float centerY = 7.0f;
                graphics.pose().translate(0, centerY, 0);
                graphics.pose().scale(scale, scale, 1.0f);
                graphics.pose().translate(0, -centerY, 0);
            } else {
                exileOverlay$currentScale.remove();
            }
        } catch (Throwable ignored) {
            exileOverlay$currentScale.remove();
        }
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Ldev/obscuria/lootjournal/client/renderer/PickupRenderer;)V", at = @At("RETURN"), remap = false)
    private static void exileOverlay$postRender(GuiGraphics graphics, Object renderer, CallbackInfo ci) {
        try {
            Float scale = exileOverlay$currentScale.get();
            if (scale != null && scale > 1.001f) {
                graphics.pose().popPose();
            }
        } finally {
            exileOverlay$currentScale.remove();
        }
    }
}
