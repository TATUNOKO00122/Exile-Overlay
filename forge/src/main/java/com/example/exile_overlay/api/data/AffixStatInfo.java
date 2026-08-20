package com.example.exile_overlay.api.data;

/**
 * Mobアフィクスの個別スタット情報
 */
public class AffixStatInfo {
    public static final int DISPLAY_COLOR = 0xFFFFFFFF;

    public final float value;
    public final String statName;
    public final boolean isPercent;

    public AffixStatInfo(float value, String statName, boolean isPercent) {
        this.value = value;
        this.statName = statName != null ? statName : "";
        this.isPercent = isPercent;
    }

    public String getDisplayText() {
        String sign = value >= 0 ? "+" : "";
        String pct = isPercent ? "%" : "";
        String val = (value == (int) value)
                ? String.valueOf((int) value)
                : String.format("%.1f", value);
        return sign + val + pct + " " + statName;
    }
}
