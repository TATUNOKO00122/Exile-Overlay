package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.dmgtracker.network.MercenarySyncS2C;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ServerTickSyncHandler {
    private static int fallbackCounter = 0;
    private static final int FALLBACK_INTERVAL_TICKS = 300; // 15秒ごとに延長
    private static int syncCounter = 0; // 同期頻度を制限するカウンター
    private static final Set<UUID> playersWithActiveMerc = new HashSet<>();

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        syncCounter++;

        // 4ティック(0.2秒)ごとに傭兵情報を同期
        boolean shouldSyncMerc = (syncCounter % 4 == 0);
        if (shouldSyncMerc) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                UUID uuid = player.getUUID();
                MercenarySyncS2C mercPacket = MethodHandlesUtil.createMercenarySyncPacket(player);
                if (mercPacket != null && mercPacket.hasMercenary()) {
                    // 傭兵が存在する場合は同期パケット送信
                    MercenarySyncS2C.sendToPlayer(player, mercPacket);
                    playersWithActiveMerc.add(uuid);
                } else if (playersWithActiveMerc.remove(uuid)) {
                    // 傭兵が消失した場合はクリアパケットを1回送信
                    MercenarySyncS2C.sendToPlayer(player, new MercenarySyncS2C());
                }
            }
        }

        boolean shouldSync = (syncCounter % 20 == 0); // 20ティック(1.0秒)ごとに同期

        if (shouldSync) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (DamageTrackerManager.consumeDirty(player.getUUID())) {
                    TrackerSyncS2C.sendToPlayer(player);
                }
            }
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

    public static void onPlayerLogout(UUID uuid) {
        if (uuid != null) {
            playersWithActiveMerc.remove(uuid);
        }
    }
}
