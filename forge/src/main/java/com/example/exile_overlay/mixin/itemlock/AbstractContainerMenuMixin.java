package com.example.exile_overlay.mixin.itemlock;

import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * バニラAbstractContainerMenuに対する安全防護Mixin。
 * ロック中スロットに対するクリック、Shift移動、ホットバー入替、ドロップ等を遮断する。
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$preventLockedContainerClicks(int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
        try {
            if (player == null) return;

            boolean isClient = player.level().isClientSide();

            // 1. 操作対象スロットがロックされている場合はすべての操作を遮断
            if (slotId >= 0 && slotId < this.slots.size()) {
                Slot slot = this.slots.get(slotId);
                int slotIdx = ItemLockHelper.getPlayerSlotIndex(slot, player);
                if (slotIdx >= 0) {
                    boolean locked = isClient
                            ? LockManager.isClientSlotLocked(slotIdx)
                            : LockManager.isServerSlotLocked(player, slotIdx);
                    if (locked) {
                        ci.cancel();
                        return;
                    }
                }
            }

            // 2. SWAP（ホットバー数字キー 0〜8 での入れ替え）の対象ホットバースロットがロックされている場合の遮断
            if (clickType == ClickType.SWAP && button >= 0 && button < 9) {
                boolean targetLocked = isClient
                        ? LockManager.isClientSlotLocked(button)
                        : LockManager.isServerSlotLocked(player, button);
                if (targetLocked) {
                    ci.cancel();
                }
            }
        } catch (Exception ignored) {
        }
    }
}
