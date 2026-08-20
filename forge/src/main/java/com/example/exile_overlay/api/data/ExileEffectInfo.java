package com.example.exile_overlay.api.data;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Set;

/**
 * プレイヤーのExileEffect情報
 */
public class ExileEffectInfo {
    public final String id;
    public final String name;
    public final ResourceLocation texture;
    public final int duration;
    public final int stacks;
    public final boolean isBeneficial;
    public final boolean isNegative;
    public final boolean isInfinite;
    public final String durationText;
    public final String spellId;
    public final boolean selfCast;
    public final String casterUuid;
    public final Set<String> tags;

    public ExileEffectInfo(String id, String name, ResourceLocation texture, int duration, int stacks,
                           boolean isBeneficial, boolean isNegative, boolean isInfinite, String durationText,
                           String spellId, boolean selfCast, String casterUuid, Set<String> tags) {
        this.id = id;
        this.name = name;
        this.texture = texture;
        this.duration = duration;
        this.stacks = stacks;
        this.isBeneficial = isBeneficial;
        this.isNegative = isNegative;
        this.isInfinite = isInfinite;
        this.durationText = durationText;
        this.spellId = spellId;
        this.selfCast = selfCast;
        this.casterUuid = casterUuid;
        this.tags = tags != null ? tags : Collections.emptySet();
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
