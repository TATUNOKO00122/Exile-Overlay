package com.example.exile_overlay.forge.mixin;

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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial$DamageInformation", remap = false)
public class DamageInformationMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageInformationMixin");

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelDamageParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            DamagePopupConfig config = DamagePopupConfig.getInstance();

            if (!config.isShowDamage()) {
                return;
            }

            Object self = (Object) this;

            Method isCritMethod;
            try {
                isCritMethod = self.getClass().getDeclaredMethod("isCrit");
            } catch (NoSuchMethodException e) {
                LOGGER.warn("isCrit() method not found, skipping damage popup");
                return;
            }
            isCritMethod.setAccessible(true);
            Object critResult = isCritMethod.invoke(self);
            if (!(critResult instanceof Boolean)) {
                LOGGER.warn("isCrit() did not return Boolean: {}", critResult);
                return;
            }
            boolean isCrit = (Boolean) critResult;

            Method getDmgMapMethod;
            try {
                getDmgMapMethod = self.getClass().getDeclaredMethod("getDmgMap");
            } catch (NoSuchMethodException e) {
                LOGGER.warn("getDmgMap() method not found, skipping damage popup");
                return;
            }
            getDmgMapMethod.setAccessible(true);
            Object dmgMapObj = getDmgMapMethod.invoke(self);

            float totalDamage = 0f;
            String dominantElement = "Physical";
            float maxElementDamage = 0f;

            if (dmgMapObj instanceof Map) {
                Map<?, ?> dmgMap = (Map<?, ?>) dmgMapObj;
                for (Map.Entry<?, ?> entry : dmgMap.entrySet()) {
                    String elementName = entry.getKey().toString();
                    float damage = 0f;
                    if (entry.getValue() instanceof Number) {
                        damage = ((Number) entry.getValue()).floatValue();
                    }
                    totalDamage += damage;

                    if (damage > maxElementDamage) {
                        maxElementDamage = damage;
                        dominantElement = elementName;
                    }
                }
            }

            if (entity instanceof LivingEntity living && totalDamage > 0) {
                if (!config.isShowPlayerDamage() && living instanceof Player) {
                    ci.cancel();
                    return;
                }

                DamageType damageType = getDamageTypeForElement(dominantElement);
                float heightRatio = config.getPopupHeightRatio();
                var position = living.position().add(0, living.getBbHeight() * heightRatio, 0);
                DamagePopupManager.getInstance().addDamageNumber(
                    position,
                    totalDamage,
                    isCrit,
                    damageType,
                    living.getId(),
                    net.minecraft.world.phys.Vec3.ZERO
                );
                LOGGER.debug("Showing damage popup: {} (crit: {}, element: {}) for entity {}",
                    totalDamage, isCrit, dominantElement, living.getId());
            }

            ci.cancel();

        } catch (Exception e) {
            LOGGER.error("Failed to process damage information: {}", e.getMessage(), e);
        }
    }

    private DamageType getDamageTypeForElement(String element) {
        return switch (element) {
            case "Fire" -> DamageType.FIRE;
            case "Cold" -> DamageType.ICE;
            case "Nature" -> DamageType.LIGHTNING;
            case "Shadow" -> DamageType.MAGIC;
            case "Elemental" -> DamageType.ELEMENTAL;
            default -> DamageType.PHYSICAL;
        };
    }
}
