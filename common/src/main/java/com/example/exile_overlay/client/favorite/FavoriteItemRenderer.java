package com.example.exile_overlay.client.favorite;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders favorite icon (star) on favorite slots in inventory screens.
 * 
 * 【機能】
 * - インベントリ画面でお気に入りスロットに星マークを表示
 * - 星はスロットの右上隅に表示
 * 
 * 【注意】
 * - このクラスはプラットフォーム別のイベントハンドラから呼び出される
 * - Fabric/Forgeで異なるイベントシステムを使用するため、
 *   実際のイベント購読はプラットフォーム別クラスで行う
 * - AbstractContainerScreenのprotectedフィールドへはMixinからアクセス可能
 */
public class FavoriteItemRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteItemRenderer.class);
    private static final int STAR_COLOR = 0xFFFFD700; // Gold color
    private static final int STAR_SHADOW_COLOR = 0xFF8B6914; // Darker gold for shadow

    /**
     * Render favorite stars on all favorite slots in a container screen.
     * Called from platform-specific event handlers (via Mixin).
     * 
     * @param graphics The GuiGraphics context
     * @param screen The container screen being rendered
     * @param guiLeft The left position of the GUI (from screen.leftPos)
     * @param guiTop The top position of the GUI (from screen.topPos)
     */
    public static void renderFavorites(GuiGraphics graphics, AbstractContainerScreen<?> screen, int guiLeft, int guiTop) {
        FavoriteItemManager manager = FavoriteItemManager.getInstance();
        if (manager == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        var container = screen.getMenu();

        // Render star on each favorite slot (player inventory slots only)
        for (Slot slot : container.slots) {
            // Check if this slot belongs to player inventory
            if (slot.container instanceof Inventory) {
                // getContainerSlot() returns the actual inventory index (0-40)
                // slot.index is the container menu position which varies by screen type
                int playerSlotId = slot.getContainerSlot();
                
                if (playerSlotId >= 0 && manager.isFavorite(playerSlotId)) {
                    int x = guiLeft + slot.x;
                    int y = guiTop + slot.y;
                    renderStar(graphics, x, y);
                }
            }
        }
    }

    /**
     * Render a small star icon at the top-right corner of a slot.
     */
    private static void renderStar(GuiGraphics graphics, int slotX, int slotY) {
        // Position star at top-right corner of the slot
        int starX = slotX + 10;
        int starY = slotY - 1;

        // Draw a simple star using text (★)
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300); // Render above items

        Minecraft mc = Minecraft.getInstance();
        
        // Shadow
        graphics.drawString(mc.font, "★", starX + 1, starY + 1, STAR_SHADOW_COLOR, false);
        // Main star
        graphics.drawString(mc.font, "★", starX, starY, STAR_COLOR, false);

        graphics.pose().popPose();
    }
}
