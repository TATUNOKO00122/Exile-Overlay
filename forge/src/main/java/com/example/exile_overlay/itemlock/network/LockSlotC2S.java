package com.example.exile_overlay.itemlock.network;

import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアントからサーバーへ、アイテムロックスロットのマスクを同期するパケット。
 */
public class LockSlotC2S {
    private final long mask;

    public LockSlotC2S(long mask) {
        this.mask = mask;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(this.mask);
    }

    public static LockSlotC2S decode(FriendlyByteBuf buf) {
        return new LockSlotC2S(buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_SERVER) return;
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            LockManager.setServerLockedMaskNbtOnly(player, this.mask);
        });
        ctx.get().setPacketHandled(true);
    }
}
