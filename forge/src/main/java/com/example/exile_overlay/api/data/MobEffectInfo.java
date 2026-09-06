package com.example.exile_overlay.api.data;

import com.example.exile_overlay.util.DurationFormatHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * MobのExileEffect情報
 */
public class MobEffectInfo {
    public final String id;
    public final String name;
    public final ResourceLocation texture;
    public final int stacks;
    public final boolean isInfinite;
    public final boolean isNegative;
    private final int displayTicksLeft;

    public MobEffectInfo(String id, String name, ResourceLocation texture, int ticksLeft,
                         int stacks, boolean isInfinite, boolean isNegative) {
        this.id = id;
        this.name = name;
        this.texture = texture;
        this.stacks = stacks;
        this.isInfinite = isInfinite;
        this.isNegative = isNegative;
        this.displayTicksLeft = ticksLeft;
    }

    public String getDurationText() {
        if (isInfinite) return "";
        return DurationFormatHelper.formatTicks(displayTicksLeft);
    }

    public boolean isExpired() {
        return !isInfinite && displayTicksLeft <= 0;
    }
}
