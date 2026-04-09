package com.example.exile_overlay.util;

import com.example.exile_overlay.api.MethodHandlesUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
            // Load.Unit(Entity) を呼び出す - Player is a subclass of Entity
            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var method = loadClass.getMethod("Unit", Entity.class);
            cachedEntityData = method.invoke(null, player);
            cachedPlayer = new WeakReference<>(player);
            cacheTime = now;
            return cachedEntityData;
        } catch (Exception e) {
            LOGGER.info("[CD Debug] getCachedEntityData failed: {}", e.getMessage());
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

    public static float getEntityHealth(LivingEntity entity) {
        if (!isMnsLoaded())
            return entity.getHealth();
        try {
            return MethodHandlesUtil.getCurrentHealth(entity);
        } catch (Throwable e) {
            return entity.getHealth();
        }
    }

    public static float getEntityMaxHealth(LivingEntity entity) {
        if (!isMnsLoaded())
            return entity.getMaxHealth();
        try {
            return MethodHandlesUtil.getMaxHealth(entity);
        } catch (Throwable e) {
            return entity.getMaxHealth();
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
     * Uses exileMap directly to access ticks_left, stacks, and is_infinite fields.
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

            Field exileMapField = statusData.getClass().getField("exileMap");
            ConcurrentHashMap<String, Object> exileMap = (ConcurrentHashMap<String, Object>) exileMapField.get(statusData);
            if (exileMap == null || exileMap.isEmpty()) return result;

            Class<?> exileDBClass = Class.forName("com.robertx22.mine_and_slash.database.registry.ExileDB");
            Method getExileEffectsMethod = exileDBClass.getMethod("ExileEffects");
            Object registry = getExileEffectsMethod.invoke(null);
            Method getMethod = registry.getClass().getMethod("get", String.class);

            for (Map.Entry<String, Object> entry : exileMap.entrySet()) {
                String effectId = entry.getKey();
                Object instanceData = entry.getValue();

                if (instanceData == null) continue;

                try {
                    Method shouldRemoveMethod = instanceData.getClass().getMethod("shouldRemove");
                    boolean shouldRemove = (boolean) shouldRemoveMethod.invoke(instanceData);
                    if (shouldRemove) continue;

                    Field ticksLeftField = instanceData.getClass().getField("ticks_left");
                    int ticksLeft = ticksLeftField.getInt(instanceData);

                    Field stacksField = instanceData.getClass().getField("stacks");
                    int stacks = stacksField.getInt(instanceData);

                    Field isInfiniteField = instanceData.getClass().getField("is_infinite");
                    boolean isInfinite = isInfiniteField.getBoolean(instanceData);

                    Method getDurationStringMethod = instanceData.getClass().getMethod("getDurationString");
                    String durationText = (String) getDurationStringMethod.invoke(instanceData);

                    Object effect = getMethod.invoke(registry, effectId);
                    if (effect == null) continue;

                    Method getTextureMethod = effect.getClass().getMethod("getTexture");
                    ResourceLocation texture = (ResourceLocation) getTextureMethod.invoke(effect);

                    Method locNameMethod = effect.getClass().getMethod("locName");
                    Object nameComponent = locNameMethod.invoke(effect);
                    String name = nameComponent != null ? nameComponent.toString() : effectId;

                    Field typeField = effect.getClass().getField("type");
                    Object effectType = typeField.get(effect);

                    boolean isBeneficial = true;
                    boolean isNegative = false;
                    if (effectType != null) {
                        String typeName = effectType.toString();
                        isNegative = typeName.contains("negative");
                        isBeneficial = !isNegative;
                    }

                    result.add(new ExileEffectInfo(effectId, name, texture, ticksLeft, stacks,
                        isBeneficial, isNegative, isInfinite, durationText));
                } catch (Exception inner) {
                    LOGGER.debug("Failed to process effect {}: {}", effectId, inner.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get ExileEffects", e);
        }

        return result;
    }

    /**
     * Get all ExileEffects (unified - buffs, debuffs, neutral)
     * Returns all effects regardless of type since display is not split by beneficial/negative.
     */
    public static List<ExileEffectInfo> getExileBuffs(Player player) {
        return getExileEffects(player);
    }

    /**
     * Returns empty list - M&S effects are all returned via getExileBuffs() to avoid duplicates.
     */
    public static List<ExileEffectInfo> getExileDebuffs(Player player) {
        return new ArrayList<>();
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

    private static long lastCooldownLogTime = 0;
    private static final long COOLDOWN_LOG_INTERVAL_MS = 2000;
    private static int lastLoggedSlot = -1;
    private static int lastLoggedCurrent = -1;

    /**
     * Get cooldown percentage (0.0 - 1.0, where 1.0 = fully on cooldown)
     */
    public static float getSpellCooldownPercent(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) {
                logCooldownDebug(slot, "spell=null", 0, 0);
                return 0;
            }

            var getGUID = spell.getClass().getMethod("GUID");
            String guid = (String) getGUID.invoke(spell);
            if (guid == null || guid.isEmpty()) {
                logCooldownDebug(slot, "guid=null", 0, 0);
                return 0;
            }

            Object data = getCachedEntityData(player);
            if (data == null) {
                logCooldownDebug(slot, "data=null", 0, 0);
                return 0;
            }

            var getCooldowns = data.getClass().getMethod("getCooldowns");
            Object cds = getCooldowns.invoke(data);
            if (cds == null) {
                logCooldownDebug(slot, "cds=null", 0, 0);
                return 0;
            }

            var getCurrentTicks = cds.getClass().getMethod("getCooldownTicks", String.class);
            var getNeededTicks = cds.getClass().getMethod("getNeededTicks", String.class);
            int current = (int) getCurrentTicks.invoke(cds, guid);
            int needed = (int) getNeededTicks.invoke(cds, guid);

            logCooldownDebug(slot, guid, current, needed);

            if (needed > 0) {
                return Math.min((float) current / needed, 1.0f);
            }
        } catch (Exception e) {
            logCooldownDebug(slot, "Exception: " + e.getMessage(), 0, 0);
        }
        return 0;
    }

    private static void logCooldownDebug(int slot, String guid, int current, int needed) {
        long now = System.currentTimeMillis();
        boolean shouldLog = false;

        if ((now - lastCooldownLogTime) > COOLDOWN_LOG_INTERVAL_MS) {
            shouldLog = true;
        } else if (slot != lastLoggedSlot || current != lastLoggedCurrent) {
            if (current > 0 || lastLoggedCurrent > 0) {
                shouldLog = true;
            }
        }

        if (shouldLog) {
            LOGGER.info("[CD Debug] Slot {} GUID={} current={} needed={}",
                    slot, guid, current, needed);
            lastCooldownLogTime = now;
            lastLoggedSlot = slot;
            lastLoggedCurrent = current;
        }
    }

    /**
     * Get remaining cooldown in seconds
     */
    public static int getSpellCooldownSeconds(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) {
                return 0;
            }

            var getGUID = spell.getClass().getMethod("GUID");
            String guid = (String) getGUID.invoke(spell);
            if (guid == null || guid.isEmpty()) {
                return 0;
            }

            Object data = getCachedEntityData(player);
            if (data == null) {
                return 0;
            }

            var getCooldowns = data.getClass().getMethod("getCooldowns");
            Object cds = getCooldowns.invoke(data);
            if (cds == null) {
                return 0;
            }

            var getCurrentTicks = cds.getClass().getMethod("getCooldownTicks", String.class);
            int ticks = (int) getCurrentTicks.invoke(cds, guid);
            return ticks / 20;
        } catch (Exception e) {
            // 例外はgetSpellCooldownPercentでログ出力済み
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

    private static boolean hotbarSwappingEnabled = false;
    private static long hotbarSwapCheckTime = 0;
    private static final long HOTBAR_SWAP_CHECK_INTERVAL_MS = 1000;
    private static Field isOnSecondHotbarField = null;

    public static boolean isHotbarSwappingEnabled() {
        if (!isMnsLoaded()) return false;

        long now = System.currentTimeMillis();
        if (now - hotbarSwapCheckTime < HOTBAR_SWAP_CHECK_INTERVAL_MS) {
            return hotbarSwappingEnabled;
        }
        hotbarSwapCheckTime = now;

        try {
            Class<?> clientConfigsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
            java.lang.reflect.Method getConfig = clientConfigsClass.getMethod("getConfig");
            Object config = getConfig.invoke(null);
            Field swappingField = config.getClass().getField("HOTBAR_SWAPPING");
            Object booleanValue = swappingField.get(config);
            java.lang.reflect.Method getMethod = booleanValue.getClass().getMethod("get");
            hotbarSwappingEnabled = (Boolean) getMethod.invoke(booleanValue);
        } catch (Exception e) {
            hotbarSwappingEnabled = false;
        }
        return hotbarSwappingEnabled;
    }

    public static boolean isOnSecondHotbar() {
        if (!isMnsLoaded()) return false;
        try {
            if (isOnSecondHotbarField == null) {
                Class<?> spellKeybindClass = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
                isOnSecondHotbarField = spellKeybindClass.getField("IS_ON_SECONd_HOTBAR");
            }
            return isOnSecondHotbarField.getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isUnbound(net.minecraft.client.KeyMapping key) {
        if (key == null) return true;
        var boundKey = key.getKey();
        return boundKey.getType() == com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM
                && boundKey.getValue() == -1;
    }

    /**
     * Get the key name for a spell slot (0-7)
     * HOTBAR_SWAPPING有効時はアクティブなホットバーのキーのみ表示
     * 未割り当てキーの場合は空文字を返す
     */
    public static String getSpellKeyText(int slot) {
        if (cachedSpellKeys == null) {
            findSpellKeys();
        }

        if (cachedSpellKeys != null && slot >= 0 && slot < cachedSpellKeys.length) {
            net.minecraft.client.KeyMapping key = cachedSpellKeys[slot];

            if (isHotbarSwappingEnabled()) {
                boolean onSecond = isOnSecondHotbar();
                boolean isFirstHalf = slot < 4;

                if (onSecond && isFirstHalf) {
                    return "";
                }
                if (!onSecond && !isFirstHalf) {
                    return "";
                }

                int keySlot = onSecond ? slot - 4 : slot;
                if (keySlot >= 0 && keySlot < cachedSpellKeys.length) {
                    key = cachedSpellKeys[keySlot];
                }
            }

            if (isUnbound(key)) {
                return "";
            }

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

    // ========== Summon Count API ==========

    /**
     * スキルスロットの召喚数を取得
     * SummonedData.getSummonedAmount(spellGUID) を呼び出し
     */
    public static int getSummonCount(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) return 0;

            var getGUID = spell.getClass().getMethod("GUID");
            String guid = (String) getGUID.invoke(spell);
            if (guid == null || guid.isEmpty()) return 0;

            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var playerMethod = loadClass.getMethod("player", Player.class);
            Object playerData = playerMethod.invoke(null, player);
            if (playerData == null) return 0;

            var getSummonedData = playerData.getClass().getMethod("getSummonedData");
            Object summonedData = getSummonedData.invoke(playerData);
            if (summonedData == null) return 0;

            var getSummonedAmount = summonedData.getClass().getMethod("getSummonedAmount", String.class);
            return (int) getSummonedAmount.invoke(summonedData, guid);
        } catch (Exception e) {
            LOGGER.debug("Failed to get summon count at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    // ========== Spell Charge API ==========

    public static boolean getSpellUsesCharges(Player player, int slot) {
        if (!isMnsLoaded()) return false;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) return false;
            var configField = spell.getClass().getField("config");
            Object config = configField.get(spell);
            if (config == null) return false;
            var chargesField = config.getClass().getField("charges");
            return chargesField.getInt(config) > 0;
        } catch (Exception e) {
            LOGGER.debug("Failed to check spell uses charges at slot {}: {}", slot, e.getMessage());
        }
        return false;
    }

    public static int getSpellCharges(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) return 0;
            var configField = spell.getClass().getField("config");
            Object config = configField.get(spell);
            if (config == null) return 0;
            var chargeNameField = config.getClass().getField("charge_name");
            String chargeName = (String) chargeNameField.get(config);
            if (chargeName == null || chargeName.isEmpty()) return 0;

            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var playerMethod = loadClass.getMethod("player", Player.class);
            Object playerData = playerMethod.invoke(null, player);
            if (playerData == null) return 0;

            var spellCastingDataField = playerData.getClass().getField("spellCastingData");
            Object spellCastingData = spellCastingDataField.get(playerData);
            if (spellCastingData == null) return 0;

            var chargesField = spellCastingData.getClass().getField("charges");
            Object chargeData = chargesField.get(spellCastingData);
            if (chargeData == null) return 0;

            var getCharges = chargeData.getClass().getMethod("getCharges", String.class);
            return (int) getCharges.invoke(chargeData, chargeName);
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell charges at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    public static int getSpellMaxCharges(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) return 0;
            var configField = spell.getClass().getField("config");
            Object config = configField.get(spell);
            if (config == null) return 0;
            var chargesField = config.getClass().getField("charges");
            return chargesField.getInt(config);
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell max charges at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    public static float getSpellChargeRegenPercent(Player player, int slot) {
        if (!isMnsLoaded()) return 0;
        try {
            var spell = getHotbarSpell(player, slot);
            if (spell == null) return 0;
            var configField = spell.getClass().getField("config");
            Object config = configField.get(spell);
            if (config == null) return 0;
            var chargeNameField = config.getClass().getField("charge_name");
            String chargeName = (String) chargeNameField.get(config);
            var chargeRegenField = config.getClass().getField("charge_regen");
            int chargeRegen = chargeRegenField.getInt(config);
            if (chargeName == null || chargeName.isEmpty() || chargeRegen <= 0) return 0;

            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var playerMethod = loadClass.getMethod("player", Player.class);
            Object playerData = playerMethod.invoke(null, player);
            if (playerData == null) return 0;

            var spellCastingDataField = playerData.getClass().getField("spellCastingData");
            Object spellCastingData = spellCastingDataField.get(playerData);
            if (spellCastingData == null) return 0;

            var chargesField = spellCastingData.getClass().getField("charges");
            Object chargeData = chargesField.get(spellCastingData);
            if (chargeData == null) return 0;

            var getCurrentTicksChargingOf = chargeData.getClass().getMethod("getCurrentTicksChargingOf", String.class);
            int currentTicks = (int) getCurrentTicksChargingOf.invoke(chargeData, chargeName);

            float remaining = chargeRegen - currentTicks;
            return Math.max(0, Math.min(remaining / chargeRegen, 1.0f));
        } catch (Exception e) {
            LOGGER.debug("Failed to get spell charge regen percent at slot {}: {}", slot, e.getMessage());
        }
        return 0;
    }

    // ========== Entity Level API (Mob/Player) ==========

    private static Boolean entityDataHasGetLevel = null;

    /**
     * LivingEntity（Mob/Player）のMine-And-Slashレベルを取得
     * Mine-And-Slash-Rework MODの Load.Unit(entity).getLevel() を呼び出し
     * 
     * @param entity 対象エンティティ（Mob or Player）
     * @return レベル（MOD未導入時、または取得失敗時は -1）
     */
    public static int getEntityLevel(net.minecraft.world.entity.LivingEntity entity) {
        if (!isMnsLoaded() || entity == null) {
            return -1;
        }

        try {
            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            var unitMethod = loadClass.getMethod("Unit", net.minecraft.world.entity.Entity.class);
            Object entityData = unitMethod.invoke(null, entity);

            if (entityData != null) {
                var getLevelMethod = entityData.getClass().getMethod("getLevel");
                return ((Number) getLevelMethod.invoke(entityData)).intValue();
            }
        } catch (Exception e) {
            if (entityDataHasGetLevel == null) {
                LOGGER.debug("Failed to get entity level from Mine-And-Slash: {}", e.getMessage());
                entityDataHasGetLevel = false;
            }
        }
        return -1;
    }

/**
     * Mine and SlashのNeat HPバーの有効/無効を設定
     * 
     * @param enabled true: HPバーを表示, false: HPバーを非表示
     * @return 定定が成功した場合true
     */
    public static boolean setNeatHpBarEnabled(boolean enabled) {
        try {
            Class<?> neatConfigClass = Class.forName("com.robertx22.mine_and_slash.a_libraries.neat.NeatConfig");
            java.lang.reflect.Field drawField = neatConfigClass.getField("draw");
            drawField.setBoolean(null, enabled);
            LOGGER.info("Set Mine and Slash Neat HP Bar to: {}", enabled);
            return true;
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Mine and Slash not found, skipping Neat HP Bar setting.");
            return false;
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.error("Failed to set Mine and Slash Neat HP Bar: {}", e.getMessage());
            return false;
        }
    }

    // ========== Potion Detection API ==========

    private static Class<?> slashPotionItemClass = null;
    private static Class<?> potionTypeEnumClass = null;
    private static Method getPotionTypeMethod = null;
    private static Method getRarityMethod = null;
    private static Object potionTypeHp = null;
    private static Object potionTypeMana = null;
    private static Boolean potionClassesInitialized = null;

    private static void initializePotionClasses() {
        if (potionClassesInitialized != null) return;
        potionClassesInitialized = false;
        
        if (!isMnsLoaded()) return;

        try {
            slashPotionItemClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem");
            potionTypeEnumClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem$Type");
            
            getPotionTypeMethod = slashPotionItemClass.getMethod("getType");
            getRarityMethod = slashPotionItemClass.getMethod("getRarity");
            
            potionTypeHp = potionTypeEnumClass.getField("HP").get(null);
            potionTypeMana = potionTypeEnumClass.getField("MANA").get(null);
            
            potionClassesInitialized = true;
            LOGGER.debug("Potion classes initialized successfully");
        } catch (Exception e) {
            LOGGER.debug("Failed to initialize potion classes: {}", e.getMessage());
        }
    }

    /**
     * ItemStackがSlashPotionItemかどうか判定
     */
    public static boolean isSlashPotionItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        initializePotionClasses();
        if (!potionClassesInitialized) return false;
        return slashPotionItemClass.isInstance(stack.getItem());
    }

    /**
     * ポーションタイプ取得（HP=true, MANA=false）
     * @return HPポーションならtrue、MANAポーションならfalse、判定不可ならfalse
     */
    public static boolean isHpPotion(net.minecraft.world.item.ItemStack stack) {
        if (!isSlashPotionItem(stack)) return false;
        try {
            Object type = getPotionTypeMethod.invoke(stack.getItem());
            return type == potionTypeHp;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * ポーションのレアリティ優先度取得（高い数値=高レアリティ）
     * @return レアリティ優先度（0-5）、取得失敗時は0
     */
    public static float getPotionRarityPriority(net.minecraft.world.item.ItemStack stack) {
        if (!isSlashPotionItem(stack)) return 0;
        try {
            Object rarity = getRarityMethod.invoke(stack.getItem());
            if (rarity != null) {
                Field statPercentsField = rarity.getClass().getField("stat_percents");
                Object statPercents = statPercentsField.get(rarity);
                if (statPercents != null) {
                    Field maxField = statPercents.getClass().getField("max");
                    return ((Number) maxField.get(statPercents)).floatValue();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get potion rarity: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * インベントリ内の最高レアリティポーション検索
     * @param player プレイヤー
     * @param isHp HPポーション検索=true、MANA=false
     * @return 最高レアリティのItemStack、見つからない場合はempty
     */
    public static net.minecraft.world.item.ItemStack findBestPotion(Player player, boolean isHp) {
        if (!isMnsLoaded() || player == null) return net.minecraft.world.item.ItemStack.EMPTY;
        
        net.minecraft.world.item.ItemStack bestStack = net.minecraft.world.item.ItemStack.EMPTY;
        float bestPriority = -1;
        
        for (net.minecraft.world.inventory.Slot slot : player.inventoryMenu.slots) {
            net.minecraft.world.item.ItemStack stack = slot.getItem();
            if (isSlashPotionItem(stack) && isHpPotion(stack) == isHp) {
                float priority = getPotionRarityPriority(stack);
                if (priority > bestPriority) {
                    bestPriority = priority;
                    bestStack = stack.copy();
                }
            }
        }
        
        return bestStack;
    }

/**
     * ポーションがクールダウン中かどうか判定
     * バニラのCooldownTrackerを使用
     */
    public static boolean isPotionOnCooldown(Player player, net.minecraft.world.item.ItemStack stack) {
        if (player == null || stack.isEmpty()) return false;
        return player.getCooldowns().isOnCooldown(stack.getItem());
    }

    // ========== Mob Info API ==========

    public static class MobRarityInfo {
        public final String id;
        public final int color;
        public final boolean isElite;
        public final boolean isSpecial;

        public MobRarityInfo(String id, int color, boolean isElite, boolean isSpecial) {
            this.id = id;
            this.color = color;
            this.isElite = isElite;
            this.isSpecial = isSpecial;
        }
    }

    public static class MobAffixInfo {
        public final String name;
        public final String icon;

        public MobAffixInfo(String name, String icon) {
            this.name = name;
            this.icon = icon != null ? icon : "";
        }
    }

    public static class MobEffectInfo {
        public final String id;
        public final String name;
        public final ResourceLocation texture;
        public final int stacks;
        public final boolean isInfinite;
        public final boolean isNegative;
        private final int displayTicksLeft;

        public MobEffectInfo(String id, String name, ResourceLocation texture, int ticksLeft,
                             int stacks, boolean isInfinite, boolean isNegative) {
            this.id = id;
            this.name = name;
            this.texture = texture;
            this.stacks = stacks;
            this.isInfinite = isInfinite;
            this.isNegative = isNegative;
            this.displayTicksLeft = ticksLeft;
        }

        public String getDurationText() {
            if (isInfinite) return "";
            int seconds = Math.max(0, displayTicksLeft / 20);
            if (seconds <= 0) return "0s";
            if (seconds >= 60) return (seconds / 60) + "m";
            return seconds + "s";
        }

        public boolean isExpired() {
            return !isInfinite && displayTicksLeft <= 0;
        }
    }

    private static final ConcurrentHashMap<String, long[]> effectTimerCache = new ConcurrentHashMap<>();
    private static long lastEffectCleanup = 0;

    private static int calcDisplayTicks(String cacheKey, int syncedTicksLeft) {
        long now = System.currentTimeMillis();
        long[] entry = effectTimerCache.computeIfAbsent(cacheKey, k -> new long[]{syncedTicksLeft, now});
        if (entry[0] != syncedTicksLeft) {
            entry[0] = syncedTicksLeft;
            entry[1] = now;
        }
        long elapsedTicks = (now - entry[1]) / 50;
        return Math.max(0, (int) (syncedTicksLeft - elapsedTicks));
    }

    private static void cleanupEffectTimers() {
        long now = System.currentTimeMillis();
        if (now - lastEffectCleanup < 30000) return;
        lastEffectCleanup = now;
        effectTimerCache.entrySet().removeIf(e -> {
            long elapsedTicks = (now - e.getValue()[1]) / 50;
            return e.getValue()[0] - elapsedTicks <= 0;
        });
    }

    public static MobRarityInfo getMobRarity(net.minecraft.world.entity.LivingEntity entity) {
        if (!MethodHandlesUtil.isAvailable() || entity == null) return null;
        try {
            Object entityData = MethodHandlesUtil.getEntityData(entity);
            if (entityData == null) return null;
            Object mobRarity = MethodHandlesUtil.getMobRarityObj(entityData);
            if (mobRarity == null) return null;
            String rarityId = MethodHandlesUtil.getRarityString(entityData);
            int color = MethodHandlesUtil.getRarityColor(mobRarity);
            boolean elite = MethodHandlesUtil.isRarityElite(mobRarity);
            boolean special = MethodHandlesUtil.isRaritySpecial(mobRarity);
            return new MobRarityInfo(rarityId != null ? rarityId : "common", color, elite, special);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob rarity: {}", t.getMessage());
            return null;
        }
    }

    public static List<MobAffixInfo> getMobAffixes(net.minecraft.world.entity.LivingEntity entity) {
        List<MobAffixInfo> result = new ArrayList<>();
        if (!MethodHandlesUtil.isAvailable() || entity == null) return result;
        try {
            Object entityData = MethodHandlesUtil.getEntityData(entity);
            if (entityData == null) return result;
            List<Object> affixObjs = MethodHandlesUtil.getMobAffixObjects(entityData);
            for (Object affix : affixObjs) {
                String locName = MethodHandlesUtil.getAffixLocName(affix);
                String icon = MethodHandlesUtil.getAffixIcon(affix);
                result.add(new MobAffixInfo(locName, icon));
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob affixes: {}", t.getMessage());
        }
        return result;
    }

    public static List<MobEffectInfo> getMobStatusEffects(net.minecraft.world.entity.LivingEntity entity) {
        List<MobEffectInfo> result = new ArrayList<>();
        if (!MethodHandlesUtil.isAvailable() || entity == null) return result;

        cleanupEffectTimers();

        try {
            Object entityData = MethodHandlesUtil.getEntityData(entity);
            if (entityData == null) return result;
            Object statusData = MethodHandlesUtil.getStatusEffectsData(entityData);
            if (statusData == null) return result;

            Map<String, Object> exileMap = MethodHandlesUtil.getExileEffectMap(statusData);

            for (Map.Entry<String, Object> entry : exileMap.entrySet()) {
                String effectId = entry.getKey();
                Object instanceData = entry.getValue();
                if (instanceData == null) continue;
                if (MethodHandlesUtil.shouldEffectRemove(instanceData)) continue;

                try {
                    int ticksLeft = MethodHandlesUtil.getEffectTicksLeft(instanceData);
                    int stacks = MethodHandlesUtil.getEffectStacks(instanceData);
                    boolean isInfinite = MethodHandlesUtil.isEffectInfinite(instanceData);

                    Object exileEffect = MethodHandlesUtil.getExileEffectFromDB(effectId);
                    if (exileEffect == null) continue;

                    String name = MethodHandlesUtil.getExileEffectName(exileEffect);
                    ResourceLocation texture = MethodHandlesUtil.getExileEffectTexture(exileEffect);
                    boolean isNegative = MethodHandlesUtil.isEffectNegative(exileEffect);

                    String cacheKey = entity.getUUID() + ":" + effectId;
                    int displayTicks = isInfinite ? ticksLeft : calcDisplayTicks(cacheKey, ticksLeft);

                    result.add(new MobEffectInfo(effectId, name, texture, displayTicks,
                            stacks, isInfinite, isNegative));
                } catch (Exception inner) {
                    LOGGER.debug("Failed to process mob effect {}: {}", effectId, inner.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob status effects: {}", t.getMessage());
        }
        return result;
    }


}
