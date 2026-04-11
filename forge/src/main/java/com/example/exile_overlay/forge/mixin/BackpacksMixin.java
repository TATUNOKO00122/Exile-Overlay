package com.example.exile_overlay.forge.mixin;

import com.example.exile_overlay.forge.util.BackpackPickupHelper;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = com.robertx22.mine_and_slash.capability.player.data.Backpacks.class, remap = false)
public abstract class BackpacksMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/BackpacksMixin");

    @Inject(method = "tryAutoPickup", at = @At(value = "RETURN"), remap = false)
    private void exileOverlay$onTryAutoPickupReturn(Player p, ItemStack stack, boolean shouldPlaySound,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (!cir.getReturnValue()) return;
            if (!(p instanceof ServerPlayer serverPlayer)) return;

            AABB searchBox = serverPlayer.getBoundingBox().inflate(2.0);
            List<ItemEntity> nearbyItems = serverPlayer.level().getEntitiesOfClass(
                    ItemEntity.class,
                    searchBox,
                    itemEntity -> ItemStack.isSameItemSameTags(itemEntity.getItem(), stack));

            if (nearbyItems.isEmpty()) return;

            ItemEntity targetEntity = nearbyItems.get(0);
            int count = stack.getCount() > 0 ? stack.getCount() : 1;

            serverPlayer.connection.send(
                    new ClientboundTakeItemEntityPacket(targetEntity.getId(), serverPlayer.getId(), count));

            targetEntity.setPickUpDelay(50);
            BackpackPickupHelper.queueDiscard(targetEntity, 5);
        } catch (Exception e) {
            LOGGER.error("Failed to handle backpack pickup", e);
        }
    }
}