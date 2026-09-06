package com.example.exile_overlay.mixin.itemlock;

import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * バニラSlotに対する安全防護Mixin。
 * ロックされたスロットからのアイテム取り出し（mayPickup）および外部配置（mayPlace）を防止する。
 */
@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow
    @Final
    public Container container;

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedSlotPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (player == null) return;
            Slot self = (Slot) (Object) this;
            int slotIdx = ItemLockHelper.getPlayerSlotIndex(self, player);
            if (slotIdx >= 0) {
                boolean locked = player.level().isClientSide()
                        ? LockManager.isClientSlotLocked(slotIdx)
                        : LockManager.isServerSlotLocked(player, slotIdx);
                if (locked) {
                    cir.setReturnValue(false);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedSlotPlace(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (this.container instanceof Inventory playerInv && playerInv.player != null) {
                Player player = playerInv.player;
                Slot self = (Slot) (Object) this;
                int slotIdx = ItemLockHelper.getPlayerSlotIndex(self, player);
                if (slotIdx >= 0) {
                    boolean locked = player.level().isClientSide()
                            ? LockManager.isClientSlotLocked(slotIdx)
                            : LockManager.isServerSlotLocked(player, slotIdx);
                    if (locked) {
                        cir.setReturnValue(false);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
