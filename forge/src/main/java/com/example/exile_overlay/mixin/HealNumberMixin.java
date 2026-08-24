package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.DamagePopupManager;
import com.example.exile_overlay.client.damage.DamageType;
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
            ci.cancel();

            DamagePopupConfig config = DamagePopupConfig.getInstance();
            if (!config.isShowHealing()) {
                return;
            }

            if (entity instanceof Player && !config.isShowPlayerHealing()) {
                return;
            }

            Object self = (Object) this;

            Method numberMethod;
            try {
                numberMethod = self.getClass().getMethod("number");
            } catch (NoSuchMethodException e) {
                try {
                    numberMethod = self.getClass().getDeclaredMethod("number");
                    numberMethod.setAccessible(true);
                } catch (NoSuchMethodException ex) {
                    LOGGER.warn("number() method not found, skipping heal popup");
                    return;
                }
            }
            Object result = numberMethod.invoke(self);
            if (!(result instanceof Number num)) {
                LOGGER.warn("number() did not return Number: {}", result);
                return;
            }
            float healAmount = num.floatValue();

            if (healAmount <= 0) {
                return;
            }

            if (entity instanceof LivingEntity living) {
                DamagePopupManager.getInstance().addHealFromMsPacket(living, healAmount);
                LOGGER.debug("Showing heal popup: {} for entity {}", healAmount, living.getId());
            } else {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.world.phys.Vec3 position;
                if (entity != null) {
                    position = entity.position().add(0, entity.getBbHeight() * 0.8f, 0);
                } else if (mc.player != null) {
                    position = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(2.5));
                } else {
                    return;
                }
                DamagePopupManager.getInstance().addDamageNumber(position, healAmount, false, DamageType.HEALING, -1, net.minecraft.world.phys.Vec3.ZERO);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process heal number: {}", e.getMessage(), e);
        }
    }
}
