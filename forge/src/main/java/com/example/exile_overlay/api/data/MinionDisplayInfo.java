package com.example.exile_overlay.api.data;

import net.minecraft.resources.ResourceLocation;

/**
 * 召喚ミニオングループの表示用データ
 */
public record MinionDisplayInfo(
        String spellId,
        String name,
        ResourceLocation icon,
        int count,
        int durationTicks,
        int maxDurationTicks,
        boolean isInfinite,
        String durationText,
        float healthRatio
) {
    public static MinionDisplayInfo of(String spellId, String name, ResourceLocation icon,
                                      int count, int durationTicks, int maxDurationTicks, boolean isInfinite,
                                      float healthRatio) {
        String durText = isInfinite ? "" : formatDuration(Math.max(0, durationTicks / 20));
        return new MinionDisplayInfo(spellId, name, icon, count, durationTicks, maxDurationTicks, isInfinite, durText, healthRatio);
    }

    public static MinionDisplayInfo of(String spellId, String name, ResourceLocation icon,
                                      int count, int durationTicks, int maxDurationTicks, boolean isInfinite) {
        return of(spellId, name, icon, count, durationTicks, maxDurationTicks, isInfinite, 1.0f);
    }

    private static String formatDuration(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }
}
