package com.example.exile_overlay.api.data;

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
        int seconds = Math.max(0, displayTicksLeft / 20);
        if (seconds <= 0) return "0s";
        if (seconds >= 60) return (seconds / 60) + "m";
        return seconds + "s";
    }

    public boolean isExpired() {
        return !isInfinite && displayTicksLeft <= 0;
    }
}
