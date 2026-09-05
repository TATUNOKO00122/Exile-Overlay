package com.example.exile_overlay.api.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 召喚中の傭兵（Mercenary）の表示用データ
 */
public record MercenaryDisplayInfo(
        String classId,
        String name,
        ResourceLocation icon,
        int level,
        float health,
        float maxHealth,
        float magicShield,
        float maxMagicShield,
        List<MercenarySkillInfo> skills
) {
    public boolean isAlive() {
        return health > 0;
    }
}

