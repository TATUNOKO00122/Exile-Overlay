package com.example.exile_overlay.client.favorite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FavoriteItemRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteItemRenderer.class);
    private static final int STAR_COLOR = 0xFFFFD700;
    private static final int STAR_SHADOW_COLOR = 0xFF8B6914;

    public static void renderFavorites(GuiGraphics graphics, AbstractContainerScreen<?> screen, int guiLeft, int guiTop) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        Inventory playerInventory = mc.player.getInventory();
        var container = screen.getMenu();

        for (Slot slot : container.slots) {
            int playerSlotId = manager.toPlayerSlotId(slot, playerInventory);
            if (playerSlotId >= 0 && manager.isFavorite(playerSlotId)) {
                int x = guiLeft + slot.x;
                int y = guiTop + slot.y;
                renderStar(graphics, x, y);
            }
        }
    }

    private static void renderStar(GuiGraphics graphics, int slotX, int slotY) {
        int starX = slotX + 10;
        int starY = slotY - 1;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);

        Minecraft mc = Minecraft.getInstance();
        
        graphics.drawString(mc.font, "★", starX + 1, starY + 1, STAR_SHADOW_COLOR, false);
        graphics.drawString(mc.font, "★", starX, starY, STAR_COLOR, false);

        graphics.pose().popPose();
    }
}