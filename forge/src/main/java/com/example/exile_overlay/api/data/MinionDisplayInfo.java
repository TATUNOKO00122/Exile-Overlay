package com.example.exile_overlay.api.data;

import com.example.exile_overlay.util.DurationFormatHelper;
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
        String durText = isInfinite ? "" : DurationFormatHelper.formatTicks(durationTicks);
        return new MinionDisplayInfo(spellId, name, icon, count, durationTicks, maxDurationTicks, isInfinite, durText, healthRatio);
    }

    public static MinionDisplayInfo of(String spellId, String name, ResourceLocation icon,
                                      int count, int durationTicks, int maxDurationTicks, boolean isInfinite) {
        return of(spellId, name, icon, count, durationTicks, maxDurationTicks, isInfinite, 1.0f);
    }
}
