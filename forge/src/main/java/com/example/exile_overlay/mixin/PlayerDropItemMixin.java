package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.favorite.FavoriteItemManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class PlayerDropItemMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/PlayerDropItemMixin");

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$onDrop(boolean dropAll, CallbackInfoReturnable<ItemEntity> cir) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;
        int selectedSlot = self.getInventory().selected;

        if (manager.isFavorite(selectedSlot)) {
            cir.setReturnValue(null);
            LOGGER.debug("Blocked drop of favorite item in slot {}", selectedSlot);
        }
    }
}