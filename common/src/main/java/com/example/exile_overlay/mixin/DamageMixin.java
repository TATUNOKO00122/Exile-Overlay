package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.damage.DamagePopupManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(LivingEntity.class)
public abstract class DamageMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageMixin");

    @Unique
    private float exileOverlay$lastHealth = -1;

    @Inject(method = "setHealth", at = @At("TAIL"))
    private void exileOverlay$onSetHealth(float health, CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (!entity.level().isClientSide()) {
                return;
            }

            if (exileOverlay$lastHealth < 0) {
                exileOverlay$lastHealth = health;
                return;
            }

            float diff = exileOverlay$lastHealth - health;
            if (Math.abs(diff) > 0.1f) {
                DamagePopupManager.getInstance().onHealthChanged(entity, health);
            }

            exileOverlay$lastHealth = health;
        } catch (Exception e) {
            LOGGER.error("Failed to handle setHealth", e);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void exileOverlay$onTick(CallbackInfo ci) {
        try {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (!entity.level().isClientSide()) {
                return;
            }

            if (exileOverlay$lastHealth < 0) {
                exileOverlay$lastHealth = entity.getHealth();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle tick", e);
        }
    }
}
