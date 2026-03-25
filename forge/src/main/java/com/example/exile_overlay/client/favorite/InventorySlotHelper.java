package com.example.exile_overlay.client.favorite;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class InventorySlotHelper {

    private InventorySlotHelper() {}

    public static int getSlotFromIncrement(ContainerType type, int increment) {
        if (type == null) {
            return increment;
        }
        switch (type) {
            case HOTBAR:
                return getHotbarSlotFromIncrement(increment);
            case PLAYER_INVENTORY:
                return getInventorySlotFromIncrement(increment);
            case CHEST:
                return getChestSlotFromIncrement(increment);
            default:
                return increment;
        }
    }

    private static int getHotbarSlotFromIncrement(int increment) {
        return increment + 27;
    }

    private static int getInventorySlotFromIncrement(int increment) {
        if (increment >= 0 && increment <= 8) {
            return increment + 27;
        } else if (increment >= 9 && increment <= 35) {
            return increment - 9;
        } else if (increment >= 36 && increment <= 39) {
            return increment;
        }
        return increment;
    }

    private static int getChestSlotFromIncrement(int increment) {
        if (increment >= 0 && increment <= 8) {
            return increment + 27;
        } else if (increment >= 9 && increment <= 35) {
            return increment - 9;
        }
        return increment;
    }

    public static int toPlayerSlotId(Slot slot, Inventory playerInventory) {
        if (slot == null || playerInventory == null) {
            return -1;
        }
        if (slot.container == playerInventory) {
            return slot.getContainerSlot();
        }
        return -1;
    }
}