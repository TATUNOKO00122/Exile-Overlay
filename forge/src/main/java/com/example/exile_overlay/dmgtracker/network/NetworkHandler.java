package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.ExileOverlayMod;
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

    public static void register(IEventBus modBus) {
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
    }
}
