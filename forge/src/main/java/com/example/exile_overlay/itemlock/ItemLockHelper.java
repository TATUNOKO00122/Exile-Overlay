package com.example.exile_overlay.itemlock;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

/**
 * スロットの絶対インデックス（0〜35）を特定するユーティリティ。
 * 大容量チェストや特殊MOD画面であってもプレイヤー自身のスロットのみを特定する。
 */
public final class ItemLockHelper {
    public static final int INVENTORY_SIZE = 36;

    private ItemLockHelper() {}

    /**
     * 指定されたスロットがプレイヤーのメインインベントリ（ホットバー含む 0〜35）であればその番号を返す。
     * チェストや防具スロット、外部コンテナの場合は -1 を返す。
     */
    public static int getPlayerSlotIndex(Slot slot, Player player) {
        if (slot == null || player == null) {
            return -1;
        }

        // 1. バニラ標準スロット（チェスト、作業台、かまど等を開いている時も有効）
        if (slot.container == player.getInventory()) {
            int slotIdx = slot.getContainerSlot();
            if (slotIdx >= 0 && slotIdx < INVENTORY_SIZE) {
                return slotIdx;
            }
            return -1;
        }

        // 2. Forge SlotItemHandler（一部のMODコンテナ対応）
        if (slot instanceof SlotItemHandler handlerSlot) {
            IItemHandler handler = handlerSlot.getItemHandler();
            if (handler instanceof PlayerMainInvWrapper || handler instanceof PlayerInvWrapper) {
                int slotIdx = handlerSlot.getSlotIndex();
                if (slotIdx >= 0 && slotIdx < INVENTORY_SIZE) {
                    return slotIdx;
                }
            }
        }

        return -1;
    }
}
