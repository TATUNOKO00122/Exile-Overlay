package com.example.exile_overlay.dmgtracker.tracking;

import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SkillDamageStats {
    private final String skillId;
    private String displayName;

    private double totalDamage;
    private int hitCount;
    private int critCount;
    private int missCount;

    private float maxHit;
    private float minHit = Float.MAX_VALUE;
    private float lastHit;

    private int killCount;

    private final Map<Elements, Double> damageByElement = new EnumMap<>(Elements.class);
    // ConcurrentLinkedDeque: recordHit（サーバースレッド）とgetOverallDps（Tickスレッド）の並行アクセスに対応
    final ConcurrentLinkedDeque<TimestampedDamage> recentHits = new ConcurrentLinkedDeque<>();

    private static final int MAX_RECENT_HITS = 10000;

    public SkillDamageStats(String skillId, String displayName) {
        this.skillId = skillId;
        this.displayName = displayName;
    }

    public synchronized void recordHit(float damage, boolean isCrit, Elements element) {
        if (damage <= 0) return;

        totalDamage += damage;
        hitCount++;
        if (isCrit) critCount++;
        if (damage > maxHit) maxHit = damage;
        if (damage < minHit) minHit = damage;
        lastHit = damage;

        if (element != null) {
            damageByElement.merge(element, (double) damage, Double::sum);
        }

        long now = System.currentTimeMillis();
        recentHits.addLast(new TimestampedDamage(now, damage));
        trimRecentHits();
    }

    public synchronized void recordMultiElementHit(Map<Elements, Float> damageMap, boolean isCrit) {
        if (damageMap == null || damageMap.isEmpty()) return;

        float totalHitDmg = 0f;
        for (Map.Entry<Elements, Float> entry : damageMap.entrySet()) {
            Elements element = entry.getKey();
            float dmg = entry.getValue() != null ? entry.getValue() : 0f;
            if (element != null && dmg > 0) {
                totalHitDmg += dmg;
                damageByElement.merge(element, (double) dmg, Double::sum);
            }
        }

        if (totalHitDmg <= 0) return;

        totalDamage += totalHitDmg;
        hitCount++;
        if (isCrit) critCount++;
        if (totalHitDmg > maxHit) maxHit = totalHitDmg;
        if (totalHitDmg < minHit) minHit = totalHitDmg;
        lastHit = totalHitDmg;

        long now = System.currentTimeMillis();
        recentHits.addLast(new TimestampedDamage(now, totalHitDmg));
        trimRecentHits();
    }

    public synchronized void recordMiss() {
        missCount++;
    }

    public synchronized void recordKill() {
        killCount++;
    }

    public synchronized float getAverageHit() {
        return hitCount > 0 ? (float) (totalDamage / hitCount) : 0;
    }

    public synchronized float getCritRate() {
        return hitCount > 0 ? (float) critCount / hitCount : 0;
    }

    void trimRecentHits() {
        trimRecentHits(System.currentTimeMillis());
    }

    void trimRecentHits(long now) {
        long cutoff = now - TimestampedDamage.DPS_WINDOW_MS;
        // ConcurrentLinkedDequeはpollFirst()でスレッドセーフに先頭から削除
        while (!recentHits.isEmpty() && recentHits.peekFirst() != null
                && recentHits.peekFirst().timestampMs < cutoff) {
            recentHits.pollFirst();
        }
        // size()はO(N)の計算コストがかかるため、頻繁な呼び出し（毎ヒット）は負荷の原因となる。
        // 時間経過で古い要素は削除されるため、上限チェックは不要。
        /*
        while (recentHits.size() > MAX_RECENT_HITS) {
            recentHits.pollFirst();
        }
        */
    }

    /**
     * イテレーション用のスナップショットを返す。
     * 並行変更によるConcurrentModificationExceptionを防止する。
     */
    public List<TimestampedDamage> getRecentHitsSnapshot() {
        return new ArrayList<>(recentHits);
    }

    public float getDps() {
        trimRecentHits();
        return TimestampedDamage.computeDps(recentHits);
    }

    public synchronized String getDominantElement() {
        String dominant = "Physical";
        double maxDmg = 0;
        for (Map.Entry<Elements, Double> e : damageByElement.entrySet()) {
            if (e.getValue() > maxDmg) {
                maxDmg = e.getValue();
                dominant = e.getKey().name();
            }
        }
        return dominant;
    }

    public synchronized float getMinHit() {
        return minHit == Float.MAX_VALUE ? 0 : minHit;
    }

    public synchronized void reset() {
        totalDamage = 0;
        hitCount = 0;
        critCount = 0;
        missCount = 0;
        maxHit = 0;
        minHit = Float.MAX_VALUE;
        lastHit = 0;
        killCount = 0;
        damageByElement.clear();
        recentHits.clear();
    }

    public String getSkillId() { return skillId; }
    public String getDisplayName() { return displayName; }
    public synchronized double getTotalDamage() { return totalDamage; }
    public synchronized int getHitCount() { return hitCount; }
    public synchronized int getCritCount() { return critCount; }
    public synchronized int getMissCount() { return missCount; }
    public synchronized float getMaxHit() { return maxHit; }
    public synchronized float getLastHit() { return lastHit; }
    public synchronized int getKillCount() { return killCount; }
    public synchronized Map<Elements, Double> getDamageByElement() {
        return new EnumMap<>(damageByElement);
    }
}
