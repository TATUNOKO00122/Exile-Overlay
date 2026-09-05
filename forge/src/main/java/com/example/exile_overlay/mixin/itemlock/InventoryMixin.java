package com.example.exile_overlay.mixin.itemlock;

import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * バニラのプレイヤーインベントリに対する安全防護Mixin。
 * ロックされたスロットのドロップや直接除去を防止する。
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Shadow
    public int selected;

    @Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedDrop(boolean entireStack, CallbackInfoReturnable<ItemStack> cir) {
        try {
            if (this.player != null && this.selected >= 0 && this.selected < ItemLockHelper.INVENTORY_SIZE) {
                boolean locked = this.player.level().isClientSide()
                        ? LockManager.isClientSlotLocked(this.selected)
                        : LockManager.isServerSlotLocked(this.player, this.selected);
                if (locked) {
                    cir.setReturnValue(ItemStack.EMPTY);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
