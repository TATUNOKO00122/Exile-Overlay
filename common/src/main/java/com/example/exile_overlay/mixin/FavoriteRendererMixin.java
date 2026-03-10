package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to render favorite stars on inventory screens.
 * Uses the FavoriteItemRenderer to draw stars on favorite slots.
 * 
 * CLIENT-ONLY mixin.
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class FavoriteRendererMixin {

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    /**
     * Inject after the container screen renders to draw favorite stars on top.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void exileOverlay$onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        FavoriteItemRenderer.renderFavorites(graphics, self, this.leftPos, this.topPos);
    }
}
