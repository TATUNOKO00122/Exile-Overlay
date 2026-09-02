package com.example.exile_overlay.client.render.ailment;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.data.MobEffectInfo;
import com.example.exile_overlay.dmgtracker.network.AilmentSyncS2C;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * クライアント側で状態異常（Ailments: 出血、毒、火傷、凍結、感電）を管理・追跡するマネージャー。
 * サーバーからの同期パケットを受信し、HUDエフェクト表示やHPバー装飾（失血バー、毒深緑バー）と連動する。
 */
public final class ClientAilmentTracker {

    private static final ClientAilmentTracker INSTANCE = new ClientAilmentTracker();

    public static ClientAilmentTracker getInstance() {
        return INSTANCE;
    }

    // Element アイコンの定義 (M&Sの属性アイコンを流用)
    private static final ResourceLocation ICON_PHYSICAL = new ResourceLocation("mmorpg", "textures/gui/stat_icons/element_icons/physical.png");
    private static final ResourceLocation ICON_CHAOS = new ResourceLocation("mmorpg", "textures/gui/stat_icons/element_icons/chaos.png");
    private static final ResourceLocation ICON_FIRE = new ResourceLocation("mmorpg", "textures/gui/stat_icons/element_icons/fire.png");
    private static final ResourceLocation ICON_WATER = new ResourceLocation("mmorpg", "textures/gui/stat_icons/element_icons/water.png");
    private static final ResourceLocation ICON_LIGHTNING = new ResourceLocation("mmorpg", "textures/gui/stat_icons/element_icons/lightning.png");

    public record SyncedAilment(
            String id,
            int ticksLeft,
            int stacks,
            float strength,
            float damage,
            long receivedTime
    ) {
        public int getCurrentTicksLeft() {
            long elapsedTicks = (System.currentTimeMillis() - receivedTime) / 50L;
            return Math.max(0, ticksLeft - (int) elapsedTicks);
        }
    }

    public static final class BloodLossEntry {
        public float accumulatedDamage;
        public long lastActiveMs;
        public float displayedEndRatio = -1.0f;
        public long lastRenderTime;

        public BloodLossEntry(float damage, long time) {
            this.accumulatedDamage = damage;
            this.lastActiveMs = time;
            this.lastRenderTime = time;
        }
    }

    private final Map<Integer, List<SyncedAilment>> entityAilmentsMap = new ConcurrentHashMap<>();
    private final Map<UUID, BloodLossEntry> bloodLossMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> poisonExpiryMap = new ConcurrentHashMap<>();

    private static final long BLOOD_LOSS_HOLD_MS = 2500L;
    private static final long BLOOD_LOSS_DECAY_MS = 1000L;
    private static final long POISON_HOLD_MS = 2500L;

    private ClientAilmentTracker() {}

    /**
     * サーバーからの Ailment 同期パケット受信時のハンドラ
     */
    public void handleSync(int entityId, List<AilmentSyncS2C.AilmentEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            entityAilmentsMap.remove(entityId);
            return;
        }

        long now = System.currentTimeMillis();
        List<SyncedAilment> list = new ArrayList<>(entries.size());
        for (AilmentSyncS2C.AilmentEntry entry : entries) {
            list.add(new SyncedAilment(
                    entry.id(),
                    entry.ticksLeft(),
                    entry.stacks(),
                    entry.strength(),
                    entry.damage(),
                    now
            ));
        }
        entityAilmentsMap.put(entityId, list);
    }

    /**
     * 対象エンティティに付与されている Ailment を MobEffectInfo のリストとして取得
     */
    public List<MobEffectInfo> getAilmentEffects(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return Collections.emptyList();
        }

        List<SyncedAilment> list = entityAilmentsMap.get(entity.getId());
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        List<MobEffectInfo> effects = new ArrayList<>(list.size());
        for (SyncedAilment a : list) {
            int currentTicks = a.getCurrentTicksLeft();
            if (currentTicks <= 0 && a.strength <= 0.0f) {
                continue;
            }

            ResourceLocation icon = resolveElementIcon(a.id);
            String name = Component.translatable("mmorpg.ailment." + a.id).getString();
            effects.add(new MobEffectInfo(
                    "ailment:" + a.id,
                    name,
                    icon,
                    currentTicks,
                    a.stacks,
                    false,
                    true
            ));
        }
        return effects;
    }

    public static ResourceLocation resolveElementIcon(String ailmentId) {
        if (ailmentId == null) return ICON_PHYSICAL;
        return switch (ailmentId.toLowerCase(java.util.Locale.ROOT)) {
            case "bleed" -> ICON_PHYSICAL;
            case "poison" -> ICON_CHAOS;
            case "burn" -> ICON_FIRE;
            case "freeze" -> ICON_WATER;
            case "electrify" -> ICON_LIGHTNING;
            default -> ICON_PHYSICAL;
        };
    }

    /**
     * 対象エンティティが毒（Poison）状態かどうかを判定する。
     */
    public boolean isPoisoned(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        // 1. 同期パケットデータ
        List<SyncedAilment> list = entityAilmentsMap.get(entity.getId());
        if (list != null) {
            for (SyncedAilment a : list) {
                if ("poison".equalsIgnoreCase(a.id) && a.getCurrentTicksLeft() > 0) {
                    return true;
                }
            }
        }

        // 2. バニラ毒効果
        if (entity.hasEffect(MobEffects.POISON)) {
            return true;
        }

        // 3. M&S Ailment 連携 (シングルプレイ直接参照)
        if (MethodHandlesUtil.isEntityPoisoned(entity)) {
            return true;
        }

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

        // 1. 同期パケットデータ（持続時間の残存を厳格に確認）
        List<SyncedAilment> list = entityAilmentsMap.get(entity.getId());
        if (list != null) {
            for (SyncedAilment a : list) {
                if ("bleed".equalsIgnoreCase(a.id) && a.getCurrentTicksLeft() > 0) {
                    return true;
                }
            }
        }

        // 2. M&S Ailment 連携 (シングルプレイ直接参照)
        return MethodHandlesUtil.isEntityBleeding(entity);
    }

    /**
     * 減衰を考慮した実効失血ダメージ量を計算
     */
    private float getEffectiveBloodLoss(BloodLossEntry entry, long now, boolean isBleeding) {
        if (entry == null || entry.accumulatedDamage <= 0.0f) {
            return 0.0f;
        }

        if (isBleeding) {
            return entry.accumulatedDamage;
        }

        long elapsed = now - entry.lastActiveMs;
        if (elapsed <= BLOOD_LOSS_HOLD_MS) {
            return entry.accumulatedDamage;
        }

        float decayProgress = (elapsed - BLOOD_LOSS_HOLD_MS) / (float) BLOOD_LOSS_DECAY_MS;
        if (decayProgress >= 1.0f) {
            return 0.0f;
        }

        return entry.accumulatedDamage * (1.0f - decayProgress);
    }

    /**
     * PoE2仕様: HPバー上の失血バー（暗赤色）の終了位置比率 (0.0F ~ 1.0F) を返す。
     * 出血DoTによって削られた実ダメージのみを失血バーとして蓄積・表示し、通常攻撃ダメージと明確に分離する。
     * また、DoT被弾時のHP更新パケット遅延による右端の突発的な飛び出し（脈動）を完全に防止する。
     */
    public float getBloodLossEndRatio(LivingEntity entity, float currentHpRatio, float maxHp) {
        if (entity == null || !entity.isAlive() || maxHp <= 0.0f) {
            return currentHpRatio;
        }

        BloodLossEntry entry = bloodLossMap.get(entity.getUUID());
        if (entry == null) {
            return currentHpRatio;
        }

        long now = System.currentTimeMillis();
        boolean bleeding = isBleeding(entity);
        float bloodLoss = getEffectiveBloodLoss(entry, now, bleeding);
        if (bloodLoss <= 0.0f) {
            bloodLossMap.remove(entity.getUUID());
            return currentHpRatio;
        }

        // 失血量は、失われたHP総量 (maxHp - currentHp) を上限とする
        float currentHp = currentHpRatio * maxHp;
        float maxLostHp = Math.max(0.0f, maxHp - currentHp);
        float cappedBloodLoss = Math.min(bloodLoss, maxLostHp);
        float bloodLossRatio = cappedBloodLoss / maxHp;

        float calculatedEnd = Math.min(1.0f, currentHpRatio + bloodLossRatio);

        if (entry.displayedEndRatio < 0.0f) {
            // 付与された瞬間の現在HPを右端の初期位置とする（右側への突発的な跳ね上がりを防止）
            entry.displayedEndRatio = currentHpRatio;
            entry.lastRenderTime = now;
            return currentHpRatio;
        }

        long dt = Math.min(100L, Math.max(1L, now - entry.lastRenderTime));
        entry.lastRenderTime = now;

        // 目標右端の決定:
        // 出血DoT被弾時、HP減少パケットの到着ラグによって右端が直前の右端より右へ飛び出す（脈動）のを防止
        float targetEndRatio;
        if (bleeding) {
            if (currentHpRatio > entry.displayedEndRatio) {
                // 回復等で現在HPが右端を押し上げた場合
                targetEndRatio = calculatedEnd;
            } else {
                // DoT被弾ラグによる右飛び出しを抑制: 直前の右端を上限とする
                targetEndRatio = Math.min(calculatedEnd, entry.displayedEndRatio);
                targetEndRatio = Math.max(currentHpRatio, targetEndRatio);
            }
        } else {
            targetEndRatio = calculatedEnd;
        }

        // スムージング追従
        float speed = 8.0f;
        float factor = (float) (1.0 - Math.exp(-speed * (dt / 1000.0)));
        entry.displayedEndRatio = entry.displayedEndRatio + (targetEndRatio - entry.displayedEndRatio) * factor;

        if (Math.abs(entry.displayedEndRatio - targetEndRatio) < 0.001f) {
            entry.displayedEndRatio = targetEndRatio;
        }

        entry.displayedEndRatio = Math.max(currentHpRatio, Math.min(1.0f, entry.displayedEndRatio));
        return entry.displayedEndRatio;
    }

    /**
     * 出血DoTダメージの発生を記録し、失血量（Blood Loss）を蓄積する。
     * 通常攻撃ダメージは加算されず、出血DoTダメージのみが蓄積される。
     */
    public void recordBleedDamage(LivingEntity target, float damage) {
        if (target == null || !target.isAlive() || damage <= 0.0f) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean bleeding = isBleeding(target);

        float maxHp = target.getMaxHealth();
        float currentHpRatio = (maxHp > 0.0f) ? Math.min(1.0f, target.getHealth() / maxHp) : 1.0f;

        bloodLossMap.compute(target.getUUID(), (k, existing) -> {
            if (existing == null) {
                BloodLossEntry entry = new BloodLossEntry(damage, now);
                entry.displayedEndRatio = currentHpRatio;
                return entry;
            } else {
                float baseDamage = getEffectiveBloodLoss(existing, now, bleeding);
                existing.accumulatedDamage = baseDamage + damage;
                existing.lastActiveMs = now;
                if (existing.displayedEndRatio < 0.0f) {
                    existing.displayedEndRatio = currentHpRatio;
                }
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
     * 対象エンティティのキャッシュをクリアする。
     */
    public void clear(LivingEntity entity) {
        if (entity != null) {
            entityAilmentsMap.remove(entity.getId());
            bloodLossMap.remove(entity.getUUID());
            poisonExpiryMap.remove(entity.getUUID());
        }
    }

    /**
     * 定期的なクリーンアップ
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        long bloodLossExpiry = BLOOD_LOSS_HOLD_MS + BLOOD_LOSS_DECAY_MS + 1000L;
        bloodLossMap.entrySet().removeIf(e -> (now - e.getValue().lastActiveMs) > bloodLossExpiry);
        poisonExpiryMap.entrySet().removeIf(e -> now > e.getValue());
        entityAilmentsMap.entrySet().removeIf(e -> {
            for (SyncedAilment a : e.getValue()) {
                if (a.getCurrentTicksLeft() > 0 || a.strength > 0.0f) return false;
            }
            return true;
        });
    }
}
