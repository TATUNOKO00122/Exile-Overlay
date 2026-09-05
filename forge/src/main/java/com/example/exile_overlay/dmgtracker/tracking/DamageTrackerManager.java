package com.example.exile_overlay.dmgtracker.tracking;

import com.example.exile_overlay.dmgtracker.util.IDamageEventAccessor;
import com.example.exile_overlay.dmgtracker.util.SkillIdResolver;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DamageTrackerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageTrackerManager");

    public record MobLastHitRecord(UUID attackerUuid, String skillId, String displayName, long timestampMs) {}

    private static final Map<UUID, PlayerTrackerData> playerData = new ConcurrentHashMap<>();
    private static final Map<UUID, MobLastHitRecord> mobLastHitRecords = new ConcurrentHashMap<>();
    private static final Set<UUID> dirtyPlayers = ConcurrentHashMap.newKeySet();
    private static final int MAX_MOB_CACHE = 1000;

    public static PlayerTrackerData getTracker(UUID playerUuid) {
        return playerData.computeIfAbsent(playerUuid, id -> new PlayerTrackerData());
    }

    public static void recordDamage(ServerPlayer player, DamageEvent dmgEvent) {
        PlayerTrackerData tracker = getTracker(player.getUUID());

        String skillId = SkillIdResolver.resolveSkillId(dmgEvent);
        String displayName = SkillIdResolver.resolveDisplayName(dmgEvent);
        SkillDamageStats stats = tracker.getOrCreateStats(skillId, displayName);

        if (dmgEvent.target != null) {
            long now = System.currentTimeMillis();
            mobLastHitRecords.put(dmgEvent.target.getUUID(),
                    new MobLastHitRecord(player.getUUID(), skillId, displayName, now));
            if (mobLastHitRecords.size() > MAX_MOB_CACHE) {
                // 上限超過時は直近5秒未満の戦闘中レコードを保護し、古いエントリを剪定
                long cutoff = now - 5_000L;
                mobLastHitRecords.entrySet().removeIf(e -> e.getValue().timestampMs() < cutoff);
            }
        }

        boolean avoided = dmgEvent.data.isHitAvoided();
        if (avoided) {
            stats.recordMiss();
            tracker.markDamageEvent();
            dirtyPlayers.add(player.getUUID());
            return;
        }

        boolean isCrit = dmgEvent.data.isCrit();

        DamageEvent.DmgByElement info = (dmgEvent instanceof IDamageEventAccessor accessor)
                ? accessor.exileOverlay$getDmgByElement()
                : null;

        if (info != null && info.getDmgmap() != null && !info.getDmgmap().isEmpty()) {
            stats.recordMultiElementHit(info.getDmgmap(), isCrit);
        } else {
            float damage = dmgEvent.getActualDamage();
            Elements element = dmgEvent.getElement();
            stats.recordHit(damage, isCrit, element);
        }

        tracker.markDamageEvent();
        dirtyPlayers.add(player.getUUID());
    }

    public static void recordKill(UUID playerUuid, String skillId) {
        recordKill(playerUuid, skillId, skillId);
    }

    public static void recordKill(UUID playerUuid, String skillId, String displayName) {
        if (playerUuid == null || skillId == null) return;
        PlayerTrackerData tracker = playerData.get(playerUuid);
        if (tracker == null) return;

        SkillDamageStats stats = tracker.getOrCreateStats(skillId, displayName != null ? displayName : skillId);
        stats.recordKill();
        dirtyPlayers.add(playerUuid);
    }

    private static final long MOB_RECORD_EXPIRY_MS = 10_000L;

    public static MobLastHitRecord consumeMobLastHit(UUID mobUuid, UUID killerPlayerUuid) {
        if (mobUuid == null) return null;
        MobLastHitRecord record = mobLastHitRecords.get(mobUuid);
        if (record != null) {
            long now = System.currentTimeMillis();
            if ((now - record.timestampMs()) > MOB_RECORD_EXPIRY_MS) {
                mobLastHitRecords.remove(mobUuid);
                return null;
            }
            if (killerPlayerUuid == null || killerPlayerUuid.equals(record.attackerUuid())) {
                return mobLastHitRecords.remove(mobUuid);
            }
        }
        return null;
    }

    public static void cleanupOldMobRecords() {
        long now = System.currentTimeMillis();
        mobLastHitRecords.entrySet().removeIf(e -> (now - e.getValue().timestampMs()) > MOB_RECORD_EXPIRY_MS);
    }

    public static boolean consumeDirty(UUID playerUuid) {
        return dirtyPlayers.remove(playerUuid);
    }

    public static void resetTracker(UUID playerUuid) {
        PlayerTrackerData tracker = getTracker(playerUuid);
        tracker.reset();
        dirtyPlayers.add(playerUuid);
    }

    public static void removeTracker(UUID playerUuid) {
        playerData.remove(playerUuid);
        dirtyPlayers.remove(playerUuid);
    }

    public static void clearAll() {
        playerData.clear();
        mobLastHitRecords.clear();
        dirtyPlayers.clear();
    }
}
