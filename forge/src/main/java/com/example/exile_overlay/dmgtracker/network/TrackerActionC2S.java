package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkDirection;

import java.util.function.Supplier;

public class TrackerActionC2S {
    public static final int ACTION_RESET = 0;
    public static final int ACTION_REQUEST_SYNC = 1;

    private final int action;

    public TrackerActionC2S(int action) {
        this.action = action;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
    }

    public static TrackerActionC2S decode(FriendlyByteBuf buf) {
        return new TrackerActionC2S(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_SERVER) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (action) {
                case ACTION_RESET:
                    DamageTrackerManager.resetTracker(player.getUUID());
                    break;
                case ACTION_REQUEST_SYNC:
                    TrackerSyncS2C.sendToPlayer(player);
                    break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
