package com.example.exile_overlay.forge.event;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.dmgtracker.network.BackpackPickupNotifyS2C;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * バックパックMOD（Sophisticated Backpacks等）によるアイテム吸い込み（イベントキャンセル）を検知し、
 * クライアントのHUD表示（Loot Journal等）へ通知するサーバーイベントハンドラー。
 */
@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BackpackPickupServerHandler {

    private static final class PendingPickup {
        int entityId = -1;
        int count = 0;
        ItemStack stack = ItemStack.EMPTY;
    }

    private static final ThreadLocal<PendingPickup> PENDING = ThreadLocal.withInitial(PendingPickup::new);

    private BackpackPickupServerHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityItemPickupPre(EntityItemPickupEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }

        ItemEntity itemEntity = event.getItem();
        if (itemEntity == null || itemEntity.isRemoved()) {
            return;
        }

        ItemStack rawStack = itemEntity.getItem();
        if (rawStack.isEmpty()) {
            return;
        }

        PendingPickup p = PENDING.get();
        p.entityId = itemEntity.getId();
        p.count = rawStack.getCount();
        p.stack = rawStack.copy();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onEntityItemPickupPost(EntityItemPickupEvent event) {
        if (event == null || event.getEntity() == null || event.getEntity().level().isClientSide()) {
            return;
        }

        PendingPickup p = PENDING.get();
        try {
            if (!event.isCanceled() || p.entityId == -1 || p.stack.isEmpty()) {
                return;
            }

            ItemEntity itemEntity = event.getItem();
            if (itemEntity == null || itemEntity.getId() != p.entityId) {
                return;
            }

            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }

            int currentCount = itemEntity.getItem().getCount();
            int absorbedCount = p.count - currentCount;

            if (absorbedCount > 0) {
                ItemStack sent = p.stack;
                sent.setCount(absorbedCount);
                BackpackPickupNotifyS2C.sendTo(player, sent);
            } else if (itemEntity.isRemoved() && p.count > 0) {
                ItemStack sent = p.stack;
                sent.setCount(p.count);
                BackpackPickupNotifyS2C.sendTo(player, sent);
            }
        } finally {
            p.entityId = -1;
            p.count = 0;
            p.stack = ItemStack.EMPTY;
        }
    }
}
