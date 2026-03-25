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
 * HJUD MOD由来: ダメージ追跡・DPS計算クラス
 */
public class DamageTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageTracker");
    private static DamageTracker instance;
    
    private static final long DAMAGE_EXPIRY_TIME = 60000; // 60秒でデータ削除
    private static final long DPS_CALCULATION_WINDOW = 5000; // 5秒間のDPS計算

    private final Map<Integer, EntityDamageData> entityDamageMap = new ConcurrentHashMap<>();

    private DamageTracker() {
    }

    public static DamageTracker getInstance() {
        if (instance == null) {
            instance = new DamageTracker();
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

                // 古いエントリを削除
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

        // ビジュアル表示
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof LivingEntity) {
                displayDamage((LivingEntity) entity, totalDamage, damageByElement, isCritical);
            }
        }
    }

    /**
     * 単純ダメージ記録
     */
    public void addDamage(LivingEntity entity, float damage, DamageType type, boolean isCrit) {
        Map<String, Float> damageByElement = new HashMap<>();
        damageByElement.put(type.toString(), damage);
        recordDetailedDamage(entity.getId(), damageByElement, isCrit);
    }

    /**
     * ダメージ表示
     */
    private void displayDamage(LivingEntity entity, float totalDamage, Map<String, Float> damageByElement,
            boolean isCrit) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        // プレイヤーへのダメージは表示しない
        if (entity == mc.player) {
            return;
        }

        // エンティティの頭上に表示
        Vec3 position = entity.position().add(0, entity.getBbHeight() * 1.2, 0);

        // 最も高いダメージの属性の色を使用
        String dominantElement = damageByElement.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NORMAL");

        int color = getColorForElement(dominantElement);
        
        // Enumに存在しない要素名の場合はNORMALを使用
        DamageType damageType;
        try {
            damageType = DamageType.valueOf(dominantElement);
        } catch (IllegalArgumentException e) {
            damageType = DamageType.NORMAL;
        }
        
        DamagePopupManager.getInstance().addDamageNumber(
            position, totalDamage, color, isCrit, 
            damageType, entity.getId()
        );
    }

    /**
     * ヒール表示
     */
    public void addHeal(LivingEntity entity, float healAmount) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null) {
            return;
        }

        Vec3 position = entity.position().add(0, entity.getBbHeight() * 1.2, 0);
        
        DamagePopupManager.getInstance().addDamageNumber(
            position, healAmount, 0x00FF00, false, 
            DamageType.HEALING, entity.getId()
        );
    }

    /**
     * 属性ごとの色を取得
     */
    private int getColorForElement(String element) {
        return switch (element) {
            case "PHYSICAL", "NORMAL" -> 0xFFFFFF; // 白
            case "FIRE" -> 0xFF4500; // 赤
            case "COLD", "ICE" -> 0x0080FF; // 青
            case "NATURE", "POISON" -> 0x00FF00; // 緑
            case "SHADOW", "DARK" -> 0x800080; // 紫
            case "LIGHTNING", "THUNDER" -> 0xFFFF00; // 黄
            case "MAGIC" -> 0x800080; // 紫
            case "ALL" -> 0xFFFFFF; // 白
            default -> 0xFFFFFF; // デフォルト白
        };
    }

    /**
     * DPSデータ取得
     */
    public EntityDamageData getDamageData(int entityId) {
        return entityDamageMap.get(entityId);
    }

    public EntityDamageData getDamageData(Entity entity) {
        return entity != null ? getDamageData(entity.getId()) : null;
    }

    /**
     * 全エンティティの合計DPSを計算
     */
    public float getTotalDPS() {
        float total = 0;
        for (EntityDamageData data : entityDamageMap.values()) {
            total += data.calculateDPS();
        }
        return total;
    }

    /**
     * 戦闘中のエンティティ数を取得
     */
    public int getCombatEntityCount() {
        int count = 0;
        for (EntityDamageData data : entityDamageMap.values()) {
            if (data.isInCombat()) {
                count++;
            }
        }
        return count;
    }

    /**
     * データクリーンアップ
     */
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
