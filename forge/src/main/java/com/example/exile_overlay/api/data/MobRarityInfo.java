package com.example.exile_overlay.api.data;

/**
 * MobのM&Sレアリティ情報
 */
public class MobRarityInfo {
    public final String id;
    public final int color;
    public final boolean isElite;
    public final boolean isSpecial;

    public MobRarityInfo(String id, int color, boolean isElite, boolean isSpecial) {
        this.id = id;
        this.color = color;
        this.isElite = isElite;
        this.isSpecial = isSpecial;
    }
}
