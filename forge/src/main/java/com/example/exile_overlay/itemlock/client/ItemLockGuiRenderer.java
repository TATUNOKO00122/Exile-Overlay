package com.example.exile_overlay.itemlock.client;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.itemlock.ItemLockHelper;
import com.example.exile_overlay.itemlock.LockManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * ロックされたスロットに南京錠アイコンを描画するGUIハンドラ。
 * 大容量チェストや特殊コンテナ画面でもプレイヤーのスロットに正確に重ねて描画する。
 */
public final class ItemLockGuiRenderer {
    public static final ResourceLocation LOCK_ICON = new ResourceLocation(ExileOverlayMod.MOD_ID, "textures/gui/lock_icon.png");

    private ItemLockGuiRenderer() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ItemLockGuiRenderer());
    }

    /**
     * 南京錠アイコン（8x8ピクセル）を描画
     */
    public static void renderLockIcon(GuiGraphics graphics, int x, int y) {
        RenderSystem.enableBlend();
        graphics.blit(LOCK_ICON, x, y, 0, 0, 8, 8, 8, 8);
        RenderSystem.disableBlend();
    }

    /**
     * 各種コンテナ画面（インベントリ、チェスト、作業台等）でのスロット描画
     */
    @SubscribeEvent
    public void onContainerRenderForeground(ContainerScreenEvent.Render.Foreground event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen == null || screen.getMenu() == null) return;

        for (Slot slot : screen.getMenu().slots) {
            int playerSlot = ItemLockHelper.getPlayerSlotIndex(slot, mc.player);
            if (playerSlot >= 0 && LockManager.isClientSlotLocked(playerSlot)) {
                // スロット枠（16x16）の右上（x + 9, y - 1）に南京錠アイコンを描画
                renderLockIcon(event.getGuiGraphics(), slot.x + 9, slot.y - 1);
            }
        }
    }

    /**
     * 通常画面のバニラホットバーHUD上での描画（exile_overlayのホットバーが無効な場合のフォールバック）
     */
    @SubscribeEvent
    public void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int left = screenWidth / 2 - 90;
        int top = screenHeight - 22;

        for (int i = 0; i < 9; i++) {
            if (LockManager.isClientSlotLocked(i)) {
                int slotX = left + i * 20 + 3;
                int slotY = top + 3;
                renderLockIcon(event.getGuiGraphics(), slotX + 9, slotY - 1);
            }
        }
    }
}
