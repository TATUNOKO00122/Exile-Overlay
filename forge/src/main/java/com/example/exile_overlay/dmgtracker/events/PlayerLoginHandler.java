package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLoginHandler {

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            TrackerSyncS2C.sendToPlayer(serverPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            DamageTrackerManager.removeTracker(event.getEntity().getUUID());
            ServerTickSyncHandler.onPlayerLogout(event.getEntity().getUUID());
        }
    }
}
