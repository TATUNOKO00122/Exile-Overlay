package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.DamagePopupManager;
import com.example.exile_overlay.client.damage.DamageType;
// import com.example.exile_overlay.client.render.kill.KillCountManager;
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
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "com.robertx22.mine_and_slash.vanilla_mc.packets.interaction.IParticleSpawnMaterial$DamageInformation", remap = false)
public class DamageInformationMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamageInformationMixin");

    @Unique
    private static Method exileOverlay$isCritMethod;
    @Unique
    private static Method exileOverlay$getDmgMapMethod;
    @Unique
    private static volatile boolean exileOverlay$methodsInitialized = false;

    @Unique
    private static void exileOverlay$ensureMethodsInitialized(Class<?> clazz) {
        if (exileOverlay$methodsInitialized) return;
        synchronized (DamageInformationMixin.class) {
            if (exileOverlay$methodsInitialized) return;
            try {
                exileOverlay$isCritMethod = clazz.getDeclaredMethod("isCrit");
                exileOverlay$isCritMethod.setAccessible(true);
            } catch (Exception e) {
                LOGGER.warn("isCrit() method not found: {}", e.getMessage());
            }
            try {
                exileOverlay$getDmgMapMethod = clazz.getDeclaredMethod("getDmgMap");
                exileOverlay$getDmgMapMethod.setAccessible(true);
            } catch (Exception e) {
                LOGGER.warn("getDmgMap() method not found: {}", e.getMessage());
            }
            exileOverlay$methodsInitialized = true;
        }
    }

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelDamageParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            DamagePopupConfig config = DamagePopupConfig.getInstance();

            if (!config.isShowDamage()) {
                return;
            }

            Object self = (Object) this;
            exileOverlay$ensureMethodsInitialized(self.getClass());

            if (exileOverlay$isCritMethod == null || exileOverlay$getDmgMapMethod == null) {
                return;
            }

            Object critResult = exileOverlay$isCritMethod.invoke(self);
            if (!(critResult instanceof Boolean)) {
                LOGGER.warn("isCrit() did not return Boolean: {}", critResult);
                return;
            }
            boolean isCrit = (Boolean) critResult;

            Object dmgMapObj = exileOverlay$getDmgMapMethod.invoke(self);

            if (entity instanceof LivingEntity living) {
                if (!config.isShowPlayerDamage() && living instanceof Player) {
                    ci.cancel();
                    return;
                }

                if (dmgMapObj instanceof Map) {
                    Map<?, ?> dmgMap = (Map<?, ?>) dmgMapObj;
                    for (Map.Entry<?, ?> entry : dmgMap.entrySet()) {
                        // Elements enumの.name()で"Physical","Cold"等を取得
                        String elementName = entry.getKey() instanceof Enum<?> e
                                ? e.name()
                                : entry.getKey().toString();
                        float damage = 0f;
                        if (entry.getValue() instanceof Number) {
                            damage = ((Number) entry.getValue()).floatValue();
                        }

                        if (damage > 0.01f) {
                            DamageType damageType = exileOverlay$getDamageTypeForElement(elementName);
                            if (damageType == DamageType.POISON) {
                                com.example.exile_overlay.client.render.ailment.ClientAilmentTracker.getInstance()
                                        .recordPoisonDamage(living);
                            }
                            float heightRatio = config.getPopupHeightRatio();
                            var position = living.position().add(0, living.getBbHeight() * heightRatio, 0);
                            DamagePopupManager.getInstance().addDamageNumber(
                                position,
                                damage,
                                isCrit,
                                damageType,
                                living.getId(),
                                net.minecraft.world.phys.Vec3.ZERO
                            );
                            LOGGER.debug("Showing damage popup: {} (crit: {}, element: {}) for entity {}",
                                damage, isCrit, elementName, living.getId());
                        }
                    }
                    // HPポーリング/setHealth由来の白い数値が二重表示されないようマーク
                    DamagePopupManager.getInstance().markMsDamageHandled(living.getId());

                    // キルカウンターへのダメージ記録と死亡判定
                    // KillCountManager.getInstance().recordPlayerAttack(living.getId());
                    // if (living.getHealth() <= 0.001f || living.isDeadOrDying() || living.deathTime > 0) {
                    //     KillCountManager.getInstance().checkEntityDeath(living);
                    // }
                }
                ci.cancel();
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process damage information: {}", e.getMessage(), e);
        }
    }

    @Unique
    private DamageType exileOverlay$getDamageTypeForElement(String element) {
        if (element == null) {
            return DamageType.PHYSICAL;
        }
        return switch (element.toUpperCase(java.util.Locale.ROOT)) {
            case "FIRE" -> DamageType.FIRE;
            case "COLD", "ICE" -> DamageType.ICE;
            case "NATURE", "LIGHTNING" -> DamageType.LIGHTNING;
            case "SHADOW", "DARK", "CHAOS" -> DamageType.MAGIC;
            case "POISON" -> DamageType.POISON;
            case "ELEMENTAL" -> DamageType.ELEMENTAL;
            default -> DamageType.PHYSICAL;
        };
    }
}
