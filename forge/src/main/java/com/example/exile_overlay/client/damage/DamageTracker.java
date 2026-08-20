package com.example.exile_overlay.client.damage;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ダメージ追跡・DPS計算クラス
 */
public class DamageTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageTracker");
    private static volatile DamageTracker instance;

    private static final long DAMAGE_EXPIRY_TIME = 60000;
    private static final long DPS_CALCULATION_WINDOW = 5000;

    private final Map<Integer, EntityDamageData> entityDamageMap = new ConcurrentHashMap<>();

    private DamageTracker() {
    }

    public static DamageTracker getInstance() {
        if (instance == null) {
            synchronized (DamageTracker.class) {
                if (instance == null) {
                    instance = new DamageTracker();
                }
            }
        }
        return instance;
    }

    public static class DamageEntry {
        public final float totalDamage;
        public final Map<String, Float> damageByElement;
        public final long timestamp;
        public final boolean isCritical;

        public DamageEntry(float totalDamage, Map<String, Float> damageByElement, boolean isCritical) {
            this.totalDamage = totalDamage;
            this.damageByElement = new HashMap<>(damageByElement);
            this.timestamp = System.currentTimeMillis();
            this.isCritical = isCritical;
        }
    }

    public static class EntityDamageData {
        private final LinkedList<DamageEntry> damageHistory = new LinkedList<>();
        private float totalDamageDealt = 0;
        private long lastDamageTime = 0;

        public void addDamage(DamageEntry entry) {
            synchronized (damageHistory) {
                damageHistory.add(entry);
                totalDamageDealt += entry.totalDamage;
                lastDamageTime = entry.timestamp;

                long cutoffTime = System.currentTimeMillis() - DAMAGE_EXPIRY_TIME;
                damageHistory.removeIf(e -> e.timestamp < cutoffTime);
            }
        }

        public float calculateDPS() {
            synchronized (damageHistory) {
                long currentTime = System.currentTimeMillis();
                long windowStart = currentTime - DPS_CALCULATION_WINDOW;

                float damageInWindow = 0;
                for (DamageEntry entry : damageHistory) {
                    if (entry.timestamp >= windowStart) {
                        damageInWindow += entry.totalDamage;
                    }
                }

                return damageInWindow / (DPS_CALCULATION_WINDOW / 1000.0f);
            }
        }

        public List<DamageEntry> getRecentDamage(int count) {
            synchronized (damageHistory) {
                List<DamageEntry> recent = new ArrayList<>();
                int start = Math.max(0, damageHistory.size() - count);
                for (int i = start; i < damageHistory.size(); i++) {
                    recent.add(damageHistory.get(i));
                }
                return recent;
            }
        }

        public float getTotalDamage() {
            return totalDamageDealt;
        }

        public long getLastDamageTime() {
            return lastDamageTime;
        }

        public boolean isInCombat() {
            return System.currentTimeMillis() - lastDamageTime < 5000;
        }
    }

    /**
     * 詳細ダメージ記録（Mine and Slash対応）
     */
    public void recordDetailedDamage(int entityId, Map<String, Float> damageByElement, boolean isCritical) {
        float totalDamage = damageByElement.values().stream().reduce(0f, Float::sum);

        EntityDamageData data = entityDamageMap.computeIfAbsent(entityId, k -> new EntityDamageData());
        data.addDamage(new DamageEntry(totalDamage, damageByElement, isCritical));

        LOGGER.debug("Detailed damage recorded for entity {}: {} total damage (crit: {})",
                entityId, totalDamage, isCritical);

        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof LivingEntity) {
                displayDamage((LivingEntity) entity, totalDamage, damageByElement, isCritical);
            }
        }
    }

    public void addDamage(LivingEntity entity, float damage, DamageType type, boolean isCrit) {
        Map<String, Float> damageByElement = new HashMap<>();
        damageByElement.put(type.toString(), damage);
        recordDetailedDamage(entity.getId(), damageByElement, isCrit);
    }

    private void displayDamage(LivingEntity entity, float totalDamage, Map<String, Float> damageByElement,
            boolean isCrit) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        if (entity == mc.player) {
            return;
        }

        DamagePopupConfig config = DamagePopupConfig.getInstance();
        Vec3 position = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);

        String dominantElement = damageByElement.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NORMAL");

        DamageType damageType = mapElementToDamageType(dominantElement);

        DamagePopupManager.getInstance().addDamageNumber(
            position, totalDamage, isCrit,
            damageType, entity.getId(), Vec3.ZERO
        );
    }

    public void addHeal(LivingEntity entity, float healAmount) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        DamagePopupConfig config = DamagePopupConfig.getInstance();
        Vec3 position = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);

        DamagePopupManager.getInstance().addDamageNumber(
            position, healAmount, false,
            DamageType.HEALING, entity.getId(), Vec3.ZERO
        );
    }

    private DamageType mapElementToDamageType(String element) {
        return switch (element) {
            case "PHYSICAL" -> DamageType.PHYSICAL;
            case "FIRE" -> DamageType.FIRE;
            case "COLD", "ICE" -> DamageType.ICE;
            case "NATURE", "POISON" -> DamageType.NATURE;
            case "SHADOW", "DARK" -> DamageType.MAGIC;
            case "LIGHTNING", "THUNDER" -> DamageType.LIGHTNING;
            case "MAGIC" -> DamageType.MAGIC;
            default -> DamageType.NORMAL;
        };
    }

    public EntityDamageData getDamageData(int entityId) {
        return entityDamageMap.get(entityId);
    }

    public EntityDamageData getDamageData(Entity entity) {
        return entity != null ? getDamageData(entity.getId()) : null;
    }

    public float getTotalDPS() {
        float total = 0;
        for (EntityDamageData data : entityDamageMap.values()) {
            total += data.calculateDPS();
        }
        return total;
    }

    public int getCombatEntityCount() {
        int count = 0;
        for (EntityDamageData data : entityDamageMap.values()) {
            if (data.isInCombat()) {
                count++;
            }
        }
        return count;
    }

    public void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - DAMAGE_EXPIRY_TIME;
        entityDamageMap.entrySet().removeIf(entry -> {
            EntityDamageData data = entry.getValue();
            return data.getLastDamageTime() < cutoffTime;
        });
    }

    public void clear() {
        entityDamageMap.clear();
    }
}
