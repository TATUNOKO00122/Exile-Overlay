package com.example.exile_overlay.dmgtracker.tracking;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.dmgtracker.network.AilmentSyncS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * サーバー側でアクティブな状態異常（Ailment）保持エンティティを追跡し、
 * クライアントへ定期的に差分同期を行うマネージャー。
 */
public final class ServerAilmentTracker {

    private static final Set<UUID> TRACKED_ENTITIES = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;
    private static final int SYNC_INTERVAL_TICKS = 4; // 0.2秒ごとに同期

    private ServerAilmentTracker() {}

    /**
     * エンティティに Ailment が付与・更新された際に追跡対象に追加
     */
    public static void track(Entity entity) {
        if (entity instanceof LivingEntity living && living.isAlive()) {
            TRACKED_ENTITIES.add(living.getUUID());
        }
    }

    /**
     * サーバーTickイベントから呼び出し（定期スキャンと同期パケット送信）
     */
    public static void tick(MinecraftServer server) {
        if (server == null || TRACKED_ENTITIES.isEmpty()) return;

        tickCounter++;
        if (tickCounter % SYNC_INTERVAL_TICKS != 0) return;

        Iterator<UUID> it = TRACKED_ENTITIES.iterator();
        while (it.hasNext()) {
            UUID uuid = it.next();
            LivingEntity target = findLivingEntity(server, uuid);
            if (target == null || !target.isAlive()) {
                it.remove();
                continue;
            }

            List<AilmentSyncS2C.AilmentEntry> entries = MethodHandlesUtil.extractAilmentEntries(target);
            if (entries.isEmpty()) {
                // Ailment がすべて切れた場合、空パケットを1回送信してクライアント側のキャッシュをクリアし、追跡解除
                AilmentSyncS2C.sendToTracking(target, new AilmentSyncS2C(target.getId(), List.of()));
                it.remove();
            } else {
                // 最新の Ailment 状態を追跡クライアント群へ送信
                AilmentSyncS2C.sendToTracking(target, new AilmentSyncS2C(target.getId(), entries));
            }
        }
    }

    private static LivingEntity findLivingEntity(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    public static void clear() {
        TRACKED_ENTITIES.clear();
    }
}
