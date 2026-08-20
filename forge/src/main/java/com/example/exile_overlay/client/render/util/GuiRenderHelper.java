package com.example.exile_overlay.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * 2D GUI HUD 描画用の共通ヘルパー
 */
public final class GuiRenderHelper {

    private GuiRenderHelper() {}

    /**
     * ブレンドモードを有効にしてテクスチャを全画面/指定領域に描画
     */
    public static void drawTexturedRect(GuiGraphics graphics, ResourceLocation texture,
                                       int x, int y, int width, int height,
                                       float uMin, float vMin, float uMax, float vMax,
                                       int textureWidth, int textureHeight) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(texture, x, y, width, height, uMin * textureWidth, vMin * textureHeight,
                (int) ((uMax - uMin) * textureWidth), (int) ((vMax - vMin) * textureHeight),
                textureWidth, textureHeight);
        RenderSystem.disableBlend();
    }

    /**
     * アウトライン付きテキスト描画 (黒シャドウ/縁取り)
     */
    public static void drawOutlinedText(GuiGraphics graphics, Font font, String text, int x, int y, int color, int outlineColor) {
        // 上下左右に影を描画
        graphics.drawString(font, text, x - 1, y, outlineColor, false);
        graphics.drawString(font, text, x + 1, y, outlineColor, false);
        graphics.drawString(font, text, x, y - 1, outlineColor, false);
        graphics.drawString(font, text, x, y + 1, outlineColor, false);
        // 本文描画
        graphics.drawString(font, text, x, y, color, false);
    }
}
