package com.example.exile_overlay.forge.event;

import com.example.exile_overlay.forge.util.BackpackPickupHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class ServerTickEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BackpackPickupHelper.processTick();
        }
    }
}