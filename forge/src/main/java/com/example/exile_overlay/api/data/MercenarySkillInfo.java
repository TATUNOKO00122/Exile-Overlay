package com.example.exile_overlay.api.data;

import net.minecraft.resources.ResourceLocation;

/**
 * 傭兵の装備スキル情報
 */
public record MercenarySkillInfo(
        String spellId,
        ResourceLocation icon,
        boolean onCooldown,
        float cooldownProgress,
        int remainingTicks,
        int totalTicks
) {
}
