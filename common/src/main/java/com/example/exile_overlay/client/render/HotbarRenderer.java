package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.render.orb.OrbRegistry;
import com.example.exile_overlay.client.render.orb.OrbRenderer;
import com.example.exile_overlay.client.render.orb.OrbType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class HotbarRenderer {

    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("exile_overlay", "textures/gui/slot.png");

    private static final int BG_WIDTH = 640;
    private static final int BG_HEIGHT = 256;
    private static final int SLOT_TEX_SIZE = 29;
    private static final int SLOT_DISPLAY_SIZE = 29;
    private static final int SLOT_GAP = 1;

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR) {
            return;
        }

        // デフォルト位置（画面下部中央）
        float scale = 0.5f;
        int bgX = (int) (screenWidth / 2 - (BG_WIDTH * scale) / 2);
        int bgY = screenHeight - (int) (BG_HEIGHT * scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(bgX, bgY, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        // --- 経験値バー ---
        renderExpBars(graphics, mc);

        // --- オーブ描画 ---
        renderOrbs(graphics, mc);

        // --- 背景フレーム ---
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(BACKGROUND_TEXTURE, 0, 0, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        // --- レベル表示 ---
        renderLevelDisplay(graphics, mc);

        // --- ホットバースロット ---
        renderHotbarSlots(graphics, mc);

        graphics.pose().popPose();
    }

    /**
     * 経験値バーを描画
     */
    private static void renderExpBars(GuiGraphics graphics, Minecraft mc) {
        int barX = 95;
        int barWidth = 449;

        // MOD経験値 (黄色) - ModDataProviderRegistry経由で取得
        float currentModExp = ModDataProviderRegistry.getExp(mc.player);
        float maxModExp = ModDataProviderRegistry.getExpRequiredForLevelUp(mc.player);
        float modXpProgress = maxModExp > 0 ? currentModExp / maxModExp : 0;

        int modBarY = 252;
        graphics.fill(barX, modBarY, barX + barWidth, modBarY + 2, 0xFF808080);
        if (modXpProgress > 0) {
            int filledWidth = (int) (barWidth * modXpProgress);
            graphics.fill(barX, modBarY, barX + filledWidth, modBarY + 2, 0xFFFFFF99);
        }

        // バニラ 経験値 (緑)
        float vanillaXpProgress = mc.player.experienceProgress;
        int vanillaBarY = 249;
        graphics.fill(barX, vanillaBarY, barX + barWidth, vanillaBarY + 2, 0xFF808080);
        if (vanillaXpProgress > 0) {
            int filledWidth = (int) (barWidth * vanillaXpProgress);
            graphics.fill(barX, vanillaBarY, barX + filledWidth, vanillaBarY + 2, 0xFF00CC00);
        }
    }

    /**
     * 全てのオーブを描画
     */
    private static void renderOrbs(GuiGraphics graphics, Minecraft mc) {
        List<OrbType> visibleOrbs = OrbRegistry.getVisibleOrbs(mc.player);
        
        for (OrbType orbType : visibleOrbs) {
            OrbRenderer.render(graphics, orbType.getConfig(), mc.player, mc);
        }
    }

    /**
     * レベル表示を描画
     */
    private static void renderLevelDisplay(GuiGraphics graphics, Minecraft mc) {
        int vanillaLevel = mc.player.experienceLevel;
        int modLevel = ModDataProviderRegistry.getLevel(mc.player);
        String levelStr = vanillaLevel + " / " + modLevel;
        OrbRenderer.renderCenteredScaledText(graphics, mc, levelStr, 319, 204, 0.7f, 0xFFFFFFFF);
    }

    /**
     * ホットバースロットを描画
     */
    private static void renderHotbarSlots(GuiGraphics graphics, Minecraft mc) {
        int slotStartX = 185;
        int slotStartY = 219;
        int slotPitch = SLOT_DISPLAY_SIZE + SLOT_GAP;
        int selectedSlot = mc.player.getInventory().selected;

        for (int i = 0; i < 9; i++) {
            int slotX = slotStartX + (i * slotPitch);
            int slotY = slotStartY;

            if (i == selectedSlot) {
                graphics.fill(slotX + 2, slotY + 2, slotX + SLOT_DISPLAY_SIZE - 2, slotY + SLOT_DISPLAY_SIZE - 2,
                        0x80FFFFFF);
            }

            graphics.blit(SLOT_TEXTURE, slotX, slotY, SLOT_DISPLAY_SIZE, SLOT_DISPLAY_SIZE, 0, 0, SLOT_TEX_SIZE,
                    SLOT_TEX_SIZE, SLOT_TEX_SIZE, SLOT_TEX_SIZE);

            ItemStack stack = mc.player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(slotX + 3.5f, slotY + 3.5f, 0);
                graphics.pose().scale(1.375f, 1.375f, 1.0f);
                graphics.renderItem(stack, 0, 0);
                graphics.renderItemDecorations(mc.font, stack, 0, 0);
                graphics.pose().popPose();
            }
        }
    }
}
