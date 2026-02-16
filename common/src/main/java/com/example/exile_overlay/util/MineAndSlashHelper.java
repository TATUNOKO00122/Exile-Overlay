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

    // ========== Skill Hotbar API ==========

    /**
     * Get spell at hotbar slot (0-7)
     */
    public static Object getHotbarSpell(Player player, int slot) {
        if (!isMnsLoaded()) return null;
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
            LOGGER.debug("Failed to get hotbar spell at slot {}: {}", slot, e.getMessage());
        }
        return null;
    }

    /**
     * Get spell icon ResourceLocation
     */
    public static ResourceLocation getSpellIcon(Player player, int slot) {
        if (!isMnsLoaded()) return null;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell != null) {
                var getIconLoc = spell.getClass().getMethod("getIconLoc");
                return (ResourceLocation) getIconLoc.invoke(spell);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell icon at slot {}: {}", slot, e.getMessage());
        }
        return null;
    }

    /**
     * Get cooldown percentage (0.0 - 1.0, where 1.0 = fully on cooldown)
     */
    public static float getSpellCooldownPercent(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell != null) {
                var getGUID = spell.getClass().getMethod("GUID");
                String guid = (String) getGUID.invoke(spell);
                
                Object data = getCachedEntityData(player);
                if (data != null) {
                    var getCooldowns = data.getClass().getMethod("getCooldowns");
                    Object cds = getCooldowns.invoke(data);
                    if (cds != null) {
                        var getCurrentTicks = cds.getClass().getMethod("getCooldownTicks", String.class);
                        var getNeededTicks = cds.getClass().getMethod("getNeededTicks", String.class);
                        int current = (int) getCurrentTicks.invoke(cds, guid);
                        int needed = (int) getNeededTicks.invoke(cds, guid);
                        if (needed > 0) {
                            return Math.min((float) current / needed, 1.0f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell cooldown at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    /**
     * Get remaining cooldown in seconds
     */
    public static int getSpellCooldownSeconds(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell != null) {
                var getGUID = spell.getClass().getMethod("GUID");
                String guid = (String) getGUID.invoke(spell);
                
                Object data = getCachedEntityData(player);
                if (data != null) {
                    var getCooldowns = data.getClass().getMethod("getCooldowns");
                    Object cds = getCooldowns.invoke(data);
                    if (cds != null) {
                        var getCurrentTicks = cds.getClass().getMethod("getCooldownTicks", String.class);
                        int ticks = (int) getCurrentTicks.invoke(cds, guid);
                        return ticks / 20;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell cooldown seconds at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    /**
     * Get mana cost for a spell
     */
    public static int getSpellManaCost(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell != null) {
                // Try getManaCost method directly
                try {
                    var method = spell.getClass().getMethod("getManaCost");
                    Object result = method.invoke(spell);
                    if (result instanceof Number) {
                        return ((Number) result).intValue();
                    }
                } catch (NoSuchMethodException e) {
                    // Try config.cast_action.getManaCost()
                    try {
                        var configField = spell.getClass().getField("config");
                        Object config = configField.get(spell);
                        if (config != null) {
                            var castActionField = config.getClass().getField("cast_action");
                            Object castAction = castActionField.get(config);
                            if (castAction != null) {
                                var manaCostMethod = castAction.getClass().getMethod("getManaCost");
                                Object result = manaCostMethod.invoke(castAction);
                                if (result instanceof Number) {
                                    return ((Number) result).intValue();
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // Ignore
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell mana cost at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    private static net.minecraft.client.KeyMapping[] cachedSpellKeys = null;

    /**
     * Get the key name for a spell slot (0-7)
     */
    public static String getSpellKeyText(int slot) {
        if (cachedSpellKeys == null) {
            findSpellKeys();
        }

        if (cachedSpellKeys != null && slot >= 0 && slot < cachedSpellKeys.length) {
            net.minecraft.client.KeyMapping key = cachedSpellKeys[slot];
            if (key != null) {
                return key.getTranslatedKeyMessage().getString().toUpperCase();
            }
        }

        return String.valueOf(slot + 1);
    }

    private static void findSpellKeys() {
        try {
            var options = net.minecraft.client.Minecraft.getInstance().options;
            var allKeys = options.keyMappings;

            net.minecraft.client.KeyMapping[] foundKeys = new net.minecraft.client.KeyMapping[8];

            for (var key : allKeys) {
                String name = key.getName().toLowerCase();
                String category = key.getCategory().toLowerCase();

                for (int i = 1; i <= 8; i++) {
                    String pattern = "spell_" + i;

                    if (name.contains(pattern)) {
                        boolean isMmorpg = name.startsWith("mmorpg") || category.contains("mmorpg")
                                || category.contains("slash");

                        if (foundKeys[i - 1] == null || isMmorpg) {
                            foundKeys[i - 1] = key;
                        }
                    }
                }
            }

            boolean anyFound = false;
            for (var k : foundKeys) if (k != null) anyFound = true;

            if (anyFound) {
                cachedSpellKeys = foundKeys;
            }
        } catch (Exception e) {
            LOGGER.warn("Error finding spell keys", e);
        }
    }

    /**
     * Check if Mine and Slash is loaded
     */
    public static boolean isLoaded() {
        return isMnsLoaded();
    }
}
