package com.example.exile_overlay.itemlock.network;

import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアントからサーバーへ、特定スロットのロック切り替えを要求するパケット。
 */
public class LockSlotC2S {
    private final int slot;

    public LockSlotC2S(int slot) {
        this.slot = slot;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
    }

    public static LockSlotC2S decode(FriendlyByteBuf buf) {
        return new LockSlotC2S(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_SERVER) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            LockManager.toggleServerSlotLock(player, slot);
        });
        ctx.get().setPacketHandled(true);
    }
}
