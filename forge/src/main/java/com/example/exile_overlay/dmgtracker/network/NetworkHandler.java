package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.itemlock.network.LockSlotC2S;
import com.example.exile_overlay.itemlock.network.LockSlotSyncS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ExileOverlayMod.MOD_ID, "dmg_tracker"),
            () -> PROTOCOL_VERSION,
            version -> true,
            version -> true
    );

    private static int packetId = 0;
    private static boolean registered = false;

    public static synchronized void register(IEventBus modBus) {
        if (registered) return;
        registered = true;
        CHANNEL.registerMessage(packetId++,
                TrackerSyncS2C.class,
                TrackerSyncS2C::encode,
                TrackerSyncS2C::decode,
                TrackerSyncS2C::handle);

        CHANNEL.registerMessage(packetId++,
                TrackerActionC2S.class,
                TrackerActionC2S::encode,
                TrackerActionC2S::decode,
                TrackerActionC2S::handle);

        CHANNEL.registerMessage(packetId++,
                MercenarySyncS2C.class,
                MercenarySyncS2C::encode,
                MercenarySyncS2C::decode,
                MercenarySyncS2C::handle);

        CHANNEL.registerMessage(packetId++,
                AilmentSyncS2C.class,
                AilmentSyncS2C::encode,
                AilmentSyncS2C::decode,
                AilmentSyncS2C::handle);

        CHANNEL.registerMessage(packetId++,
                LockSlotC2S.class,
                LockSlotC2S::encode,
                LockSlotC2S::decode,
                LockSlotC2S::handle);

        CHANNEL.registerMessage(packetId++,
                LockSlotSyncS2C.class,
                LockSlotSyncS2C::encode,
                LockSlotSyncS2C::decode,
                LockSlotSyncS2C::handle);
    }
}
