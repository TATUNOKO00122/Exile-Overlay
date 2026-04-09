package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.HudFontConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * HUDテキスト描画の統一ヘルパー
 *
 * 【責任】
 * - カスタムフォント設定に応じたテキスト描画の統一API提供
 * - Minecraftネイティブフォント方式（Style.withFont）を使用
 */
public final class HudFontHelper {

    private static final ResourceLocation HUD_FONT = ResourceLocation.tryParse("exile_overlay:hud_font");

    private HudFontHelper() {
    }

    public static boolean isCustomFontEnabled() {
        return HudFontConfig.getInstance().isUseCustomFont();
    }

    public static Component styledText(String text) {
        if (isCustomFontEnabled()) {
            return Component.literal(text).withStyle(Style.EMPTY.withFont(HUD_FONT));
        }
        return Component.literal(text);
    }

    public static int getTextWidth(Font font, String text) {
        if (isCustomFontEnabled()) {
            return font.width(styledText(text));
        }
        return font.width(text);
    }

    public static void drawString(GuiGraphics graphics, Font font, String text, float x, float y, int color, boolean shadow) {
        if (isCustomFontEnabled()) {
            graphics.drawString(font, styledText(text), (int) x, (int) y, color, shadow);
        } else {
            graphics.drawString(font, text, (int) x, (int) y, color, shadow);
        }
    }
}
