package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import com.example.exile_overlay.client.favorite.FavoriteItemRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class FavoriteRendererMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/FavoriteRendererMixin");

    @Inject(method = "render", at = @At("TAIL"))
    private void exileOverlay$onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            Screen self = (Screen) (Object) this;
            if (!(self instanceof AbstractContainerScreen<?> containerScreen)) {
                return;
            }

            FavoriteItemManager manager = FavoriteItemManager.getInstance();
            if (manager == null || !manager.isEnabled()) {
                return;
            }

            RenderSystem.disableDepthTest();
            FavoriteItemRenderer.renderFavorites(graphics, containerScreen, containerScreen.getGuiLeft(), containerScreen.getGuiTop());
            RenderSystem.enableDepthTest();
        } catch (Exception e) {
            LOGGER.error("Failed to render favorites", e);
        }
    }
}
