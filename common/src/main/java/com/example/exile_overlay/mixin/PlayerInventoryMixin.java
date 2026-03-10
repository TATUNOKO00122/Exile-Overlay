package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to protect favorite items from being removed by other mods (Quickstack, etc.).
 * This mixin intercepts inventory removal operations and blocks them for favorite slots.
 * 
 * Player inventory slots:
 * - 0-8: Hotbar
 * - 9-35: Main inventory
 * - 36-39: Armor (boots, leggings, chestplate, helmet)
 * - 40: Offhand
 * 
 * CLIENT-ONLY mixin - applied via @Environment annotation.
 */
@Environment(EnvType.CLIENT)
@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/PlayerInventoryMixin");

    /**
     * Intercept removeItem to prevent removing items from favorite slots.
     * This method is used by mods like Quickstack to move items to containers.
     * 
     * @param slotIndex The slot index in player inventory (0-40)
     * @param count     The number of items to remove
     */
    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onRemoveItem(int slotIndex, int count, CallbackInfoReturnable<ItemStack> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        // Check if this slot is a favorite (player inventory slots 0-40)
        if (slotIndex >= 0 && slotIndex <= 40 && manager.isFavorite(slotIndex)) {
            // Return empty ItemStack to block the removal
            cir.setReturnValue(ItemStack.EMPTY);
            LOGGER.debug("Blocked removal from favorite slot {}", slotIndex);
        }
    }

    /**
     * Intercept removeItemNoUpdate to prevent removing items from favorite slots.
     * This is another method that might be used to remove items entirely.
     * 
     * @param slotIndex The slot index in player inventory (0-40)
     */
    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onRemoveItemNoUpdate(int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        // Check if this slot is a favorite (player inventory slots 0-40)
        if (slotIndex >= 0 && slotIndex <= 40 && manager.isFavorite(slotIndex)) {
            // Return empty ItemStack to block the removal
            cir.setReturnValue(ItemStack.EMPTY);
            LOGGER.debug("Blocked removal (no update) from favorite slot {}", slotIndex);
        }
    }
}
