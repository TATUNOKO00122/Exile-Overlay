package com.example.exile_overlay.dmgtracker.tracking;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.dmgtracker.network.AilmentSyncS2C;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でアクティブな状態異常（Ailment）保持エンティティを追跡し、
 * クライアントへ定期的に差分同期を行うマネージャー。
 */
public final class ServerAilmentTracker {

    private record TrackedTarget(
            WeakReference<LivingEntity> ref,
            ResourceKey<Level> dimension,
            UUID uuid
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
    private static final int SCAN_INTERVAL_TICKS = 4; // 0.2秒ごとにスキャン
    private static final int HEARTBEAT_TICKS = 20;   // 最大1秒ごとに補正同期

    private ServerAilmentTracker() {}

    /**
     * エンティティに Ailment が付与・更新された際に追跡対象に追加
     */
    public static void track(Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive() && !living.level().isClientSide()) {
            ResourceKey<Level> dim = living.level().dimension();
            UUID uuid = living.getUUID();
            TRACKED_ENTITIES.put(uuid, new TrackedTarget(new WeakReference<>(living), dim, uuid));
        }
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
                    mapEntry.setValue(new TrackedTarget(new WeakReference<>(living), targetEntry.dimension, uuid));
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
                } else if (hasSignificantChange(entries, state.lastEntries)) {
                    shouldSend = true;
                } else if (tickCounter - state.lastSyncTick >= HEARTBEAT_TICKS) {
                    shouldSend = true;
                }

                if (shouldSend) {
                    AilmentSyncS2C.sendToTracking(target, new AilmentSyncS2C(target.getId(), entries));
                    SYNC_STATES.put(uuid, new SyncState(entries, tickCounter));
                }
            }
        }
    }

    private static boolean hasSignificantChange(List<AilmentSyncS2C.AilmentEntry> current, List<AilmentSyncS2C.AilmentEntry> previous) {
        if (previous == null || current.size() != previous.size()) return true;
        for (int i = 0; i < current.size(); i++) {
            AilmentSyncS2C.AilmentEntry c = current.get(i);
            AilmentSyncS2C.AilmentEntry p = previous.get(i);
            if (!c.id().equals(p.id())) return true;
            if (c.stacks() != p.stacks()) return true;
            if (Math.abs(c.strength() - p.strength()) > 0.01f) return true;
            if (Math.abs(c.damage() - p.damage()) > 0.1f) return true;
            if (Math.abs(c.ticksLeft() - p.ticksLeft()) > 20) return true;
        }
        return false;
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
