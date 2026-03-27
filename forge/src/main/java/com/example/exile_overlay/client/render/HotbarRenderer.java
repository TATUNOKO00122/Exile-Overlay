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
 * 
 * 【パフォーマンス最適化】
 * - StringBuilder再利用によるGCプレッシャー低減
 * - 変更検出による不要な文字列生成の回避
 */
public class HotbarRenderer {
    
    /**
     * 文字列フォーマットキャッシュ
     * StringBuilderを再利用してGCプレッシャーを低減
     */
    private static final class StringFormatCache {
        private final StringBuilder sb = new StringBuilder(32);
        private int lastVanillaLevel = -1;
        private int lastModLevel = -1;
        private String cached = "";
        
        /**
         * レベル表示文字列を取得（キャッシュ利用）
         */
        String formatLevel(int vanillaLevel, int modLevel) {
            if (vanillaLevel != lastVanillaLevel || modLevel != lastModLevel) {
                sb.setLength(0);
                sb.append(vanillaLevel).append(" / ").append(modLevel);
                cached = sb.toString();
                lastVanillaLevel = vanillaLevel;
                lastModLevel = modLevel;
            }
            return cached;
        }
        
        /**
         * キャッシュを無効化
         */
        void invalidate() {
            lastVanillaLevel = -1;
            lastModLevel = -1;
            cached = "";
        }
    }
    
    // シングルトンキャッシュインスタンス
    private static final StringFormatCache levelFormatCache = new StringFormatCache();
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
    private static final int SLOT_START_Y = 218;
    private static final int SLOT_PITCH = SLOT_DISPLAY_SIZE + SLOT_GAP;

    // 経験値バー定数
    private static final int EXP_BAR_X = 65;
    private static final int EXP_BAR_WIDTH = 509;
    private static final int MOD_EXP_BAR_Y = 252;
    private static final int VANILLA_EXP_BAR_Y = 249;
    private static final int EXP_BAR_HEIGHT = 2;

    // テキスト定数（中心座標、HJUDと同じ値）
    private static final float LEVEL_CENTER_X = 320.0f;
    private static final float LEVEL_CENTER_Y = 202.5f;

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
        // MOD経験値 (くすんだ黄土色)
        float currentModExp = ModDataProviderRegistry.getValue(mc.player, DataType.EXP);
        float maxModExp = ModDataProviderRegistry.getMaxValue(mc.player, DataType.EXP_REQUIRED);
        renderSingleExpBar(graphics, MOD_EXP_BAR_Y, currentModExp, maxModExp, 0xFFB8A05C);

        // バニラ経験値 (オリーブ系)
        renderSingleExpBar(graphics, VANILLA_EXP_BAR_Y, mc.player.experienceProgress, 1.0f, 0xFF4D8B4D);
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
        
        String vanillaStr = String.valueOf(vanillaLevel);
        String separator = " / ";
        String modStr = String.valueOf(modLevel);
        
        // 各テキストの幅を計算
        int vanillaWidth = mc.font.width(vanillaStr);
        int sepWidth = mc.font.width(separator);
        int modWidth = mc.font.width(modStr);
        int totalWidth = vanillaWidth + sepWidth + modWidth;
        int textHeight = mc.font.lineHeight;
        
        // HJUDと同じ計算方法
        float maxWidth = 27.0f;
        float scale = Math.min(maxWidth / totalWidth, 1.0f);
        scale = Math.min(scale, 0.8f);
        
        graphics.pose().pushPose();
        graphics.pose().translate(LEVEL_CENTER_X, LEVEL_CENTER_Y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        
        int startX = -totalWidth / 2;
        int textY = -textHeight / 2 + 1;
        
        // バニラレベル（緑）
        graphics.drawString(mc.font, vanillaStr, startX, textY, 0x55FF55, false);
        
        // 区切り（白）
        graphics.drawString(mc.font, separator, startX + vanillaWidth, textY, 0xFFFFFF, false);
        
        // M\u0026Sレベル（黄色）
        graphics.drawString(mc.font, modStr, startX + vanillaWidth + sepWidth, textY, 0xFFFF55, false);
        
        graphics.pose().popPose();
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
