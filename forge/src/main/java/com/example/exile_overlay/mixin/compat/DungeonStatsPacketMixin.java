package com.example.exile_overlay.mixin.compat;

import com.example.exile_overlay.client.dungeon.DungeonTimerManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "com.robertx22.dungeon_realm.packets.DungeonStatsPacket", remap = false)
public class DungeonStatsPacketMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DungeonStatsPacketMixin");

    @Shadow
    public ItemStack snapshotStack;

    @Inject(method = "onReceived", at = @At("TAIL"), require = 0)
    private void exileOverlay$onStatsReceived(Object context, CallbackInfo ci) {
        try {
            if (snapshotStack != null && !snapshotStack.isEmpty()) {
                DungeonTimerManager.getInstance().resetTimer();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process dungeon timer reset: {}", e.getMessage());
        }
    }
}
