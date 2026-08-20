package com.example.exile_overlay.dmgtracker.gui;

import net.minecraft.client.gui.Font;

public final class FormatUtil {
    private FormatUtil() {}

    public static String fmt(double v) {
        if (v >= 1_000_000) return String.format("%.1fM", v / 1_000_000.0);
        if (v >= 1_000) return String.format("%.1fk", v / 1_000.0);
        if (v >= 100) return String.format("%.0f", v);
        if (v >= 10) return String.format("%.1f", v);
        return String.format("%.2f", v);
    }

    public static String truncate(Font font, String text, int maxWidth) {
        if (font.width(text) <= maxWidth) return text;
        while (text.length() > 1 && font.width(text + "..") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "..";
    }
}
