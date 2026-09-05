package com.example.exile_overlay.dmgtracker.tracking;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.dmgtracker.network.AilmentSyncS2C;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でアクティブな状態異常（Ailment）保持エンティティを追跡し、
 * クライアントへ定期的に差分同期を行うマネージャー。
 */
public final class ServerAilmentTracker {

    private static final int MAX_PER_PLAYER = 20;

    private record TrackedTarget(
            WeakReference<LivingEntity> ref,
            ResourceKey<Level> dimension,
            UUID uuid,
            UUID playerUuid,
            boolean isPriority,
            long addedTime
    ) {}

    private static final class SyncState {
        List<AilmentSyncS2C.AilmentEntry> lastEntries;
        int lastSyncTick;

        SyncState(List<AilmentSyncS2C.AilmentEntry> entries, int tick) {
            this.lastEntries = entries;
            this.lastSyncTick = tick;
        }
    }

    private static final Map<UUID, TrackedTarget> TRACKED_ENTITIES = new ConcurrentHashMap<>();
    private static final Map<UUID, SyncState> SYNC_STATES = new ConcurrentHashMap<>();
    private static int tickCounter = 0;
    private static final int SCAN_INTERVAL_TICKS = 10; // 0.5秒ごとにスキャン
    private static final int HEARTBEAT_TICKS = 40;   // 最大2秒ごとに補正同期

    private ServerAilmentTracker() {}

    /**
     * 後方互換用: プレイヤー未指定で追跡
     */
    public static void track(Entity entity) {
        track(null, entity);
    }

    /**
     * エンティティに Ailment が付与・更新された際に追跡対象に追加（1プレイヤーあたり最大20体上限）
     */
    public static void track(ServerPlayer player, Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive() && !living.level().isClientSide()) {
            UUID uuid = living.getUUID();
            if (TRACKED_ENTITIES.containsKey(uuid)) {
                return;
            }

            UUID pUuid = player != null ? player.getUUID() : null;
            boolean priority = isPriorityTarget(living);

            // プレイヤーごとの現在の追跡数を集計
            int count = 0;
            UUID oldestUuid = null;
            long oldestTime = Long.MAX_VALUE;
            UUID oldestNonPriorityUuid = null;
            long oldestNonPriorityTime = Long.MAX_VALUE;

            for (Map.Entry<UUID, TrackedTarget> e : TRACKED_ENTITIES.entrySet()) {
                if (Objects.equals(e.getValue().playerUuid(), pUuid)) {
                    count++;
                    long t = e.getValue().addedTime();
                    if (t < oldestTime) {
                        oldestTime = t;
                        oldestUuid = e.getKey();
                    }
                    if (!e.getValue().isPriority() && t < oldestNonPriorityTime) {
                        oldestNonPriorityTime = t;
                        oldestNonPriorityUuid = e.getKey();
                    }
                }
            }

            if (count >= MAX_PER_PLAYER) {
                UUID toEvict;
                if (oldestNonPriorityUuid != null) {
                    toEvict = oldestNonPriorityUuid;
                } else if (priority) {
                    toEvict = oldestUuid;
                } else {
                    return; // 一般ターゲットは優先ターゲットを追い出せない
                }
                TRACKED_ENTITIES.remove(toEvict);
                SYNC_STATES.remove(toEvict);
            }

            ResourceKey<Level> dim = living.level().dimension();
            TRACKED_ENTITIES.put(uuid, new TrackedTarget(new WeakReference<>(living), dim, uuid, pUuid, priority, System.currentTimeMillis()));
        }
    }

    public static void onPlayerLogout(UUID playerUuid) {
        if (playerUuid == null) return;
        TRACKED_ENTITIES.entrySet().removeIf(e -> playerUuid.equals(e.getValue().playerUuid()));
        SYNC_STATES.entrySet().removeIf(e -> !TRACKED_ENTITIES.containsKey(e.getKey()));
    }

    private static boolean isPriorityTarget(LivingEntity living) {
        if (living == null) return false;
        if (!living.canChangeDimensions()) return true;

        if (MethodHandlesUtil.isAvailable()) {
            try {
                Object entityData = MethodHandlesUtil.getEntityData(living);
                if (entityData != null) {
                    Object rarity = MethodHandlesUtil.getMobRarityObj(entityData);
                    if (rarity != null) {
                        return MethodHandlesUtil.isRarityElite(rarity) || MethodHandlesUtil.isRaritySpecial(rarity);
                    }
                }
            } catch (Throwable ignored) {}
        }

        return false;
    }

    /**
     * サーバーTickイベントから呼び出し（定期スキャンと差分同期パケット送信）
     */
    public static void tick(MinecraftServer server) {
        if (server == null || TRACKED_ENTITIES.isEmpty()) return;

        tickCounter++;
        if (tickCounter % SCAN_INTERVAL_TICKS != 0) return;

        Iterator<Map.Entry<UUID, TrackedTarget>> it = TRACKED_ENTITIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedTarget> mapEntry = it.next();
            UUID uuid = mapEntry.getKey();
            TrackedTarget targetEntry = mapEntry.getValue();

            LivingEntity target = targetEntry.ref.get();
            if (target == null || !target.isAlive() || target.isRemoved()) {
                // 弱参照が切れている場合、該当ディメンションから一度だけ取得を試行
                ServerLevel level = server.getLevel(targetEntry.dimension);
                if (level != null && level.getEntity(uuid) instanceof LivingEntity living && living.isAlive()) {
                    target = living;
                    mapEntry.setValue(new TrackedTarget(new WeakReference<>(living), targetEntry.dimension, uuid, targetEntry.playerUuid(), targetEntry.isPriority(), targetEntry.addedTime()));
                } else {
                    it.remove();
                    SYNC_STATES.remove(uuid);
                    continue;
                }
            }

            List<AilmentSyncS2C.AilmentEntry> entries = MethodHandlesUtil.extractAilmentEntries(target);
            if (entries.isEmpty()) {
                // Ailment がすべて切れた場合、空パケットを1回送信してクライアント側のキャッシュをクリアし、追跡解除
                AilmentSyncS2C.sendToTracking(target, new AilmentSyncS2C(target.getId(), List.of()));
                it.remove();
                SYNC_STATES.remove(uuid);
            } else {
                SyncState state = SYNC_STATES.get(uuid);
                boolean shouldSend = false;

                if (state == null) {
                    shouldSend = true;
                } else {
                    int elapsedTicks = tickCounter - state.lastSyncTick;
                    if (hasSignificantChange(entries, state.lastEntries, elapsedTicks)) {
                        shouldSend = true;
                    } else if (elapsedTicks >= HEARTBEAT_TICKS) {
                        shouldSend = true;
                    }
                }

                if (shouldSend) {
                    AilmentSyncS2C.sendToTracking(target, new AilmentSyncS2C(target.getId(), entries));
                    SYNC_STATES.put(uuid, new SyncState(entries, tickCounter));
                }
            }
        }
    }

    private static boolean hasSignificantChange(List<AilmentSyncS2C.AilmentEntry> current, List<AilmentSyncS2C.AilmentEntry> previous, int elapsedTicks) {
        if (previous == null || current.size() != previous.size()) return true;
        for (AilmentSyncS2C.AilmentEntry c : current) {
            AilmentSyncS2C.AilmentEntry p = findEntry(previous, c.id());
            if (p == null) return true;
            if (c.stacks() != p.stacks()) return true;
            if (Math.abs(c.strength() - p.strength()) > 0.01f) return true;
            if (Math.abs(c.damage() - p.damage()) > 0.1f) return true;
            // 自然減少では即時送信せず（ハートビートに委ねる）、再付与等で時間が増加・延長した場合のみ即時同期
            int expectedTicks = Math.max(0, p.ticksLeft() - elapsedTicks);
            if (c.ticksLeft() > expectedTicks + 5) return true;
        }
        return false;
    }

    private static AilmentSyncS2C.AilmentEntry findEntry(List<AilmentSyncS2C.AilmentEntry> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            AilmentSyncS2C.AilmentEntry entry = list.get(i);
            if (entry.id().equals(id)) return entry;
        }
        return null;
    }

    /**
     * 指定レベルの追跡エントリを破棄（メモリリーク防止）
     */
    public static void clearForLevel(ServerLevel level) {
        if (level == null) return;
        ResourceKey<Level> dim = level.dimension();
        TRACKED_ENTITIES.entrySet().removeIf(e -> e.getValue().dimension.equals(dim));
        SYNC_STATES.entrySet().removeIf(e -> !TRACKED_ENTITIES.containsKey(e.getKey()));
    }

    public static void clear() {
        TRACKED_ENTITIES.clear();
        SYNC_STATES.clear();
    }
}
