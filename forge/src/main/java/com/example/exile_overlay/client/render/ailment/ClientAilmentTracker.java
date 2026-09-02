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
        public long lastUpdatedTime;

        public BloodLossEntry(float initialDamage, long time) {
            this.accumulatedDamage = initialDamage;
            this.lastUpdatedTime = time;
        }
    }

    // entityId -> Ailment一覧
    private final Map<Integer, List<SyncedAilment>> entityAilmentsMap = new ConcurrentHashMap<>();
    private final Map<UUID, BloodLossEntry> bloodLossMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> poisonExpiryMap = new ConcurrentHashMap<>();

    private static final long BLOOD_LOSS_HOLD_MS = 3000L;
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

        // 1. 同期パケットデータ
        List<SyncedAilment> list = entityAilmentsMap.get(entity.getId());
        if (list != null) {
            for (SyncedAilment a : list) {
                if ("bleed".equalsIgnoreCase(a.id) && a.getCurrentTicksLeft() > 0) {
                    return true;
                }
            }
        }

        // 2. M&S Ailment 連携 (シングルプレイ直接参照)
        if (MethodHandlesUtil.isEntityBleeding(entity)) {
            return true;
        }

        BloodLossEntry entry = bloodLossMap.get(entity.getUUID());
        return entry != null && entry.accumulatedDamage > 0.0f;
    }

    /**
     * 対象エンティティが出血によって削られた累積HP（Blood Loss）の量を取得する。
     */
    public float getBloodLoss(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return 0.0f;
        }

        // 同期パケットに出血ダメージがあれば優先
        List<SyncedAilment> list = entityAilmentsMap.get(entity.getId());
        if (list != null) {
            for (SyncedAilment a : list) {
                if ("bleed".equalsIgnoreCase(a.id) && a.damage > 0.0f) {
                    return a.damage;
                }
            }
        }

        BloodLossEntry entry = bloodLossMap.get(entity.getUUID());
        if (entry == null || entry.accumulatedDamage <= 0.0f) {
            return 0.0f;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - entry.lastUpdatedTime;

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
        bloodLossMap.entrySet().removeIf(e -> (now - e.getValue().lastUpdatedTime) > 10000L);
        poisonExpiryMap.entrySet().removeIf(e -> now > e.getValue());
        entityAilmentsMap.entrySet().removeIf(e -> {
            for (SyncedAilment a : e.getValue()) {
                if (a.getCurrentTicksLeft() > 0 || a.strength > 0.0f) return false;
            }
            return true;
        });
    }
}
