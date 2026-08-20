package com.example.exile_overlay.client.render.ailment;

import com.example.exile_overlay.api.MethodHandlesUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側で状態異常（毒の有無、出血による失血蓄積量）を管理・追跡するマネージャー。
 * PoE2スタイルのHPバー描画（毒の深緑化、出血の暗赤色セグメント）をサポートする。
 */
public final class ClientAilmentTracker {

    private static final ClientAilmentTracker INSTANCE = new ClientAilmentTracker();

    public static ClientAilmentTracker getInstance() {
        return INSTANCE;
    }

    /**
     * エンティティごとの失血（Blood Loss）蓄積データ
     */
    public static final class BloodLossEntry {
        public float accumulatedDamage;
        public long lastUpdatedTime;

        public BloodLossEntry(float initialDamage, long time) {
            this.accumulatedDamage = initialDamage;
            this.lastUpdatedTime = time;
        }
    }

    private final Map<UUID, BloodLossEntry> bloodLossMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> poisonExpiryMap = new ConcurrentHashMap<>();

    // 出血セグメントの保持時間（ミリ秒）: 出血ダメージが途絶えてから自然減衰を開始するまでの時間
    private static final long BLOOD_LOSS_HOLD_MS = 3000L;
    // 毒状態のフォールバック保持時間（ミリ秒）
    private static final long POISON_HOLD_MS = 2500L;

    private ClientAilmentTracker() {}

    /**
     * 対象エンティティが毒（Poison）状態かどうかを判定する。
     * バニラの毒効果またはMine and Slashの毒Ailmentが存在する場合にtrueを返す。
     */
    public boolean isPoisoned(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        // 1. バニラ毒効果
        if (entity.hasEffect(MobEffects.POISON)) {
            return true;
        }

        // 2. M&S Ailment 連携
        if (MethodHandlesUtil.isEntityPoisoned(entity)) {
            return true;
        }

        // 3. MOBEffect / StatusEffects 同期リストの走査（クライアント同期済みエフェクト）
        try {
            var effects = MethodHandlesUtil.getMobStatusEffectsInfo(entity);
            for (var eff : effects) {
                if (eff != null && eff.id != null) {
                    String id = eff.id.toLowerCase(java.util.Locale.ROOT);
                    if (id.contains("poison") || id.contains("toxic") || id.contains("venom") || id.contains("chaos")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignore) {}

        // 4. クライアント側ダメージ追跡キャッシュ
        Long expiry = poisonExpiryMap.get(entity.getUUID());
        if (expiry != null) {
            if (System.currentTimeMillis() <= expiry) {
                return true;
            } else {
                poisonExpiryMap.remove(entity.getUUID());
            }
        }

        return false;
    }

    /**
     * 対象エンティティが出血（Bleed）状態かどうかを判定する。
     */
    public boolean isBleeding(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (MethodHandlesUtil.isEntityBleeding(entity)) {
            return true;
        }
        try {
            var effects = MethodHandlesUtil.getMobStatusEffectsInfo(entity);
            for (var eff : effects) {
                if (eff != null && eff.id != null) {
                    String id = eff.id.toLowerCase(java.util.Locale.ROOT);
                    if (id.contains("bleed") || id.contains("wounds") || id.contains("blood")) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignore) {}
        BloodLossEntry entry = bloodLossMap.get(entity.getUUID());
        return entry != null && entry.accumulatedDamage > 0.0f;
    }

    /**
     * 対象エンティティが出血によって削られた累積HP（Blood Loss）の量を取得する。
     * 将来受ける予測値ではなく、既に出血で削れた累積ダメージを表す。
     */
    public float getBloodLoss(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return 0.0f;
        }

        BloodLossEntry entry = bloodLossMap.get(entity.getUUID());
        if (entry == null || entry.accumulatedDamage <= 0.0f) {
            return 0.0f;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - entry.lastUpdatedTime;

        // 一定時間経過後は徐々に減衰
        if (elapsed > BLOOD_LOSS_HOLD_MS) {
            float decayFactor = 1.0f - Math.min(1.0f, (elapsed - BLOOD_LOSS_HOLD_MS) / 2000.0f);
            float decayedDamage = entry.accumulatedDamage * decayFactor;
            if (decayedDamage <= 0.5f) {
                bloodLossMap.remove(entity.getUUID());
                return 0.0f;
            }
            return decayedDamage;
        }

        return entry.accumulatedDamage;
    }

    /**
     * 出血DoTダメージの発生を記録し、失血量を蓄積する。
     */
    public void recordBleedDamage(LivingEntity target, float damage) {
        if (target == null || !target.isAlive() || damage <= 0.0f) {
            return;
        }

        long now = System.currentTimeMillis();
        bloodLossMap.compute(target.getUUID(), (k, existing) -> {
            if (existing == null) {
                return new BloodLossEntry(damage, now);
            } else {
                existing.accumulatedDamage += damage;
                existing.lastUpdatedTime = now;
                return existing;
            }
        });
    }

    /**
     * 毒DoTダメージの発生を記録する。
     */
    public void recordPoisonDamage(LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return;
        }
        poisonExpiryMap.put(target.getUUID(), System.currentTimeMillis() + POISON_HOLD_MS);
    }

    /**
     * 対象エンティティの失血蓄積を消費（起爆）またはリセットする。
     */
    public void clear(LivingEntity entity) {
        if (entity != null) {
            bloodLossMap.remove(entity.getUUID());
            poisonExpiryMap.remove(entity.getUUID());
        }
    }

    /**
     * ワールド退出時や定期的なクリーンアップ
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        bloodLossMap.entrySet().removeIf(e -> (now - e.getValue().lastUpdatedTime) > 10000L);
        poisonExpiryMap.entrySet().removeIf(e -> now > e.getValue());
    }
}
