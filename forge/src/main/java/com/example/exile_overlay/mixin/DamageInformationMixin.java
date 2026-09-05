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
    private static Method exileOverlay$elementsMethod;
    @Unique
    private static Method exileOverlay$damageMethod;
    @Unique
    private static volatile boolean exileOverlay$methodsInitialized = false;

    @Unique
    private static void exileOverlay$ensureMethodsInitialized(Class<?> clazz) {
        if (exileOverlay$methodsInitialized) return;
        synchronized (DamageInformationMixin.class) {
            if (exileOverlay$methodsInitialized) return;
            try {
                exileOverlay$isCritMethod = clazz.getMethod("isCrit");
            } catch (Exception e) {
                try {
                    exileOverlay$isCritMethod = clazz.getDeclaredMethod("isCrit");
                    exileOverlay$isCritMethod.setAccessible(true);
                } catch (Exception ex) {
                    LOGGER.warn("isCrit method lookup failed: {}", ex.getMessage());
                }
            }
            try {
                exileOverlay$getDmgMapMethod = clazz.getMethod("getDmgMap");
            } catch (Exception e) {
                try {
                    exileOverlay$getDmgMapMethod = clazz.getDeclaredMethod("getDmgMap");
                    exileOverlay$getDmgMapMethod.setAccessible(true);
                } catch (Exception ex) {
                    LOGGER.warn("getDmgMap method lookup failed: {}", ex.getMessage());
                }
            }
            try {
                exileOverlay$elementsMethod = clazz.getMethod("elements");
            } catch (Exception ignored) {
            }
            try {
                exileOverlay$damageMethod = clazz.getMethod("damage");
            } catch (Exception ignored) {
            }
            exileOverlay$methodsInitialized = true;
        }
    }

    @Inject(method = "spawnOnClient", at = @At("HEAD"), cancellable = true)
    private void exileOverlay$cancelDamageParticleSpawn(Entity entity, CallbackInfo ci) {
        try {
            ci.cancel();

            DamagePopupConfig config = DamagePopupConfig.getInstance();
            if (!config.isShowDamage()) {
                return;
            }

            if (entity instanceof Player && !config.isShowPlayerDamage()) {
                return;
            }

            Object self = (Object) this;
            exileOverlay$ensureMethodsInitialized(self.getClass());

            boolean isCrit = false;
            if (exileOverlay$isCritMethod != null) {
                try {
                    Object critResult = exileOverlay$isCritMethod.invoke(self);
                    if (critResult instanceof Boolean b) {
                        isCrit = b;
                    }
                } catch (Exception ignored) {
                }
            }

            Map<?, ?> dmgMap = null;
            if (exileOverlay$getDmgMapMethod != null) {
                try {
                    Object mapObj = exileOverlay$getDmgMapMethod.invoke(self);
                    if (mapObj instanceof Map<?, ?> m) {
                        dmgMap = m;
                    }
                } catch (Exception ignored) {
                }
            }

            // フォールバック: getDmgMap() が失敗した場合、Recordの elements と damage から直接復元
            if (dmgMap == null && exileOverlay$elementsMethod != null && exileOverlay$damageMethod != null) {
                dmgMap = exileOverlay$extractFallbackMap(self);
            }

            if (dmgMap == null || dmgMap.isEmpty()) {
                return;
            }

            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.phys.Vec3 position;
            int entityId;

            if (entity instanceof LivingEntity living) {
                float heightRatio = config.getPopupHeightRatio();
                position = living.position().add(0, living.getBbHeight() * heightRatio, 0);
                entityId = living.getId();
                DamagePopupManager.getInstance().markMsDamageHandled(entityId);
                // if (mc.player == null || entityId != mc.player.getId()) {
                //     KillCountManager.getInstance().recordPlayerAttack(entityId);
                // }
            } else if (entity != null) {
                position = entity.position().add(0, entity.getBbHeight() * 0.8f, 0);
                entityId = entity.getId();
                DamagePopupManager.getInstance().markMsDamageHandled(entityId);
                // if (mc.player == null || entityId != mc.player.getId()) {
                //     KillCountManager.getInstance().recordPlayerAttack(entityId);
                // }
            } else if (mc.player != null) {
                // entity が null の場合のフォールバック: プレイヤーの前方 2.5 ブロック位置に配置
                position = mc.player.getEyePosition().add(mc.player.getLookAngle().scale(2.5));
                entityId = -1;
            } else {
                return;
            }

            for (Map.Entry<?, ?> entry : dmgMap.entrySet()) {
                String elementName = entry.getKey() instanceof Enum<?> e
                        ? e.name()
                        : String.valueOf(entry.getKey());
                float damage = 0f;
                if (entry.getValue() instanceof Number num) {
                    damage = num.floatValue();
                }

                if (damage > 0.01f) {
                    DamageType damageType = exileOverlay$getDamageTypeForElement(elementName);
                    if (damageType == DamageType.POISON && entity instanceof LivingEntity living) {
                        com.example.exile_overlay.client.render.ailment.ClientAilmentTracker.getInstance()
                                .recordPoisonDamage(living);
                    }
                    DamagePopupManager.getInstance().addDamageNumber(
                        position,
                        damage,
                        isCrit,
                        damageType,
                        entityId,
                        net.minecraft.world.phys.Vec3.ZERO
                    );
                    LOGGER.debug("Showing damage popup: {} (crit: {}, element: {}) for entity {}",
                        damage, isCrit, elementName, entityId);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Failed to process damage information: {}", e.getMessage(), e);
        }
    }

    @Unique
    private static final String[] EXILE_ELEMENT_NAMES = {
        "Physical", "Fire", "Cold", "Nature", "Shadow", "Elemental", "ALL"
    };

    @Unique
    private Map<String, Float> exileOverlay$extractFallbackMap(Object self) {
        try {
            Object elementsObj = exileOverlay$elementsMethod.invoke(self);
            Object damageObj = exileOverlay$damageMethod.invoke(self);

            if (elementsObj instanceof byte[] bytes) {
                Map<String, Float> map = new java.util.LinkedHashMap<>();
                if (damageObj instanceof it.unimi.dsi.fastutil.floats.FloatList floatList) {
                    for (int i = 0; i < bytes.length && i < floatList.size(); i++) {
                        int ord = bytes[i] & 0xFF;
                        String name = ord < EXILE_ELEMENT_NAMES.length ? EXILE_ELEMENT_NAMES[ord] : "Physical";
                        map.put(name, floatList.getFloat(i));
                    }
                } else if (damageObj instanceof java.util.List<?> list) {
                    for (int i = 0; i < bytes.length && i < list.size(); i++) {
                        int ord = bytes[i] & 0xFF;
                        String name = ord < EXILE_ELEMENT_NAMES.length ? EXILE_ELEMENT_NAMES[ord] : "Physical";
                        Object val = list.get(i);
                        if (val instanceof Number n) {
                            map.put(name, n.floatValue());
                        }
                    }
                }
                return map;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Unique
    private DamageType exileOverlay$getDamageTypeForElement(String element) {
        if (element == null) {
            return DamageType.PHYSICAL;
        }
        return switch (element.toUpperCase(java.util.Locale.ROOT)) {
            case "FIRE" -> DamageType.FIRE;
            case "COLD", "ICE", "WATER" -> DamageType.ICE;
            case "NATURE", "LIGHTNING" -> DamageType.LIGHTNING;
            case "SHADOW", "DARK", "CHAOS" -> DamageType.MAGIC;
            case "POISON" -> DamageType.POISON;
            case "ELEMENTAL" -> DamageType.ELEMENTAL;
            default -> DamageType.PHYSICAL;
        };
    }
}
