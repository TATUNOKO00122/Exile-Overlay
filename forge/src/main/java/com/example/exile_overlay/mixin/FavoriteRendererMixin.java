package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class FavoriteRendererMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/FavoriteRendererMixin");

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Inject(method = "render", at = @At("TAIL"))
    private void exileOverlay$onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        try {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            FavoriteItemRenderer.renderFavorites(graphics, self, this.leftPos, this.topPos);
        } catch (Exception e) {
            LOGGER.error("Failed to render favorites", e);
        }
    }
}
