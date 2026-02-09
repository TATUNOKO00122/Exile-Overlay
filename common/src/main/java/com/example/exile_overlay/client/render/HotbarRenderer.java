package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.client.render.orb.OrbRegistry;
import com.example.exile_overlay.client.render.orb.OrbRenderer;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.example.exile_overlay.client.render.orb.OrbType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.exile_overlay.client.render.orb.OrbType.ORB_3;

/**
 * ホットバー描画クラス
 * 
 * 【Layered Sandwich Rendering Architecture】
 * 描画順序（下から上）：
 * 1. Background Layer (背面): 経験値バーなど
 * 2. Fill Layer (中間): オーブ液面（シェーダーで描画）
 * 3. Frame Layer (前面): 背景フレーム（透明マスクで円形を形成）
 * 4. Overlay Layer (最前面): 反射、テキスト、スロット
 * 
 * 背景画像の透明部分を「抜型」として利用し、中間層の四角が円形に見える
 */
public class HotbarRenderer {
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background.png");
    private static final ResourceLocation BACKGROUND_NO_ORBS_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background_no_orbs.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("exile_overlay", "textures/gui/slot.png");

    private static final int BG_WIDTH = 640;
    private static final int BG_HEIGHT = 256;
    private static final float RENDER_SCALE = 0.5f;

    // スロット定数
    private static final int SLOT_TEX_SIZE = 29;
    private static final int SLOT_DISPLAY_SIZE = 29;
    private static final int SLOT_GAP = 1;
    private static final int SLOT_START_X = 185;
    private static final int SLOT_START_Y = 219;
    private static final int SLOT_PITCH = SLOT_DISPLAY_SIZE + SLOT_GAP;

    // 経験値バー定数
    private static final int EXP_BAR_X = 95;
    private static final int EXP_BAR_WIDTH = 449;
    private static final int MOD_EXP_BAR_Y = 252;
    private static final int VANILLA_EXP_BAR_Y = 249;
    private static final int EXP_BAR_HEIGHT = 2;

    // テキスト定数
    private static final int LEVEL_TEXT_X = 319;
    private static final int LEVEL_TEXT_Y = 204;

    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || (mc.gameMode != null
                && mc.gameMode.getPlayerMode() == net.minecraft.world.level.GameType.SPECTATOR)) {
            return;
        }

        // 表示中のオーブを一度だけ取得してキャッシュ
        List<OrbType> visibleOrbs = OrbRegistry.getVisibleOrbs(mc.player);

        // デフォルト位置（画面下部中央）
        int bgX = (int) (screenWidth / 2 - (BG_WIDTH * RENDER_SCALE) / 2);
        int bgY = screenHeight - (int) (BG_HEIGHT * RENDER_SCALE);

        OrbShaderRenderer.updateAnimationTime(0.016f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(bgX, bgY, 0);
        graphics.pose().scale(RENDER_SCALE, RENDER_SCALE, 1.0f);

        // Layer 1: Background Layer (背面)
        renderExpBars(graphics, mc);

        // Layer 2: Fill Layer (中間)
        visibleOrbs.forEach(orbType -> OrbRenderer.renderFillLayer(graphics, orbType.getConfig(), mc.player));

        // Layer 3: Frame Layer (前面)
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        ResourceLocation bgTex = selectBackgroundTexture(visibleOrbs, mc.player);
        graphics.blit(bgTex, 0, 0, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

        // Layer 4: Overlay Layer (最前面)
        visibleOrbs.forEach(orbType -> OrbRenderer.renderOverlayLayer(graphics, orbType.getConfig(), mc.player, mc));
        renderLevelDisplay(graphics, mc);
        renderHotbarSlots(graphics, mc);

        graphics.pose().popPose();
    }

    private static void renderExpBars(GuiGraphics graphics, Minecraft mc) {
        // MOD経験値 (黄色)
        float currentModExp = ModDataProviderRegistry.getValue(mc.player, DataType.EXP);
        float maxModExp = ModDataProviderRegistry.getMaxValue(mc.player, DataType.EXP_REQUIRED);
        renderSingleExpBar(graphics, MOD_EXP_BAR_Y, currentModExp, maxModExp, 0xFFFFFF99);

        // バニラ経験値 (緑)
        renderSingleExpBar(graphics, VANILLA_EXP_BAR_Y, mc.player.experienceProgress, 1.0f, 0xFF00CC00);
    }

    private static void renderSingleExpBar(GuiGraphics graphics, int y, float current, float max, int color) {
        float progress = max > 0 ? Math.min(current / max, 1.0f) : 0;

        // 背景
        graphics.fill(EXP_BAR_X, y, EXP_BAR_X + EXP_BAR_WIDTH, y + EXP_BAR_HEIGHT, 0xFF808080);

        // プログレス
        if (progress > 0) {
            int filledWidth = (int) (EXP_BAR_WIDTH * progress);
            graphics.fill(EXP_BAR_X, y, EXP_BAR_X + filledWidth, y + EXP_BAR_HEIGHT, color);
        }
    }

    private static void renderLevelDisplay(GuiGraphics graphics, Minecraft mc) {
        int vanillaLevel = mc.player.experienceLevel;
        int modLevel = (int) ModDataProviderRegistry.getValue(mc.player, DataType.LEVEL);
        String levelStr = vanillaLevel + " / " + modLevel;
        OrbRenderer.renderCenteredScaledText(graphics, mc, levelStr, LEVEL_TEXT_X, LEVEL_TEXT_Y, 0.7f, 0xFFFFFFFF);
    }

    private static ResourceLocation selectBackgroundTexture(List<OrbType> visibleOrbs, Player player) {
        // ORB_3が非表示の場合は、no_orbsテクスチャを使用
        if (!OrbRegistry.isOrbVisible(player, ORB_3)) {
            return BACKGROUND_NO_ORBS_TEXTURE;
        }
        return BACKGROUND_TEXTURE;
    }

    private static void renderHotbarSlots(GuiGraphics graphics, Minecraft mc) {
        int selectedSlot = mc.player.getInventory().selected;

        for (int i = 0; i < 9; i++) {
            int slotX = SLOT_START_X + (i * SLOT_PITCH);

            if (i == selectedSlot) {
                graphics.fill(slotX + 2, SLOT_START_Y + 2, slotX + SLOT_DISPLAY_SIZE - 2,
                        SLOT_START_Y + SLOT_DISPLAY_SIZE - 2,
                        0x80FFFFFF);
            }

            graphics.blit(SLOT_TEXTURE, slotX, SLOT_START_Y, SLOT_DISPLAY_SIZE, SLOT_DISPLAY_SIZE, 0, 0, SLOT_TEX_SIZE,
                    SLOT_TEX_SIZE, SLOT_TEX_SIZE, SLOT_TEX_SIZE);

            ItemStack stack = mc.player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                renderHotbarItem(graphics, mc, stack, slotX, SLOT_START_Y);
            }
        }
    }

    private static void renderHotbarItem(GuiGraphics graphics, Minecraft mc, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 3.5f, y + 3.5f, 0);
        graphics.pose().scale(1.375f, 1.375f, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(mc.font, stack, 0, 0);
        graphics.pose().popPose();
    }
}
