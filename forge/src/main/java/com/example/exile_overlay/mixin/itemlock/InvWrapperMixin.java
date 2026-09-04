package com.example.exile_overlay.mixin.itemlock;

import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ForgeのInvWrapperに対する保護Mixin。
 * 外部MOD（Quick Stack等）がプレイヤーインベントリからアイテムを引き抜くのを防ぐ。
 */
@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperMixin {

    @Shadow(remap = false)
    public abstract Container getInv();

    @Inject(method = "extractItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void exileOverlay$preventLockedSlotExtraction(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        try {
            Container container = this.getInv();
            if (container instanceof Inventory playerInv && playerInv.player != null) {
                if (slot >= 0 && slot < ItemLockHelper.INVENTORY_SIZE) {
                    if (LockManager.isServerSlotLocked(playerInv.player, slot)) {
                        cir.setReturnValue(ItemStack.EMPTY);
                    }
                }
            }
        } catch (Exception ignored) {
            // 安全第一: 例外時は通常処理を継続
        }
    }
}
