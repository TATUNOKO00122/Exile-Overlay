package com.example.exile_overlay.client.dungeon;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class RollingDigit {

    private static final long DURATION_MS = 320L;

    private int currentValue = 0;
    private int previousValue = 0;
    private long transitionStartTime = 0L;

    public void setDigit(int newDigit) {
        if (newDigit != currentValue) {
            this.previousValue = this.currentValue;
            this.currentValue = newDigit;
            this.transitionStartTime = System.currentTimeMillis();
        }
    }

    public void reset(int digit) {
        this.currentValue = digit;
        this.previousValue = digit;
        this.transitionStartTime = 0L;
    }

    public void render(GuiGraphics g, Font font, int x, int y, int width, int color) {
        long elapsed = System.currentTimeMillis() - transitionStartTime;
        int lineHeight = font.lineHeight;

        if (transitionStartTime == 0L || elapsed >= DURATION_MS) {
            String str = String.valueOf(currentValue);
            int drawX = x + (width - font.width(str)) / 2;
            g.drawString(font, str, drawX, y, color, true);
            return;
        }

        float t = Math.min(1.0f, elapsed / (float) DURATION_MS);
        float eased = easeOutCubic(t);

        int baseRgb = color & 0x00FFFFFF;
        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }

        // 前の数字: 下方向へ押し出されながらフェードアウト
        int prevY = y + Math.round(lineHeight * eased);
        int prevAlpha = Math.round(baseAlpha * (1.0f - eased));
        if (prevAlpha > 10) {
            int prevColor = (prevAlpha << 24) | baseRgb;
            String prevStr = String.valueOf(previousValue);
            int prevX = x + (width - font.width(prevStr)) / 2;
            g.drawString(font, prevStr, prevX, prevY, prevColor, true);
        }

        // 新しい数字: 上方向からスライドして下りてきながらフェードイン
        int currY = y - Math.round(lineHeight * (1.0f - eased));
        int currAlpha = Math.round(baseAlpha * eased);
        if (currAlpha > 10) {
            int currColor = (currAlpha << 24) | baseRgb;
            String currStr = String.valueOf(currentValue);
            int currX = x + (width - font.width(currStr)) / 2;
            g.drawString(font, currStr, currX, currY, currColor, true);
        }
    }

    private static float easeOutCubic(float x) {
        float inv = 1.0f - x;
        return 1.0f - inv * inv * inv;
    }

    public int getCurrentValue() {
        return currentValue;
    }
}
