package com.example.exile_overlay.api.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Mobアフィクス情報
 */
public class MobAffixInfo {
    public final String name;
    public final String icon;
    public final List<AffixStatInfo> stats;
    public final boolean isPrefix;

    public MobAffixInfo(String name, String icon, List<AffixStatInfo> stats) {
        this(name, icon, stats, true);
    }

    public MobAffixInfo(String name, String icon, List<AffixStatInfo> stats, boolean isPrefix) {
        this.name = name;
        this.icon = icon != null ? icon : "";
        this.stats = stats != null ? stats : new ArrayList<>();
        this.isPrefix = isPrefix;
    }
}
