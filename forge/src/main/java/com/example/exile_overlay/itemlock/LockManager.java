package com.example.exile_overlay.itemlock;

import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.itemlock.network.LockSlotSyncS2C;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アイテムロックスロットの管理クラス（サーバー・クライアント共通）。
 * 64bit整数のビットマスクにより、スロット0〜35のロック状態を管理する。
 */
public final class LockManager {
    public static final String NBT_TAG_KEY = "exile_overlay_itemlocks";
    private static volatile long clientLockedMask = 0L;
    private static volatile long clientItemPresentMask = 0L;
    private static final Map<UUID, Long> SERVER_ITEM_PRESENT_MAP = new ConcurrentHashMap<>();

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

    public static long clearBit(long mask, int slot) {
        if (slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) {
            return mask;
        }
        return mask & ~(1L << slot);
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

    public static void setServerLockedMaskNbtOnly(Player player, long mask) {
        if (player == null) return;
        CompoundTag persistent = player.getPersistentData();
        CompoundTag persisted = persistent.getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putLong(NBT_TAG_KEY, mask);
        persistent.put(Player.PERSISTED_NBT_TAG, persisted);
        persistent.putLong(NBT_TAG_KEY, mask);
    }

    public static void setServerLockedMask(ServerPlayer player, long mask) {
        if (player == null) return;
        setServerLockedMaskNbtOnly(player, mask);
        syncToClient(player);
    }

    public static void copyServerLockedMask(Player from, Player to) {
        if (from == null || to == null) return;
        long mask = getServerLockedMask(from);
        setServerLockedMaskNbtOnly(to, mask);
    }

    public static void toggleServerSlotLock(ServerPlayer player, int slot) {
        if (player == null || slot < 0 || slot >= ItemLockHelper.INVENTORY_SIZE) return;
        long current = getServerLockedMask(player);
        long updated = toggleBit(current, slot);
        setServerLockedMask(player, updated);
    }

    public static void resetServerItemTracking(Player player) {
        if (player != null) {
            SERVER_ITEM_PRESENT_MAP.remove(player.getUUID());
        }
    }

    public static boolean cleanupEmptyServerSlots(ServerPlayer player) {
        if (player == null || player.isDeadOrDying()) return false;
        long currentMask = getServerLockedMask(player);
        if (currentMask == 0L) return false;

        long prevPresent = SERVER_ITEM_PRESENT_MAP.getOrDefault(player.getUUID(), 0L);
        long newPresent = 0L;
        long newMask = currentMask;

        for (int i = 0; i < ItemLockHelper.INVENTORY_SIZE; i++) {
            boolean hasItem = !player.getInventory().getItem(i).isEmpty();
            if (hasItem) {
                newPresent |= (1L << i);
            } else if ((prevPresent & (1L << i)) != 0L) {
                if (isBitSet(newMask, i)) {
                    newMask = clearBit(newMask, i);
                }
            }
        }

        SERVER_ITEM_PRESENT_MAP.put(player.getUUID(), newPresent);

        if (newMask != currentMask) {
            setServerLockedMask(player, newMask);
            return true;
        }
        return false;
    }

    public static void syncToClient(ServerPlayer player) {
        if (player == null || player.connection == null || player.connection.connection == null) return;
        boolean canSend = player.server.isSingleplayer()
                || player.connection.connection.isMemoryConnection()
                || NetworkHandler.CHANNEL.isRemotePresent(player.connection.connection);
        if (!canSend) return;
        try {
            long mask = getServerLockedMask(player);
            NetworkHandler.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new LockSlotSyncS2C(mask)
            );
        } catch (Exception ignored) {
        }
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
        clientLockedMask = toggleBit(clientLockedMask, slot);
    }

    public static void resetClientItemTracking() {
        clientItemPresentMask = 0L;
    }

    public static boolean cleanupEmptyClientSlots(Player player) {
        if (player == null || clientLockedMask == 0L || player.isDeadOrDying()) return false;

        long currentMask = clientLockedMask;
        long newMask = currentMask;
        long newPresentMask = 0L;

        for (int i = 0; i < ItemLockHelper.INVENTORY_SIZE; i++) {
            boolean hasItem = !player.getInventory().getItem(i).isEmpty();
            if (hasItem) {
                newPresentMask |= (1L << i);
            } else if ((clientItemPresentMask & (1L << i)) != 0L) {
                // 直前までアイテムが存在していたスロットが空になった場合のみロック解除
                if (isBitSet(newMask, i)) {
                    newMask = clearBit(newMask, i);
                }
            }
        }

        clientItemPresentMask = newPresentMask;

        if (newMask != currentMask) {
            clientLockedMask = newMask;
            return true;
        }
        return false;
    }

    public static void resetClient() {
        clientLockedMask = 0L;
        clientItemPresentMask = 0L;
    }
}
