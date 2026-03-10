package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent dropping favorite items with Q key outside of inventory GUI.
 * This applies only to the currently selected hotbar slot.
 * 
 * CLIENT-ONLY mixin.
 */
@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public abstract class PlayerDropItemMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/PlayerDropItemMixin");

    /**
     * Intercept item drop to prevent dropping favorite items.
     * This is called when player presses Q key to drop item.
     * 
     * @param dropAll true if Ctrl+Q (drop entire stack), false if Q (drop one)
     */
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onDrop(boolean dropAll, CallbackInfoReturnable<ItemEntity> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return; // Skip on server side
        }

        // Get currently selected hotbar slot (0-8)
        LocalPlayer self = (LocalPlayer) (Object) this;
        int selectedSlot = self.getInventory().selected;

        // Check if this slot is a favorite
        if (manager.isFavorite(selectedSlot)) {
            // Cancel the drop operation
            cir.setReturnValue(null);
            LOGGER.debug("Blocked drop of favorite item in slot {}", selectedSlot);
        }
    }
}
