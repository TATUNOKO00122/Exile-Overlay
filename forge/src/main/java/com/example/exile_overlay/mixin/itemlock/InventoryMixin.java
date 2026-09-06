package com.example.exile_overlay.mixin.itemlock;

import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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

    @Shadow
    @Final
    public NonNullList<ItemStack> items;

    @Shadow
    @Final
    public NonNullList<ItemStack> armor;

    @Shadow
    @Final
    public NonNullList<ItemStack> offhand;

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

    @Inject(method = "removeItem(II)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedRemoveItem(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        try {
            if (this.player != null && slot >= 0 && slot < ItemLockHelper.INVENTORY_SIZE) {
                boolean locked = this.player.level().isClientSide()
                        ? LockManager.isClientSlotLocked(slot)
                        : LockManager.isServerSlotLocked(this.player, slot);
                if (locked) {
                    cir.setReturnValue(ItemStack.EMPTY);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedRemoveItemNoUpdate(int slot, CallbackInfoReturnable<ItemStack> cir) {
        try {
            if (this.player != null && slot >= 0 && slot < ItemLockHelper.INVENTORY_SIZE) {
                boolean locked = this.player.level().isClientSide()
                        ? LockManager.isClientSlotLocked(slot)
                        : LockManager.isServerSlotLocked(this.player, slot);
                if (locked) {
                    cir.setReturnValue(ItemStack.EMPTY);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Inject(method = "clearContent", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedClearContent(CallbackInfo ci) {
        try {
            if (this.player != null) {
                boolean isClient = this.player.level().isClientSide();
                long mask = isClient
                        ? LockManager.getClientLockedMask()
                        : LockManager.getServerLockedMask(this.player);
                if (mask != 0L) {
                    ci.cancel();
                    for (int i = 0; i < ItemLockHelper.INVENTORY_SIZE; i++) {
                        boolean locked = isClient
                                ? LockManager.isClientSlotLocked(i)
                                : LockManager.isServerSlotLocked(this.player, i);
                        if (!locked) {
                            this.items.set(i, ItemStack.EMPTY);
                        }
                    }
                    this.armor.clear();
                    this.offhand.clear();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
