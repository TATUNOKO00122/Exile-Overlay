package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.HudFontConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * HUDテキスト描画の統一ヘルパー。
 * カスタムフォント設定に応じた描画APIを提供する。Style.withFont方式利用。
 */
public final class HudFontHelper {

    private static final HudFontConfig FONT_CONFIG = HudFontConfig.getInstance();
    private static final ResourceLocation HUD_FONT = ResourceLocation.tryParse("exile_overlay:hud_font");
    private static final Style HUD_FONT_STYLE = Style.EMPTY.withFont(HUD_FONT);

    private HudFontHelper() {
    }

    public static boolean isCustomFontEnabled() {
        return FONT_CONFIG.isUseCustomFont();
    }

    public static Component styledText(String text) {
        if (isCustomFontEnabled()) {
            return Component.literal(text).withStyle(HUD_FONT_STYLE);
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
