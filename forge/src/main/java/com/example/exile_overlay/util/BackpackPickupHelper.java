package com.example.exile_overlay.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * バックパック自動ピックアップのアイテム遅延破棄ヘルパー
 *
 * エンティティをキーとして直接保持するのではなく、
 * (levelHashCode, entityId) のペアで管理し、Entity参照はWeakReferenceを使用することでメモリリークを防ぐ。
 */
public final class BackpackPickupHelper {

    /**
     * キー: level.hashCode() << 32 | entityId の複合キー
     * 値: 残りティック数とWeakReferenceを保持するオブジェクト
     */
    private static final Map<Long, EntityRef> delayedDiscards = new HashMap<>();

    private BackpackPickupHelper() {}

    /** エンティティの破棄をキューに入れる */
    public static void queueDiscard(Entity entity, int delayTicks) {
        if (entity == null || delayTicks <= 0) return;
        long key = makeKey(entity);
        delayedDiscards.put(key, new EntityRef(entity, delayTicks));
    }

    /** サーバーTickごとに呼ぶ。カウントダウンし、0になったら破棄する */
    public static void processTick() {
        if (delayedDiscards.isEmpty()) return;

        Iterator<Map.Entry<Long, EntityRef>> iterator = delayedDiscards.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, EntityRef> entry = iterator.next();
            EntityRef ref = entry.getValue();
            Entity entity = ref.getEntity();

            if (entity == null || !entity.isAlive()) {
                iterator.remove();
                continue;
            }

            ref.ticks--;
            if (ref.ticks <= 0) {
                entity.discard();
                iterator.remove();
            }
        }
    }

    /** ワールドアンロード時などに呼んでエントリをクリアする */
    public static void clear() {
        delayedDiscards.clear();
    }

    /**
     * 指定レベルに属するエントリをクリアする。
     * ワールド切り替え時に古いエンティティ参照を解放する。
     */
    public static void clearForLevel(ServerLevel level) {
        if (level == null) return;
        int levelHash = System.identityHashCode(level);
        delayedDiscards.entrySet().removeIf(e -> {
            long key = e.getKey();
            return (int) (key >> 32) == levelHash;
        });
    }

    private static long makeKey(Entity entity) {
        // 上位32bit: levelのidentityHashCode、下位32bit: entityId
        int levelHash = System.identityHashCode(entity.level());
        int entityId = entity.getId();
        return ((long) levelHash << 32) | (entityId & 0xFFFFFFFFL);
    }

    private static final class EntityRef {
        private final WeakReference<Entity> entityRef;
        int ticks;

        EntityRef(Entity entity, int ticks) {
            this.entityRef = new WeakReference<>(entity);
            this.ticks = ticks;
        }

        Entity getEntity() {
            return entityRef.get();
        }
    }
}