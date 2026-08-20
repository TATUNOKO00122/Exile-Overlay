package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerTickSyncHandler {
    private static int fallbackCounter = 0;
    private static final int FALLBACK_INTERVAL_TICKS = 300; // 15秒ごとに延長
    private static int syncCounter = 0; // 同期頻度を制限するカウンター

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        if (server == null) return;

        syncCounter++;
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
}
