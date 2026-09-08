package com.example.exile_overlay.itemlock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * サーバー側でのプレイヤー参加・リスポーン時のアイテムロック状態同期および永続化ハンドラ。
 */
public final class ItemLockServerHandler {

    private ItemLockServerHandler() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ItemLockServerHandler());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LockManager.resetServerItemTracking(player);
            LockManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LockManager.resetServerItemTracking(player);
            LockManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        // 死亡リスポーンまたはディメンション移動時にNBTデータを引き継ぐ（パケット送信はRespawn/ChangedDimensionで行う）
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            LockManager.copyServerLockedMask(event.getOriginal(), newPlayer);
            LockManager.resetServerItemTracking(newPlayer);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LockManager.resetServerItemTracking(player);
            LockManager.syncToClient(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            LockManager.resetServerItemTracking(event.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            LockManager.cleanupEmptyServerSlots(serverPlayer);
        }
    }
}
