package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class PlayerInventoryMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/PlayerInventoryMixin");

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onRemoveItem(int slotIndex, int count, CallbackInfoReturnable<ItemStack> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        if (manager.isBypassActive()) {
            return;
        }

        if (slotIndex >= 0 && slotIndex <= 40 && manager.isFavorite(slotIndex)) {
            cir.setReturnValue(ItemStack.EMPTY);
            LOGGER.debug("Blocked removal from favorite slot {}", slotIndex);
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onRemoveItemNoUpdate(int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        if (manager.isBypassActive()) {
            return;
        }

        if (slotIndex >= 0 && slotIndex <= 40 && manager.isFavorite(slotIndex)) {
            cir.setReturnValue(ItemStack.EMPTY);
            LOGGER.debug("Blocked removal (no update) from favorite slot {}", slotIndex);
        }
    }
}