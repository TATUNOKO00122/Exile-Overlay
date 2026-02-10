package com.example.exile_overlay.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Mine and Slash (M&S) モッドとの連携を担当するヘルパークラス。
 * M&Sのデータ（HP、マナ、エネルギー、レベル等）をリフレクションを介して取得します。
 */
public class MineAndSlashHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Boolean mnsLoaded = null;

    // キャッシュ
    private static WeakReference<Player> cachedPlayer = null;
    private static Object cachedEntityData = null;
    private static long cacheTime = 0;
    private static final long CACHE_DURATION_MS = 250;

    /**
     * Mine and Slashがロードされているか確認します。
     */
    private static boolean isMnsLoaded() {
        if (mnsLoaded == null) {
            try {
                Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
                mnsLoaded = true;
                LOGGER.info("Mine and Slash detected, enabling integration.");
            } catch (ClassNotFoundException e) {
                mnsLoaded = false;
                LOGGER.info("Mine and Slash not found, using vanilla fallbacks.");
            }
        }
        return mnsLoaded;
    }

    /**
     * プレイヤーのEntityDataを取得します（内部用）。
     * Mine and Slash 1.20.1 では Load.Unit(player) は EntityData を返します。
     */
    private static Object getCachedEntityData(Player player) {
        if (!isMnsLoaded())
            return null;
        if (player == null)
            return null;

        long now = System.currentTimeMillis();
        Player cached = cachedPlayer != null ? cachedPlayer.get() : null;

        if (cached == player && cachedEntityData != null && (now - cacheTime) < CACHE_DURATION_MS) {
            return cachedEntityData;
        }

        try {
            // Load.Unit(player) を呼び出す
            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var method = loadClass.getMethod("Unit", Player.class);
            cachedEntityData = method.invoke(null, player);
            cachedPlayer = new WeakReference<>(player);
            cacheTime = now;
            return cachedEntityData;
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getResources(Player player) {
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getResources = data.getClass().getMethod("getResources");
                return getResources.invoke(data);
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static float getCurrentMana(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object resources = getResources(player);
        if (resources != null) {
            try {
                var getMana = resources.getClass().getMethod("getMana");
                return ((Number) getMana.invoke(resources)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getMaxMana(Player player) {
        if (!isMnsLoaded())
            return 1;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                Class<?> resourceTypeClass = Class
                        .forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
                var manaType = resourceTypeClass.getField("mana").get(null);
                var getMaximumResource = data.getClass().getMethod("getMaximumResource", resourceTypeClass);
                return ((Number) getMaximumResource.invoke(data, manaType)).floatValue();
            } catch (Exception e) {
            }
        }
        return 1;
    }

    public static float getCurrentMagicShield(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object resources = getResources(player);
        if (resources != null) {
            try {
                var getMagicShield = resources.getClass().getMethod("getMagicShield");
                return ((Number) getMagicShield.invoke(resources)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getMaxMagicShield(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                Class<?> resourceTypeClass = Class
                        .forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
                var shieldType = resourceTypeClass.getField("magic_shield").get(null);
                var getMaximumResource = data.getClass().getMethod("getMaximumResource", resourceTypeClass);
                return ((Number) getMaximumResource.invoke(data, shieldType)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getCurrentEnergy(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object resources = getResources(player);
        if (resources != null) {
            try {
                var getEnergy = resources.getClass().getMethod("getEnergy");
                return ((Number) getEnergy.invoke(resources)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getMaxEnergy(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                Class<?> resourceTypeClass = Class
                        .forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
                var energyType = resourceTypeClass.getField("energy").get(null);
                var getMaximumResource = data.getClass().getMethod("getMaximumResource", resourceTypeClass);
                return ((Number) getMaximumResource.invoke(data, energyType)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getCurrentHealth(Player player) {
        if (!isMnsLoaded())
            return player.getHealth();
        try {
            Class<?> healthUtils = Class.forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");
            var method = healthUtils.getMethod("getCurrentHealth", Player.class);
            return ((Number) method.invoke(null, player)).floatValue();
        } catch (Exception e) {
            return player.getHealth();
        }
    }

    public static float getMaxHealth(Player player) {
        if (!isMnsLoaded())
            return player.getMaxHealth();
        try {
            Class<?> healthUtils = Class.forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");
            var method = healthUtils.getMethod("getMaxHealth", Player.class);
            return ((Number) method.invoke(null, player)).floatValue();
        } catch (Exception e) {
            return player.getMaxHealth();
        }
    }

    public static float getExp(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var method = data.getClass().getMethod("getExp");
                return ((Number) method.invoke(data)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getExpRequiredForLevelUp(Player player) {
        if (!isMnsLoaded())
            return 1;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var method = data.getClass().getMethod("getExpRequiredForLevelUp");
                return ((Number) method.invoke(data)).floatValue();
            } catch (Exception e) {
            }
        }
        return 1;
    }

    public static int getLevel(Player player) {
        if (!isMnsLoaded())
            return 1;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var method = data.getClass().getMethod("getLevel");
                return ((Number) method.invoke(data)).intValue();
            } catch (Exception e) {
            }
        }
        return 1;
    }

    public static boolean isBloodMagicActive(Player player) {
        if (!isMnsLoaded())
            return false;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getUnit = data.getClass().getMethod("getUnit");
                Object unit = getUnit.invoke(data);
                if (unit != null) {
                    var isBloodMage = unit.getClass().getMethod("isBloodMage");
                    return (boolean) isBloodMage.invoke(unit);
                }
            } catch (Exception e) {
            }
        }
        return false;
    }

    public static float getCurrentBlood(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object resources = getResources(player);
        if (resources != null) {
            try {
                var getBlood = resources.getClass().getMethod("getBlood");
                return ((Number) getBlood.invoke(resources)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    public static float getMaxBlood(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                Class<?> resourceTypeClass = Class
                        .forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
                var bloodType = resourceTypeClass.getField("blood").get(null);
                var getMaximumResource = data.getClass().getMethod("getMaximumResource", resourceTypeClass);
                return ((Number) getMaximumResource.invoke(data, bloodType)).floatValue();
            } catch (Exception e) {
            }
        }
        return 0;
    }

    // スキル関連の取得メソッド（将来の拡張用）
    public static Object getHotbarSpell(Player player, int slot) {
        if (!isMnsLoaded())
            return null;
        try {
            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var method = loadClass.getMethod("player", Player.class);
            Object playerData = method.invoke(null, player);
            if (playerData != null) {
                var getSkillGemInventory = playerData.getClass().getMethod("getSkillGemInventory");
                Object inv = getSkillGemInventory.invoke(playerData);
                if (inv != null) {
                    var getHotbarGem = inv.getClass().getMethod("getHotbarGem", int.class);
                    Object gem = getHotbarGem.invoke(inv, slot);
                    if (gem != null) {
                        return gem.getClass().getMethod("getSpell").invoke(gem);
                    }
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    // ========== Exile Effect (Buff/Debuff) Data Classes ==========
    
    /**
     * ExileEffectの情報を保持するデータクラス
     */
    public static class ExileEffectInfo {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        public final int duration; // in ticks
        public final int stacks;
        public final boolean isBeneficial;
        public final boolean isNegative;
        public final boolean isInfinite;
        public final String durationText;

        public ExileEffectInfo(String id, String name, ResourceLocation texture, int duration, int stacks,
                              boolean isBeneficial, boolean isNegative, boolean isInfinite, String durationText) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.duration = duration;
            this.stacks = stacks;
            this.isBeneficial = isBeneficial;
            this.isNegative = isNegative;
            this.isInfinite = isInfinite;
            this.durationText = durationText;
        }
    }

    // ========== Exile Effect API ==========

    /**
     * Get all active ExileEffects (Mine and Slash buffs/debuffs) on the player.
     */
    public static List<ExileEffectInfo> getExileEffects(Player player) {
        List<ExileEffectInfo> result = new ArrayList<>();
        if (!isMnsLoaded()) return result;

        Object data = getCachedEntityData(player);
        if (data == null) return result;

        try {
            var getStatusEffectsData = data.getClass().getMethod("getStatusEffectsData");
            Object statusData = getStatusEffectsData.invoke(data);
            if (statusData == null) return result;

            var getEffects = statusData.getClass().getMethod("getEffects");
            List<?> effects = (List<?>) getEffects.invoke(statusData);
            if (effects == null || effects.isEmpty()) return result;

            for (Object effect : effects) {
                if (effect == null) continue;

                var getId = effect.getClass().getMethod("GUID");
                String id = (String) getId.invoke(effect);

                var getTexture = effect.getClass().getMethod("getTexture");
                ResourceLocation texture = (ResourceLocation) getTexture.invoke(effect);

                var getType = effect.getClass().getMethod("getEffectType");
                Object effectType = getType.invoke(effect);

                boolean isBeneficial = false;
                boolean isNegative = false;
                if (effectType != null) {
                    String typeName = effectType.toString();
                    isBeneficial = typeName.contains("beneficial");
                    isNegative = typeName.contains("negative");
                }

                var getLocName = effect.getClass().getMethod("locName");
                Object nameComponent = getLocName.invoke(effect);
                String name = nameComponent != null ? nameComponent.toString() : id;

                // Get instance data for duration and stacks
                var getInstanceData = statusData.getClass().getMethod("get", effect.getClass());
                Object instanceData = getInstanceData.invoke(statusData, effect);

                int duration = 0;
                int stacks = 1;
                boolean isInfinite = false;

                if (instanceData != null) {
                    try {
                        var getDuration = instanceData.getClass().getMethod("getRemainingDurationInSeconds");
                        duration = ((Number) getDuration.invoke(instanceData)).intValue() * 20;
                        
                        var getStacks = instanceData.getClass().getMethod("getStacks");
                        stacks = ((Number) getStacks.invoke(instanceData)).intValue();
                        
                        isInfinite = duration <= 0 || duration > 1000000;
                    } catch (Exception e) {
                        // Duration method might not exist or return differently
                    }
                }

                String durationText = formatDuration(duration / 20);

                result.add(new ExileEffectInfo(id, name, texture, duration, stacks, 
                    isBeneficial, isNegative, isInfinite, durationText));
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ExileEffects", e);
        }

        return result;
    }

    /**
     * Get only beneficial ExileEffects (buffs)
     */
    public static List<ExileEffectInfo> getExileBuffs(Player player) {
        List<ExileEffectInfo> all = getExileEffects(player);
        List<ExileEffectInfo> buffs = new ArrayList<>();
        for (ExileEffectInfo info : all) {
            if (info.isBeneficial) {
                buffs.add(info);
            }
        }
        return buffs;
    }

    /**
     * Get only negative ExileEffects (debuffs)
     */
    public static List<ExileEffectInfo> getExileDebuffs(Player player) {
        List<ExileEffectInfo> all = getExileEffects(player);
        List<ExileEffectInfo> debuffs = new ArrayList<>();
        for (ExileEffectInfo info : all) {
            if (info.isNegative) {
                debuffs.add(info);
            }
        }
        return debuffs;
    }

    private static String formatDuration(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        } else if (seconds >= 60) {
            return (seconds / 60) + "m";
        } else if (seconds <= 0) {
            return "**";
        } else {
            return seconds + "s";
        }
    }
}
