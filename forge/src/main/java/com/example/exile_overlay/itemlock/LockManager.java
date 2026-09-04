package com.example.exile_overlay.itemlock;

import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.itemlock.network.LockSlotC2S;
import com.example.exile_overlay.itemlock.network.LockSlotSyncS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * アイテムロックスロットの管理クラス（サーバー・クライアント共通）。
 * 64bit整数のビットマスクにより、スロット0〜35のロック状態を管理する。
 */
public final class LockManager {
    public static final String NBT_TAG_KEY = "exile_overlay_itemlocks";
    private static volatile long clientLockedMask = 0L;

    private LockManager() {}

    // ==========================================
    // 共通ビット操作
    // ==========================================

    public static boolean isBitSet(long mask, int slot) {
        if (slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) {
            return false;
        }
        return (mask & (1L << slot)) != 0L;
    }

    public static long toggleBit(long mask, int slot) {
        if (slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) {
            return mask;
        }
        return mask ^ (1L << slot);
    }

    // ==========================================
    // サーバーサイド処理
    // ==========================================

    public static long getServerLockedMask(Player player) {
        if (player == null) return 0L;
        CompoundTag persistent = player.getPersistentData();
        if (persistent.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
            if (persisted.contains(NBT_TAG_KEY)) {
                return persisted.getLong(NBT_TAG_KEY);
            }
        }
        return persistent.getLong(NBT_TAG_KEY);
    }

    public static boolean isServerSlotLocked(Player player, int slot) {
        if (player == null) return false;
        return isBitSet(getServerLockedMask(player), slot);
    }

    public static void setServerLockedMask(ServerPlayer player, long mask) {
        if (player == null) return;
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putLong(NBT_TAG_KEY, mask);
        persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        persistent.putLong(NBT_TAG_KEY, mask);
        NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new LockSlotSyncS2C(mask)
        );
    }

    public static void toggleServerSlotLock(ServerPlayer player, int slot) {
        if (player == null || slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) return;
        long current = getServerLockedMask(player);
        long updated = toggleBit(current, slot);
        setServerLockedMask(player, updated);
    }

    public static void syncToClient(ServerPlayer player) {
        if (player == null) return;
        long mask = getServerLockedMask(player);
        NetworkHandler.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new LockSlotSyncS2C(mask)
        );
    }

    // ==========================================
    // クライアントサイド処理
    // ==========================================

    public static boolean isClientSlotLocked(int slot) {
        return isBitSet(clientLockedMask, slot);
    }

    public static long getClientLockedMask() {
        return clientLockedMask;
    }

    public static void setClientLockedMask(long mask) {
        clientLockedMask = mask;
    }

    public static void toggleClientSlotLock(int slot) {
        if (slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) return;
        // 即時プレビュー反映
        clientLockedMask = toggleBit(clientLockedMask, slot);
        // サーバーへ更新要求を送信
        NetworkHandler.CHANNEL.sendToServer(new LockSlotC2S(slot));
    }

    public static void resetClient() {
        clientLockedMask = 0L;
    }
}
