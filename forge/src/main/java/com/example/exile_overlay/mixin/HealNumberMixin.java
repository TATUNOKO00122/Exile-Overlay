package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.DamagePopupManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial$HealNumber", remap = false)
public class HealNumberMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/HealNumberMixin");

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelHealParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            DamagePopupConfig config = DamagePopupConfig.getInstance();

            if (!config.isShowHealing()) {
                return;
            }

            Object self = (Object) this;

            Method numberMethod;
            try {
                numberMethod = self.getClass().getDeclaredMethod("number");
            } catch (NoSuchMethodException e) {
                LOGGER.warn("number() method not found, skipping heal popup");
                return;
            }
            numberMethod.setAccessible(true);
            Object result = numberMethod.invoke(self);
            if (!(result instanceof Number)) {
                LOGGER.warn("number() did not return Number: {}", result);
                return;
            }
            float healAmount = ((Number) result).floatValue();

            if (entity instanceof LivingEntity living && healAmount > 0) {
                if (living instanceof Player && !config.isShowPlayerHealing()) {
                    ci.cancel();
                    return;
                }

                DamagePopupManager.getInstance().addHealFromMsPacket(living, healAmount);
                LOGGER.debug("Showing heal popup: {} for entity {}", healAmount, living.getId());
            }

            ci.cancel();

        } catch (Exception e) {
            LOGGER.error("Failed to process heal number: {}", e.getMessage(), e);
        }
    }
}
