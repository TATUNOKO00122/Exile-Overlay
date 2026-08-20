package com.example.exile_overlay.dmgtracker.tracking;

public class TimestampedDamage {
    public final long timestampMs;
    public final float damage;

    public static final int DPS_WINDOW_MS = 10_000;

    public TimestampedDamage(long timestampMs, float damage) {
        this.timestampMs = timestampMs;
        this.damage = damage;
    }

    public static float computeDps(Iterable<TimestampedDamage> hits) {
        long now = System.currentTimeMillis();
        long cutoff = now - DPS_WINDOW_MS;
        float total = 0;
        long oldest = Long.MAX_VALUE;
        for (TimestampedDamage td : hits) {
            if (td.timestampMs >= cutoff) {
                total += td.damage;
                if (td.timestampMs < oldest) oldest = td.timestampMs;
            }
        }
        if (total <= 0 || oldest == Long.MAX_VALUE) return 0;
        long spanMs = Math.max(1000, Math.min(DPS_WINDOW_MS, now - oldest));
        return total / (spanMs / 1000f);
    }
}
