package com.example.exile_overlay.client.render.entity;

import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;

public final class EntityHealthBarTimer {

    private static final EntityHealthBarTimer INSTANCE = new EntityHealthBarTimer();

    private final Map<LivingEntity, Long> damageTimes = new WeakHashMap<>();
    private final Map<LivingEntity, Float> lastHealthMap = new WeakHashMap<>();

    private EntityHealthBarTimer() {}

    public static EntityHealthBarTimer getInstance() {
        return INSTANCE;
    }

    public void onDamage(LivingEntity entity) {
        damageTimes.put(entity, System.currentTimeMillis());
    }

    public long getLastDamageTime(LivingEntity entity) {
        return damageTimes.getOrDefault(entity, 0L);
    }

    public boolean shouldShow(LivingEntity living, int displayDurationSeconds) {
        float currentHealth = living.getHealth();
        float maxHealth = living.getMaxHealth();

        Float lastHealth = lastHealthMap.get(living);
        if (lastHealth != null && currentHealth < lastHealth - 0.1f) {
            onDamage(living);
        }
        lastHealthMap.put(living, currentHealth);

        if (displayDurationSeconds <= 0) {
            return currentHealth < maxHealth;
        }

        long lastDamage = getLastDamageTime(living);
        if (lastDamage == 0L) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - lastDamage;
        return elapsed < displayDurationSeconds * 1000L;
    }

    public void clear() {
        damageTimes.clear();
        lastHealthMap.clear();
    }
}