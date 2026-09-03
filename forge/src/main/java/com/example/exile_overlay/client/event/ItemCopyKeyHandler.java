package com.example.exile_overlay.client.event;

import com.example.exile_overlay.compat.jei.ExileOverlayJeiPlugin;
import com.example.exile_overlay.util.DropItemResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public final class ItemCopyKeyHandler {

    private static ItemStack lastTooltipStack = ItemStack.EMPTY;
    private static long lastTooltipTime = 0;

    private ItemCopyKeyHandler() {
    }

    /**
     * 画面上のアイテムにマウスが乗ってツールチップが表示された瞬間にアイテムを捕捉する。
     * バニラインベントリ、JEI、EMI、REIなど、あらゆるGUIで共通して発火する。
     */
    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack != null && !stack.isEmpty()) {
            lastTooltipStack = stack.copy();
            lastTooltipTime = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (!Screen.hasControlDown() || event.getKeyCode() != GLFW.GLFW_KEY_C) {
            return;
        }

        Screen screen = event.getScreen();
        if (screen == null) {
            return;
        }

        // テキスト入力欄にフォーカスがある場合は通常のクリップボード操作を優先
        if (screen.getFocused() instanceof EditBox) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double mouseY = mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();

        ItemStack targetStack = ItemStack.EMPTY;

        // 1. 直近にツールチップが描画されていたアイテムを最優先（JEI・EMI・バニラ全て対応）
        if (!lastTooltipStack.isEmpty() && (System.currentTimeMillis() - lastTooltipTime) < 500) {
            targetStack = lastTooltipStack;
        }

        // 2. JEI API からの直接取得
        if (targetStack.isEmpty() && ModList.get().isLoaded("jei")) {
            try {
                targetStack = ExileOverlayJeiPlugin.getItemStackUnderMouse(mouseX, mouseY);
            } catch (Throwable ignored) {
            }
        }

        // 3. バニラインベントリスロットの判定（マウス座標が実際にスロット枠内にあるか厳密にチェック）
        if (targetStack.isEmpty() && screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) {
                int guiLeft = containerScreen.getGuiLeft();
                int guiTop = containerScreen.getGuiTop();
                double slotMinX = guiLeft + slot.x;
                double slotMaxX = slotMinX + 16;
                double slotMinY = guiTop + slot.y;
                double slotMaxY = slotMinY + 16;

                if (mouseX >= slotMinX && mouseX <= slotMaxX && mouseY >= slotMinY && mouseY <= slotMaxY) {
                    targetStack = slot.getItem();
                }
            }
        }

        if (!targetStack.isEmpty()) {
            copyItemFilterId(targetStack);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) {
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS && Screen.hasControlDown() && event.getKey() == GLFW.GLFW_KEY_C) {
            ItemStack mainHand = mc.player.getMainHandItem();
            if (!mainHand.isEmpty()) {
                copyItemFilterId(mainHand);
            }
        }
    }

    private static void copyItemFilterId(ItemStack stack) {
        String filterId = DropItemResolver.resolveFilterId(stack);
        if (filterId == null || filterId.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.keyboardHandler.setClipboard(filterId);
        mc.gui.setOverlayMessage(
                Component.translatable("exile_overlay.message.copied_filter_id", filterId),
                false
        );
    }
}
