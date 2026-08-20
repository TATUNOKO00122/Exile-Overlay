package com.example.exile_overlay.dmgtracker.tracking;

import com.example.exile_overlay.dmgtracker.util.SkillIdResolver;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.enumclasses.Elements;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DamageTrackerManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageTrackerManager");

    private static final Map<UUID, PlayerTrackerData> playerData = new ConcurrentHashMap<>();
    private static final Map<UUID, String> mobLastHitSkill = new ConcurrentHashMap<>();
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
            mobLastHitSkill.put(dmgEvent.target.getUUID(), skillId);
            // サイズ超過時は全消しせず半分を削除し、直近の記録を保持する
            if (mobLastHitSkill.size() > MAX_MOB_CACHE) {
                int removeCount = MAX_MOB_CACHE / 2;
                Iterator<UUID> it = mobLastHitSkill.keySet().iterator();
                while (it.hasNext() && removeCount-- > 0) {
                    it.next();
                    it.remove();
                }
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

        DamageEvent.DmgByElement info = (dmgEvent instanceof com.example.exile_overlay.dmgtracker.util.IDamageEventAccessor accessor)
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
        PlayerTrackerData tracker = getTracker(playerUuid);
        SkillDamageStats stats = tracker.getOrCreateStats(skillId, skillId);
        stats.recordKill();
        dirtyPlayers.add(playerUuid);
    }

    public static String consumeMobLastHitSkill(UUID mobUuid) {
        return mobLastHitSkill.remove(mobUuid);
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
}
