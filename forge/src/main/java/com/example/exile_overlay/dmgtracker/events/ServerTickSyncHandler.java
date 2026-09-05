package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.dmgtracker.network.MercenarySyncS2C;
import com.example.exile_overlay.dmgtracker.network.TrackerActionC2S;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.example.exile_overlay.dmgtracker.tracking.ServerAilmentTracker;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerTickSyncHandler {
    private static int fallbackCounter = 0;
    private static final int FALLBACK_INTERVAL_TICKS = 300; // 15秒ごとに延長
    private static int syncCounter = 0; // 同期頻度を制限するカウンター
    private static final Set<UUID> playersWithActiveMerc = ConcurrentHashMap.newKeySet();

    private static final class MercSyncState {
        final MercenarySyncS2C lastPacket;
        final int lastSyncTick;

        MercSyncState(MercenarySyncS2C packet, int tick) {
            this.lastPacket = packet;
            this.lastSyncTick = tick;
        }
    }

    private static final Map<UUID, MercSyncState> mercSyncStates = new ConcurrentHashMap<>();
    private static final int MERC_HEARTBEAT_TICKS = 20; // 1秒ごとに補正送信
    private static final Map<UUID, Boolean> lastCombatStates = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        ServerAilmentTracker.tick(server);

        syncCounter++;

        // 10ティック(0.5秒)ごとに傭兵情報をスキャンし、差分時またはハートビート時のみ同期
        boolean shouldSyncMerc = (syncCounter % 10 == 0) && MethodHandlesUtil.isMercenarySupported();
        if (shouldSyncMerc) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                MercenarySyncS2C mercPacket = MethodHandlesUtil.createMercenarySyncPacket(player);
                if (mercPacket != null && mercPacket.hasMercenary()) {
                    MercSyncState state = mercSyncStates.get(uuid);
                    boolean shouldSend = (state == null)
                            || hasMercChanged(mercPacket, state.lastPacket)
                            || (syncCounter - state.lastSyncTick >= MERC_HEARTBEAT_TICKS);

                    if (shouldSend) {
                        MercenarySyncS2C.sendToPlayer(player, mercPacket);
                        mercSyncStates.put(uuid, new MercSyncState(mercPacket, syncCounter));
                    }
                    playersWithActiveMerc.add(uuid);
                } else if (playersWithActiveMerc.remove(uuid)) {
                    // 傭兵が消失した場合はクリアパケットを1回送信
                    MercenarySyncS2C.sendToPlayer(player, new MercenarySyncS2C());
                    mercSyncStates.remove(uuid);
                }
            }
        }

        boolean shouldSync = (syncCounter % 20 == 0); // 20ティック(1.0秒)ごとに同期

        if (shouldSync) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                var data = DamageTrackerManager.getTracker(uuid);
                boolean inCombat = data != null && data.isInCombat();
                Boolean wasInCombat = lastCombatStates.put(uuid, inCombat);

                boolean combatEnded = (wasInCombat != null && wasInCombat && !inCombat);
                boolean dirty = DamageTrackerManager.consumeDirty(uuid);

                // ダメージ変動時または戦闘終了検知時に即座に同期
                if (dirty || combatEnded) {
                    TrackerSyncS2C.sendToPlayer(player);
                }
            }
        }

        if (syncCounter % 100 == 0) {
            DamageTrackerManager.cleanupOldMobRecords();
        }

        fallbackCounter++;
        if (fallbackCounter >= FALLBACK_INTERVAL_TICKS) {
            fallbackCounter = 0;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                var data = DamageTrackerManager.getTracker(player.getUUID());
                if (data != null && data.isInCombat()) {
                    DamageTrackerManager.consumeDirty(player.getUUID());
                    TrackerSyncS2C.sendToPlayer(player);
                }
            }
        }
    }

    private static boolean hasMercChanged(MercenarySyncS2C current, MercenarySyncS2C previous) {
        if (previous == null) return true;
        if (!current.hasMercenary() && previous.hasMercenary()) return true;
        if (current.hasMercenary() != previous.hasMercenary()) return true;
        if (Math.abs(current.getHealth() - previous.getHealth()) > 0.1f) return true;
        if (Math.abs(current.getMaxHealth() - previous.getMaxHealth()) > 0.1f) return true;
        if (Math.abs(current.getEnergyShield() - previous.getEnergyShield()) > 0.1f) return true;
        if (Math.abs(current.getMaxEnergyShield() - previous.getMaxEnergyShield()) > 0.1f) return true;
        if (current.getLevel() != previous.getLevel()) return true;
        if (!current.getClassId().equals(previous.getClassId())) return true;

        List<MercenarySyncS2C.SkillData> cSkills = current.getSkills();
        List<MercenarySyncS2C.SkillData> pSkills = previous.getSkills();
        if (cSkills.size() != pSkills.size()) return true;
        for (int i = 0; i < cSkills.size(); i++) {
            MercenarySyncS2C.SkillData cs = cSkills.get(i);
            MercenarySyncS2C.SkillData ps = pSkills.get(i);
            if (!cs.spellId().equals(ps.spellId())) return true;
            if (cs.onCooldown() != ps.onCooldown()) return true;
            if (Math.abs(cs.remainingTicks() - ps.remainingTicks()) > 4) return true;
        }
        return false;
    }

    public static void onPlayerLogout(UUID uuid) {
        if (uuid != null) {
            playersWithActiveMerc.remove(uuid);
            mercSyncStates.remove(uuid);
            lastCombatStates.remove(uuid);
            ServerAilmentTracker.onPlayerLogout(uuid);
            TrackerActionC2S.onPlayerLogout(uuid);
        }
    }
}
