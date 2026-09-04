package com.example.exile_overlay.itemlock.network;

import com.example.exile_overlay.itemlock.LockManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバーからクライアントへ、現在のスロットロック状態（ビットマスク）を同期するパケット。
 */
public class LockSlotSyncS2C {
    private final long lockedMask;

    public LockSlotSyncS2C(long lockedMask) {
        this.lockedMask = lockedMask;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(lockedMask);
    }

    public static LockSlotSyncS2C decode(FriendlyByteBuf buf) {
        return new LockSlotSyncS2C(buf.readLong());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) return;
            LockManager.setClientLockedMask(lockedMask);
        });
        ctx.get().setPacketHandled(true);
    }
}
