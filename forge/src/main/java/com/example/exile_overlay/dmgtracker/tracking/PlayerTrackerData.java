package com.example.exile_overlay.dmgtracker.tracking;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerTrackerData {
    // ConcurrentHashMap: ダメージイベント（サーバースレッド）とSync送信（Tickスレッド）の並行アクセスに対応
    private final Map<String, SkillDamageStats> skillStats = new ConcurrentHashMap<>();
    private long sessionStartMs;

    private static final int COMBAT_TIMEOUT_MS = 10_000;
    private static final int IDLE_FREEZE_MS = 1_500;

    private volatile long lastDamageTimeMs = 0;

    public PlayerTrackerData() {
        this.sessionStartMs = System.currentTimeMillis();
    }

    public SkillDamageStats getStats(String skillId) {
        return skillStats.get(skillId);
    }

    public SkillDamageStats getOrCreateStats(String skillId, String displayName) {
        return skillStats.computeIfAbsent(skillId, id -> new SkillDamageStats(id, displayName));
    }

    public Collection<SkillDamageStats> getAllStats() {
        return skillStats.values();
    }

    public List<SkillDamageStats> getTopSkillsByDamage(int count) {
        List<SkillDamageStats> active = new ArrayList<>();
        for (SkillDamageStats s : skillStats.values()) {
            if (s.getTotalDamage() > 0 || s.getHitCount() > 0) {
                active.add(s);
            }
        }

        // 各要素の総ダメージ量を同一固定値としてマップし安定ソート
        Map<SkillDamageStats, Double> dmgSnapshots = new HashMap<>();
        for (SkillDamageStats s : active) {
            dmgSnapshots.put(s, s.getTotalDamage());
        }
        active.sort((a, b) -> Double.compare(dmgSnapshots.getOrDefault(b, 0.0), dmgSnapshots.getOrDefault(a, 0.0)));
        return active.size() <= count ? active : active.subList(0, count);
    }

    public double getTotalDamage() {
        double total = 0;
        for (SkillDamageStats s : skillStats.values()) {
            total += s.getTotalDamage();
        }
        return total;
    }

    public int getTotalHits() {
        int total = 0;
        for (SkillDamageStats s : skillStats.values()) {
            total += s.getHitCount();
        }
        return total;
    }

    public int getTotalKills() {
        int total = 0;
        for (SkillDamageStats s : skillStats.values()) {
            total += s.getKillCount();
        }
        return total;
    }

    public void markDamageEvent() {
        lastDamageTimeMs = System.currentTimeMillis();
    }

    public boolean isInCombat() {
        return lastDamageTimeMs > 0 && (System.currentTimeMillis() - lastDamageTimeMs) <= COMBAT_TIMEOUT_MS;
    }

    private long effectiveNow() {
        long realNow = System.currentTimeMillis();
        if (IDLE_FREEZE_MS <= 0 || lastDamageTimeMs == 0) return realNow;
        long idle = realNow - lastDamageTimeMs;
        if (idle > IDLE_FREEZE_MS) return lastDamageTimeMs + IDLE_FREEZE_MS;
        return realNow;
    }

    public float getOverallDps() {
        long now = effectiveNow();
        long cutoff = now - TimestampedDamage.DPS_WINDOW_MS;

        for (SkillDamageStats s : skillStats.values()) {
            s.trimRecentHits(now);
        }

        float total = 0;
        long oldest = Long.MAX_VALUE;
        for (SkillDamageStats s : skillStats.values()) {
            // スナップショットを取得してからイテレーション（並行変更を回避）
            List<TimestampedDamage> snapshot = s.getRecentHitsSnapshot();
            for (TimestampedDamage td : snapshot) {
                if (td.timestampMs >= cutoff) {
                    total += td.damage;
                    if (td.timestampMs < oldest) oldest = td.timestampMs;
                }
            }
        }

        if (total <= 0 || oldest == Long.MAX_VALUE) return 0;
        long spanMs = Math.max(1000, Math.min(TimestampedDamage.DPS_WINDOW_MS, now - oldest));
        return total / (spanMs / 1000f);
    }

    public long getSessionDurationSeconds() {
        return (System.currentTimeMillis() - sessionStartMs) / 1000;
    }

    public void reset() {
        skillStats.clear();
        sessionStartMs = System.currentTimeMillis();
        lastDamageTimeMs = 0;
    }
}
