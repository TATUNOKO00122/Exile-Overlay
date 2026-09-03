package com.example.exile_overlay.dmgtracker.network;

import com.example.exile_overlay.client.compat.lootjournal.LootJournalPickupClientHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * バックパックMOD（Sophisticated Backpacks等）によるアイテム吸い込みをクライアントに通知するパケット（S2C）
 */
public class BackpackPickupNotifyS2C {

    private final ItemStack stack;

    public BackpackPickupNotifyS2C(ItemStack stack) {
        this.stack = stack != null ? stack : ItemStack.EMPTY;
    }

    public ItemStack getStack() {
        return stack;
    }

    public static void encode(BackpackPickupNotifyS2C msg, FriendlyByteBuf buf) {
        buf.writeItem(msg.stack);
    }

    public static BackpackPickupNotifyS2C decode(FriendlyByteBuf buf) {
        return new BackpackPickupNotifyS2C(buf.readItem());
    }

    public static void handle(BackpackPickupNotifyS2C msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            LootJournalPickupClientHandler.handlePickup(msg.stack);
        });
        ctx.setPacketHandled(true);
    }

    public static void sendTo(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;
        NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new BackpackPickupNotifyS2C(stack));
    }
}
