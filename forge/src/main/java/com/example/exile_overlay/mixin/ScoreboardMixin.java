package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Scoreboard.class)
public abstract class ScoreboardMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ScoreboardMixin");

    @Inject(
        method = "setDisplayObjective(ILnet/minecraft/world/scores/Objective;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void exileOverlay$cancelDungeonScoreboard(int slot, Objective objective, CallbackInfo ci) {
        try {
            if (slot != 1 || objective == null) return;

            if (!EquipmentDisplayConfig.getInstance().isCancelDungeonRealmScoreboard()) return;

            if ("completion_percent".equals(objective.getName())) {
                ci.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to cancel dungeon scoreboard", e);
        }
    }
}
