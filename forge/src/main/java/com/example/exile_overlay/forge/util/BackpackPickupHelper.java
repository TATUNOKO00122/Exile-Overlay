package com.example.exile_overlay.forge.util;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BackpackPickupHelper {
    private static final Map<Entity, Integer> delayedDiscards = new HashMap<>();

    private BackpackPickupHelper() {}

    public static void queueDiscard(Entity entity, int delayTicks) {
        if (entity == null || delayTicks <= 0) return;
        delayedDiscards.put(entity, delayTicks);
    }

    public static void processTick() {
        if (delayedDiscards.isEmpty()) return;

        Iterator<Map.Entry<Entity, Integer>> iterator = delayedDiscards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Entity, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                Entity entity = entry.getKey();
                if (entity.isAlive()) {
                    entity.discard();
                }
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }

    public static void clear() {
        delayedDiscards.clear();
    }
}