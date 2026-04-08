package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class FavoriteItemMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/FavoriteItemMixin");

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onSlotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        try {
            if (slot == null) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            FavoriteItemManager manager = FavoriteItemManager.getInstance();
            if (manager == null || !manager.isEnabled()) {
                return;
            }

            Inventory playerInventory = mc.player.getInventory();
            int playerSlotId = manager.toPlayerSlotId(slot, playerInventory);
            if (playerSlotId < 0) {
                return;
            }

            if (manager.isToggleKeyPressed()) {
                manager.toggleFavorite(playerSlotId);
                ci.cancel();
                return;
            }

            if (manager.isFavorite(playerSlotId)) {
                ci.cancel();
                LOGGER.debug("Blocked click on favorite slot {}", playerSlotId);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle slotClicked", e);
        }
    }
}