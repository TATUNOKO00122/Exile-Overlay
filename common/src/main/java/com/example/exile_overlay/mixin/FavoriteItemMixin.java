package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to protect favorite items from all operations in container screens.
 * ALT + Click toggles favorite status.
 * 
 * CLIENT-ONLY mixin.
 */
@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class FavoriteItemMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/FavoriteItemMixin");

    /**
     * Convert a slot to player inventory slot ID.
     * Returns -1 if not a player inventory slot.
     */
    @Unique
    private int exileOverlay$toPlayerSlotId(Slot slot) {
        if (slot == null) {
            return -1;
        }
        // Check if this slot belongs to player inventory
        if (slot.container instanceof Inventory) {
            // getContainerSlot() returns the actual inventory index (0-40)
            // slot.index is the container menu position which varies by screen type
            return slot.getContainerSlot();
        }
        return -1;
    }

    /**
     * Intercept mouse clicks to handle ALT+click for favorites and block operations on favorites.
     */
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onSlotClicked(Slot slot, int slotId, int mouseButton, ClickType type, CallbackInfo ci) {
        if (slot == null) {
            return;
        }

        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return; // Skip on server side
        }

        int playerSlotId = exileOverlay$toPlayerSlotId(slot);
        if (playerSlotId < 0) {
            return;
        }

        // Check for ALT key press to toggle favorite
        long window = org.lwjgl.glfw.GLFW.glfwGetCurrentContext();
        boolean isAltPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS ||
                               GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS;

        if (isAltPressed) {
            // Toggle favorite status
            manager.toggleFavorite(playerSlotId);
            ci.cancel();
            return;
        }

        // Block all click operations on favorites
        if (manager.isFavorite(playerSlotId)) {
            ci.cancel();
        }
    }
}
