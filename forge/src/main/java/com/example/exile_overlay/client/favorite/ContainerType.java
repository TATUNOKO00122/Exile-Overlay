package com.example.exile_overlay.client.favorite;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public enum ContainerType {
    PLAYER_INVENTORY,
    CHEST,
    HOTBAR;

    public static ContainerType match(Screen screen) {
        if (screen == null) {
            return HOTBAR;
        }
        if (screen instanceof InventoryScreen) {
            return PLAYER_INVENTORY;
        }
        if (screen instanceof ContainerScreen) {
            return CHEST;
        }
        return PLAYER_INVENTORY;
    }
}