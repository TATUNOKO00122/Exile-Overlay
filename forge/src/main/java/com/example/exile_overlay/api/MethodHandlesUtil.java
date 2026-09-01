package com.example.exile_overlay.api;

import com.example.exile_overlay.api.data.MercenaryDisplayInfo;
import com.example.exile_overlay.api.data.MinionDisplayInfo;
import com.example.exile_overlay.client.render.minion.MercenaryClientCache;
import com.example.exile_overlay.dmgtracker.network.MercenarySyncS2C;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * MethodHandlesを使用した高速リフレクションアクセスユーティリティ。
 * MethodHandleは通常のリフレクションより10-100倍高速でJIT最適化も効く。
 * 一度初期化したら再利用可能で、不変なので複数スレッドから安全に呼び出し可能。
 */
public class MethodHandlesUtil {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MethodHandles.Lookup PUBLIC_LOOKUP = MethodHandles.publicLookup();
    
    // 利用可能性フラグ
    private static boolean available = false;
    
    // === M&Sクラス ===
    private static Class<?> loadClass = null;
    private static Class<?> entityDataClass = null;
    private static Class<?> resourcesDataClass = null;
    private static Class<?> resourceTypeClass = null;
    private static Class<?> unitClass = null;
    private static Class<?> healthUtilsClass = null;
    private static Class<?> elementsClass = null;
    private static Class<?> damageEventClass = null;
    private static Class<?> mobRarityClass = null;
    private static Class<?> mobDataClass = null;
    private static Class<?> mobAffixClass = null;
    private static Class<?> exileDBClass = null;
    private static Class<?> exileEffectClass = null;
    private static Class<?> exileEffectInstanceDataClass = null;
    private static Class<?> entityStatusEffectsDataClass = null;
    private static Class<?> chatFormattingClass = null;
    private static Class<?> statModClass = null;
    private static Class<?> statClass = null;
    private static Class<?> playerProfessionsDataClass = null;

    // === MethodHandles ===
    private static MethodHandle LOAD_UNIT = null;
    private static MethodHandle GET_RESOURCES = null;
    private static MethodHandle GET_MANA = null;
    private static MethodHandle GET_MAGIC_SHIELD = null;
    private static MethodHandle GET_ENERGY = null;
    private static MethodHandle GET_BLOOD = null;
    private static MethodHandle GET_MAXIMUM_RESOURCE = null;
    private static MethodHandle GET_EXP = null;
    private static MethodHandle GET_EXP_REQUIRED = null;
    private static MethodHandle GET_LEVEL = null;
    private static MethodHandle GET_UNIT = null;
    private static MethodHandle IS_BLOOD_MAGE = null;
    private static MethodHandle GET_CURRENT_HEALTH = null;
    private static MethodHandle GET_MAX_HEALTH = null;
    private static MethodHandle GET_CURRENT_MAGIC_SHIELD = null;
    private static MethodHandle GET_LAST_DAMAGE_TAKEN = null;
    private static MethodHandle GET_DAMAGE_EVENT_ELEMENT = null;
    private static MethodHandle GET_ELEMENTS_FORMAT = null;

    // === Profession MethodHandles ===
    private static MethodHandle GET_PROFESSIONS_FIELD = null;
    private static MethodHandle GET_PROF_EXP = null;
    private static MethodHandle GET_PROF_MAX_EXP = null;
    private static MethodHandle GET_PROF_LEVEL = null;

    // === Mob Info MethodHandles ===
    private static MethodHandle GET_RARITY = null;
    private static MethodHandle GET_MOB_RARITY = null;
    private static MethodHandle GET_RARITY_TEXT_FORMAT = null;
    private static MethodHandle GET_RARITY_IS_ELITE = null;
    private static MethodHandle GET_RARITY_IS_SPECIAL = null;
    private static MethodHandle GET_AFFIX_DATA = null;
    private static MethodHandle GET_AFFIXES_LIST = null;
    private static MethodHandle GET_AFFIX_LOC_NAME = null;
    private static MethodHandle GET_AFFIX_ICON = null;
    private static MethodHandle GET_AFFIX_TYPE = null;
    private static MethodHandle GET_STATUS_EFFECTS_DATA = null;
    private static MethodHandle EXILE_EFFECTS_GET = null;

    // === Affix Stats MethodHandles ===
    private static java.lang.reflect.Field AFFIX_STATS_FIELD = null;
    private static MethodHandle GET_STAT_MOD_MIN = null;
    private static MethodHandle GET_STAT_MOD_STAT = null;
    private static MethodHandle GET_STAT_MOD_TYPE = null;
    private static MethodHandle GET_STAT_LOC_NAME = null;
    private static MethodHandle GET_STAT_IS_PERC = null;
    private static volatile java.lang.reflect.Method statsRegistryMethod = null;
    private static volatile Object statsRegistry = null;
    private static volatile java.lang.reflect.Method statsRegistryGetMethod = null;

    // === ExileEffect MethodHandles ===
    private static MethodHandle GET_EXILE_MAP = null;
    private static MethodHandle GET_TICKS_LEFT = null;
    private static MethodHandle GET_STACKS = null;
    private static MethodHandle GET_IS_INFINITE = null;
    private static MethodHandle SHOULD_REMOVE = null;
    private static MethodHandle GET_EFFECT_TEXTURE = null;
    private static MethodHandle GET_EFFECT_LOC_NAME = null;
    private static MethodHandle GET_EFFECT_TYPE = null;
    private static MethodHandle GET_DURATION_STRING = null;

    // === Ailment MethodHandles ===
    private static MethodHandle GET_AILMENT_DATA = null;
    private static MethodHandle GET_DOT_MAP = null;

    // === ResourceType enum values ===
    private static Object MANA_TYPE = null;
    private static Object MAGIC_SHIELD_TYPE = null;
    private static Object ENERGY_TYPE = null;
    private static Object BLOOD_TYPE = null;

    // === Minion MethodHandles ===
    private static java.lang.reflect.Field SUMMONED_TYPES_FIELD = null;
    private static MethodHandle GET_SUMMONED_PET_DATA = null;
    private static java.lang.reflect.Field PET_SPELL_FIELD = null;
    private static java.lang.reflect.Field PET_TICKS_FIELD = null;
    private static Class<?> summonEntityClass = null;
    private static volatile java.lang.reflect.Method spellsRegistryMethod = null;
    private static volatile Object spellsRegistry = null;
    private static volatile java.lang.reflect.Method spellsRegistryGetMethod = null;

    // === Skill Hotbar MethodHandles ===
    private static Class<?> playerDataClass = null;
    private static Class<?> spellClass = null;
    private static Class<?> spellConfigClass = null;
    private static MethodHandle LOAD_PLAYER = null;
    private static MethodHandle GET_SKILL_GEM_INVENTORY = null;
    private static MethodHandle GET_HOTBAR_GEM = null;
    private static MethodHandle GET_SPELL = null;
    private static MethodHandle GET_SPELL_ICON_LOC = null;
    private static MethodHandle GET_SPELL_GUID = null;
    private static MethodHandle GET_COOLDOWNS = null;
    private static MethodHandle GET_COOLDOWN_TICKS = null;
    private static MethodHandle GET_NEEDED_TICKS = null;
    private static MethodHandle GET_IS_ON_COOLDOWN = null;
    private static String blockCooldownKey = "block";
    private static java.lang.reflect.Field MANA_COST_MIN_FIELD = null;
    private static MethodHandle SPELL_CONFIG_GETTER = null;
    private static MethodHandle GET_SUMMONED_DATA = null;
    private static MethodHandle GET_SUMMONED_AMOUNT = null;
    private static java.lang.reflect.Field SPELL_CONFIG_CHARGES_FIELD = null;
    private static java.lang.reflect.Field SPELL_CONFIG_CHARGE_NAME_FIELD = null;
    private static java.lang.reflect.Field SPELL_CONFIG_CHARGE_REGEN_FIELD = null;
    private static java.lang.reflect.Field SPELL_CASTING_DATA_FIELD = null;
    private static java.lang.reflect.Field CHARGES_OBJ_FIELD = null;
    private static MethodHandle GET_CHARGES = null;
    private static MethodHandle GET_CURRENT_TICKS_CHARGING = null;
    private static java.lang.reflect.Field IS_ON_SECOND_HOTBAR_FIELD = null;
    private static java.lang.reflect.Method CLIENT_CONFIG_GET_METHOD = null;
    private static java.lang.reflect.Field HOTBAR_SWAPPING_FIELD = null;
    private static java.lang.reflect.Method BOOLEAN_VALUE_GET_METHOD = null;

    // === Config MethodHandles ===
    private static java.lang.reflect.Field NEAT_CONFIG_DRAW_FIELD = null;

    // === Potion MethodHandles ===
    private static Class<?> slashPotionItemClass = null;
    private static MethodHandle GET_POTION_TYPE = null;
    private static MethodHandle GET_POTION_RARITY = null;
    private static Object POTION_TYPE_HP = null;
    private static Object POTION_TYPE_MANA = null;
    private static java.lang.reflect.Field RARITY_STAT_PERCENTS_FIELD = null;
    private static java.lang.reflect.Field STAT_PERCENTS_MAX_FIELD = null;

    // === Mercenary MethodHandles ===
    private static MethodHandle GET_SERVER_MERCENARY = null;
    private static MethodHandle GET_MERC_CLASS = null;
    private static MethodHandle GET_MERC_ICON_LOC = null;
    private static MethodHandle GET_MERC_DATA = null;
    private static MethodHandle GET_EQUIPPED_SPELL = null;
    private static Method mercRegistryMethod = null;
    private static Object mercRegistry = null;
    private static Method mercRegistryGetMethod = null;
    private static final ResourceLocation MERC_ICON_FALLBACK = new ResourceLocation("mmorpg", "textures/gui/spells/icons/summon_zombie.png");

    // === Additional ExileEffect Field Handles ===
    private static MethodHandle GET_SPELL_ID = null;
    private static MethodHandle GET_SELF_CAST = null;
    private static MethodHandle GET_CASTER_UUID = null;
    private static MethodHandle GET_EFFECT_TAGS = null;
    private static MethodHandle GET_TAG_LIST_TAGS = null;

    // === Effect Timer Cache ===
    private static final java.util.concurrent.ConcurrentHashMap<String, long[]> effectTimerCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static long lastEffectCleanup = 0;

    static {
        initialize();
    }
    
    /**
     * MethodHandlesを初期化
     * 各クラス・メソッドを個別にtry-catchで保護
     */
    private static void initialize() {
        LOGGER.debug("Initializing MethodHandles for Mine and Slash integration...");
        
        try {
            loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            entityDataClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.EntityData");
            resourcesDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourcesData");
            resourceTypeClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
            unitClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit");
            healthUtilsClass = Class.forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");
            elementsClass = Class.forName("com.robertx22.mine_and_slash.uncommon.enumclasses.Elements");
            damageEventClass = Class.forName("com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent");
            
            LOAD_UNIT = lookupMethod(loadClass, "Unit", Entity.class);
            GET_RESOURCES = lookupMethod(entityDataClass, "getResources");
            GET_MANA = lookupMethod(resourcesDataClass, "getMana");
            GET_MAGIC_SHIELD = lookupMethod(resourcesDataClass, "getMagicShield");
            GET_ENERGY = lookupMethod(resourcesDataClass, "getEnergy");
            GET_BLOOD = lookupMethod(resourcesDataClass, "getBlood");
            GET_MAXIMUM_RESOURCE = lookupMethod(entityDataClass, "getMaximumResource", resourceTypeClass);
            GET_EXP = lookupMethod(entityDataClass, "getExp");
            GET_EXP_REQUIRED = lookupMethod(entityDataClass, "getExpRequiredForLevelUp");
            GET_LEVEL = lookupMethod(entityDataClass, "getLevel");
            GET_UNIT = lookupMethod(entityDataClass, "getUnit");
            IS_BLOOD_MAGE = lookupMethod(unitClass, "isBloodMage");
            GET_CURRENT_HEALTH = lookupMethod(healthUtilsClass, "getCurrentHealth", LivingEntity.class);
            GET_MAX_HEALTH = lookupMethod(healthUtilsClass, "getMaxHealth", LivingEntity.class);
            GET_CURRENT_MAGIC_SHIELD = lookupMethod(healthUtilsClass, "getCurrentMagicShield", LivingEntity.class);
            
            GET_LAST_DAMAGE_TAKEN = lookupFieldGetter(entityDataClass, "lastDamageTaken");
            GET_DAMAGE_EVENT_ELEMENT = lookupMethod(damageEventClass, "getElement");
            GET_ELEMENTS_FORMAT = lookupFieldGetter(elementsClass, "format");
            
            MANA_TYPE = getEnumValue(resourceTypeClass, "mana");
            MAGIC_SHIELD_TYPE = getEnumValue(resourceTypeClass, "magic_shield");
            ENERGY_TYPE = getEnumValue(resourceTypeClass, "energy");
            BLOOD_TYPE = getEnumValue(resourceTypeClass, "blood");

            try {
                playerDataClass = Class.forName("com.robertx22.mine_and_slash.capability.player.PlayerData");
                playerProfessionsDataClass = Class.forName("com.robertx22.mine_and_slash.capability.player.data.PlayerProfessionsData");
                LOAD_PLAYER = lookupMethod(loadClass, "player", Player.class);
                GET_PROFESSIONS_FIELD = lookupFieldGetter(playerDataClass, "professions");
                GET_PROF_EXP = lookupMethod(playerProfessionsDataClass, "getExp", String.class);
                GET_PROF_MAX_EXP = lookupMethod(playerProfessionsDataClass, "getMaxExp", String.class);
                GET_PROF_LEVEL = lookupMethod(playerProfessionsDataClass, "getLevel", String.class);
            } catch (Throwable t) {
                LOGGER.debug("Profession MethodHandles initialization skipped or failed: {}", t.getMessage());
            }
            
            available = LOAD_UNIT != null && GET_RESOURCES != null;
            
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MethodHandles initialization status:");
                LOGGER.debug("  LOAD_UNIT: {}", LOAD_UNIT != null ? "OK" : "NULL");
                LOGGER.debug("  GET_RESOURCES: {}", GET_RESOURCES != null ? "OK" : "NULL");
                LOGGER.debug("  GET_CURRENT_HEALTH: {}", GET_CURRENT_HEALTH != null ? "OK" : "NULL");
                LOGGER.debug("  GET_MAX_HEALTH: {}", GET_MAX_HEALTH != null ? "OK" : "NULL");
                LOGGER.debug("  GET_MANA: {}", GET_MANA != null ? "OK" : "NULL");
                LOGGER.debug("  GET_MAGIC_SHIELD: {}", GET_MAGIC_SHIELD != null ? "OK" : "NULL");
                LOGGER.debug("  GET_ENERGY: {}", GET_ENERGY != null ? "OK" : "NULL");
                LOGGER.debug("  GET_BLOOD: {}", GET_BLOOD != null ? "OK" : "NULL");
                LOGGER.debug("  GET_LAST_DAMAGE_TAKEN: {}", GET_LAST_DAMAGE_TAKEN != null ? "OK" : "NULL");
                LOGGER.debug("  GET_DAMAGE_EVENT_ELEMENT: {}", GET_DAMAGE_EVENT_ELEMENT != null ? "OK" : "NULL");
            }
            
            if (available) {
                LOGGER.debug("MethodHandles initialized successfully. M&S integration enabled.");
            } else {
                LOGGER.warn("Required MethodHandles not available. M&S integration disabled.");
            }
            
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Mine and Slash classes not found. Using vanilla fallbacks.");
            available = false;
        } catch (Exception e) {
            LOGGER.error("Failed to initialize MethodHandles: {}", e.getMessage(), e);
            available = false;
        }

        if (available) {
            initializeMobInfoHandles();
            initializeSkillHandles();
            initializePotionHandles();
            initializeConfigHandles();
            initializeAdditionalEffectHandles();
            initializeMercenaryHandles();
        }
    }

    private static void initializeMobInfoHandles() {
        try {
            mobRarityClass = Class.forName("com.robertx22.mine_and_slash.database.data.rarities.MobRarity");
            mobDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.MobData");
            mobAffixClass = Class.forName("com.robertx22.mine_and_slash.database.data.mob_affixes.MobAffix");
            exileDBClass = Class.forName("com.robertx22.mine_and_slash.database.registry.ExileDB");
            chatFormattingClass = Class.forName("net.minecraft.ChatFormatting");

            GET_RARITY = lookupFieldGetter(entityDataClass, "rarity");
            GET_MOB_RARITY = lookupMethod(entityDataClass, "getMobRarity");
            GET_RARITY_TEXT_FORMAT = lookupFieldGetter(mobRarityClass, "text_format");
            GET_RARITY_IS_ELITE = lookupFieldGetter(mobRarityClass, "is_elite");
            GET_RARITY_IS_SPECIAL = lookupFieldGetter(mobRarityClass, "is_special");

            GET_AFFIX_DATA = lookupMethod(entityDataClass, "getAffixData");
            GET_AFFIXES_LIST = lookupMethod(mobDataClass, "getAffixes");
            GET_AFFIX_LOC_NAME = lookupMethod(mobAffixClass, "locName");
            GET_AFFIX_ICON = lookupFieldGetter(mobAffixClass, "icon");
            GET_AFFIX_TYPE = lookupFieldGetter(mobAffixClass, "type");

            statModClass = Class.forName("com.robertx22.mine_and_slash.database.data.StatMod");
            statClass = Class.forName("com.robertx22.mine_and_slash.database.data.stats.Stat");

            AFFIX_STATS_FIELD = mobAffixClass.getDeclaredField("stats");
            AFFIX_STATS_FIELD.setAccessible(true);

            GET_STAT_MOD_MIN = lookupFieldGetter(statModClass, "min");
            GET_STAT_MOD_STAT = lookupFieldGetter(statModClass, "stat");
            GET_STAT_MOD_TYPE = lookupFieldGetter(statModClass, "type");

            GET_STAT_LOC_NAME = lookupMethod(statClass, "locName");
            GET_STAT_IS_PERC = lookupFieldGetter(statClass, "is_perc");

            statsRegistryMethod = exileDBClass.getMethod("Stats");

            GET_STATUS_EFFECTS_DATA = lookupMethod(entityDataClass, "getStatusEffectsData");
            EXILE_EFFECTS_GET = lookupMethod(exileDBClass, "ExileEffects");

            entityStatusEffectsDataClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.potion_effects.EntityStatusEffectsData");
            exileEffectInstanceDataClass = Class.forName("com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffectInstanceData");
            exileEffectClass = Class.forName("com.robertx22.mine_and_slash.database.data.exile_effects.ExileEffect");

            GET_EXILE_MAP = lookupFieldGetter(entityStatusEffectsDataClass, "exileMap");
            GET_TICKS_LEFT = lookupFieldGetter(exileEffectInstanceDataClass, "ticks_left");
            GET_STACKS = lookupFieldGetter(exileEffectInstanceDataClass, "stacks");
            GET_IS_INFINITE = lookupFieldGetter(exileEffectInstanceDataClass, "is_infinite");
            SHOULD_REMOVE = lookupMethod(exileEffectInstanceDataClass, "shouldRemove");
            GET_EFFECT_TEXTURE = lookupMethod(exileEffectClass, "getTexture");
            GET_EFFECT_LOC_NAME = lookupMethod(exileEffectClass, "locName");
            GET_EFFECT_TYPE = lookupFieldGetter(exileEffectClass, "type");
            GET_DURATION_STRING = lookupMethod(exileEffectInstanceDataClass, "getDurationString");

            // Ailment Handles
            try {
                GET_AILMENT_DATA = lookupFieldGetter(entityDataClass, "ailments");
                Class<?> ailmentDataClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.EntityAilmentData");
                GET_DOT_MAP = lookupFieldGetter(ailmentDataClass, "datas");
            } catch (Exception e) {
                LOGGER.debug("Ailment handles not available: {}", e.getMessage());
            }

            LOGGER.debug("Mob info MethodHandles initialized: rarity={}, affix={}, status={}, effects={}, ailment={}",
                    GET_MOB_RARITY != null, GET_AFFIX_DATA != null, GET_STATUS_EFFECTS_DATA != null,
                    GET_EXILE_MAP != null, GET_AILMENT_DATA != null);
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize mob info handles (non-critical): {}", e.getMessage());
        }
    }
    
    /**
     * メソッドをLookupしてMethodHandleを取得
     */
    private static MethodHandle lookupMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            Method method = clazz.getMethod(name, paramTypes);
            return PUBLIC_LOOKUP.unreflect(method);
        } catch (Exception e) {
            LOGGER.debug("Failed to lookup method {}.{}: {}", clazz.getSimpleName(), name, e.getMessage());
            return null;
        }
    }
    
    /**
     * フィールドのGetter MethodHandleをLookup
     */
    private static MethodHandle lookupFieldGetter(Class<?> clazz, String fieldName) {
        try {
            java.lang.reflect.Field field = clazz.getField(fieldName);
            return PUBLIC_LOOKUP.unreflectGetter(field);
        } catch (NoSuchFieldException e) {
            try {
                java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return PUBLIC_LOOKUP.unreflectGetter(field);
            } catch (Exception ex) {
                LOGGER.debug("Failed to lookup field getter {}.{}: {}", clazz.getSimpleName(), fieldName, ex.getMessage());
                return null;
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to lookup field getter {}.{}: {}", clazz.getSimpleName(), fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * enum値を取得
     */
    private static Object getEnumValue(Class<?> enumClass, String fieldName) {
        try {
            return enumClass.getField(fieldName).get(null);
        } catch (Exception e) {
            LOGGER.debug("Failed to get enum value {}.{}: {}", enumClass.getSimpleName(), fieldName, e.getMessage());
            return null;
        }
    }
    
    /**
     * MethodHandlesが利用可能か
     */
    public static boolean isAvailable() {
        return available;
    }
    
    // ========== 高速アクセッサーメソッド ==========
    
    public static Object loadUnit(Entity entity) throws Throwable {
        if (LOAD_UNIT == null) return null;
        return LOAD_UNIT.invoke(entity);
    }
    
    public static Object getResources(Object entityData) throws Throwable {
        if (GET_RESOURCES == null || entityData == null) return null;
        return GET_RESOURCES.invoke(entityData);
    }
    
    public static float getMana(Object resources) throws Throwable {
        if (GET_MANA == null || resources == null) return 0f;
        Object result = GET_MANA.invoke(resources);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    public static float getMagicShield(Object resources) throws Throwable {
        if (GET_MAGIC_SHIELD == null || resources == null) return 0f;
        Object result = GET_MAGIC_SHIELD.invoke(resources);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    public static float getEnergy(Object resources) throws Throwable {
        if (GET_ENERGY == null || resources == null) return 0f;
        Object result = GET_ENERGY.invoke(resources);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    public static float getBlood(Object resources) throws Throwable {
        if (GET_BLOOD == null || resources == null) return 0f;
        Object result = GET_BLOOD.invoke(resources);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    public static float getMaximumResource(Object entityData, Object resourceType) throws Throwable {
        if (GET_MAXIMUM_RESOURCE == null || entityData == null || resourceType == null) return 1f;
        Object result = GET_MAXIMUM_RESOURCE.invoke(entityData, resourceType);
        return result instanceof Number ? ((Number) result).floatValue() : 1f;
    }
    
    public static float getExp(Object entityData) throws Throwable {
        if (GET_EXP == null || entityData == null) return 0f;
        Object result = GET_EXP.invoke(entityData);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    public static float getExpRequired(Object entityData) throws Throwable {
        if (GET_EXP_REQUIRED == null || entityData == null) return 1f;
        Object result = GET_EXP_REQUIRED.invoke(entityData);
        return result instanceof Number ? ((Number) result).floatValue() : 1f;
    }
    
    public static int getLevel(Object entityData) throws Throwable {
        if (GET_LEVEL == null || entityData == null) return 0;
        Object result = GET_LEVEL.invoke(entityData);
        return result instanceof Number ? ((Number) result).intValue() : 0;
    }
    
    public static Object getUnit(Object entityData) throws Throwable {
        if (GET_UNIT == null || entityData == null) return null;
        return GET_UNIT.invoke(entityData);
    }
    
    public static boolean isBloodMage(Object unit) throws Throwable {
        if (IS_BLOOD_MAGE == null || unit == null) return false;
        Object result = IS_BLOOD_MAGE.invoke(unit);
        return result instanceof Boolean ? (Boolean) result : false;
    }
    
    public static float getCurrentHealth(LivingEntity entity) throws Throwable {
        if (entity == null) return 0f;
        if (GET_CURRENT_HEALTH == null) throw new IllegalStateException("M&S health handle not available");
        Object result = GET_CURRENT_HEALTH.invoke(entity);
        // M&SのgetCurrentHealthはintを返すため、Numberとして扱う
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }

    public static float getMaxHealth(LivingEntity entity) throws Throwable {
        if (entity == null) return 0f;
        if (GET_MAX_HEALTH == null) throw new IllegalStateException("M&S max health handle not available");
        Object result = GET_MAX_HEALTH.invoke(entity);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }

    public static float getCurrentMagicShield(LivingEntity entity) throws Throwable {
        if (entity == null || GET_CURRENT_MAGIC_SHIELD == null) return 0f;
        Object result = GET_CURRENT_MAGIC_SHIELD.invoke(entity);
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }
    
    // ========== ResourceType取得 ==========
    
    public static Object getManaType() {
        return MANA_TYPE;
    }
    
    public static Object getMagicShieldType() {
        return MAGIC_SHIELD_TYPE;
    }
    
    public static Object getEnergyType() {
        return ENERGY_TYPE;
    }
    
    public static Object getBloodType() {
        return BLOOD_TYPE;
    }

    // ========== 属性（Element）取得 ==========

    /**
     * エンティティの最後に受けたダメージの属性（Element）を取得
     * @param entity LivingEntity
     * @return Elements enum値、取得失敗時はnull
     */
    public static Object getLastDamageElement(LivingEntity entity) throws Throwable {
        if (GET_LAST_DAMAGE_TAKEN == null || GET_DAMAGE_EVENT_ELEMENT == null || entity == null) {
            return null;
        }

        // EntityDataを取得
        Object entityData = LOAD_UNIT.invoke(entity);
        if (entityData == null) return null;

        // lastDamageTakenフィールドを取得
        Object damageEvent = GET_LAST_DAMAGE_TAKEN.invoke(entityData);
        if (damageEvent == null) return null;

        // getElement()を呼び出し
        return GET_DAMAGE_EVENT_ELEMENT.invoke(damageEvent);
    }

    /**
     * Elements enumの色（ChatFormatting）を取得
     * @param element Elements enum値
     * @return ChatFormatting、取得失敗時はnull
     */
    public static Object getElementFormat(Object element) throws Throwable {
        if (GET_ELEMENTS_FORMAT == null || element == null) {
            return null;
        }
        return GET_ELEMENTS_FORMAT.invoke(element);
    }

    /**
     * 属性名からElements enum値を取得
     * @param name 属性名（"Physical", "Fire", "Cold", "Nature", "Shadow"など）
     * @return Elements enum値、取得失敗時はnull
     */
    public static Object getElementByName(String name) {
        if (elementsClass == null || name == null) {
            return null;
        }
        try {
            return elementsClass.getField(name.toUpperCase()).get(null);
        } catch (Exception e) {
            LOGGER.debug("Failed to get Element by name: {}", name);
            return null;
        }
    }

    /**
     * 属性に対応する色コード（int）を取得
     * M&SのDamageポップアップ色と同じ色を返す
     * @param element Elements enum値
     * @return ARGB色コード、取得失敗時は0xFFFFFF（白）
     */
    public static int getElementColor(Object element) {
        if (element == null) {
            return 0xFFFFFF; // 白（デフォルト）
        }

        try {
            // Elements.format（ChatFormatting）を取得
            Object format = GET_ELEMENTS_FORMAT.invoke(element);
            if (format == null) return 0xFFFFFF;

            // ChatFormattingの色を取得
            // ChatFormattingはMinecraftの標準的な色定義
            String formatName = format.toString();
            return chatFormattingToColor(format);
        } catch (Throwable t) {
            LOGGER.debug("Error getting element color: {}", t.getMessage());
            return 0xFFFFFF;
        }
    }

    /**
     * ChatFormattingをARGB色コードに変換
     */
    private static int chatFormattingToColor(Object format) {
        if (format == null) return 0xFFFFFF;

        String name = format.toString();
        return switch (name) {
            case "GOLD" -> 0xFFAA00;          // Physical - 金色
            case "RED" -> 0xFF5555;           // Fire - 赤
            case "AQUA" -> 0x55FFFF;          // Cold (Water) - 水色
            case "YELLOW" -> 0xFFFF55;        // Nature (Lightning) - 黄色
            case "DARK_PURPLE" -> 0xAA00AA;   // Shadow (Chaos) - 紫
            case "LIGHT_PURPLE" -> 0xFF77FF;  // Elemental/All - 薄紫
            case "DARK_RED" -> 0xAA0000;
            case "GREEN" -> 0x55FF55;
            case "DARK_GREEN" -> 0x00AA00;
            case "BLUE" -> 0x5555FF;
            case "DARK_BLUE" -> 0x0000AA;
            case "DARK_AQUA" -> 0x00AAAA;
            case "DARK_GRAY" -> 0x555555;
            case "GRAY" -> 0xAAAAAA;
            case "BLACK" -> 0x000000;
            case "WHITE" -> 0xFFFFFF;
            default -> 0xFFFFFF;
        };
    }

    /**
     * 属性名（String）から直接色コードを取得する便利メソッド
     * @param entity LivingEntity（被弾者）
     * @return ARGB色コード、取得失敗時は0xFFFFFF（白）
     */
    public static int getLastDamageElementColor(LivingEntity entity) {
        try {
            Object element = getLastDamageElement(entity);
            return getElementColor(element);
        } catch (Throwable t) {
            LOGGER.debug("Error getting last damage element color: {}", t.getMessage());
            return 0xFFFFFF;
        }
    }

    // ========== Mob Info Accessors ==========

    public static Object getEntityData(Entity entity) throws Throwable {
        if (LOAD_UNIT == null || entity == null) return null;
        return LOAD_UNIT.invoke(entity);
    }

    public static String getRarityString(Object entityData) throws Throwable {
        if (GET_RARITY == null || entityData == null) return null;
        Object result = GET_RARITY.invoke(entityData);
        return result instanceof String ? (String) result : null;
    }

    public static Object getMobRarityObj(Object entityData) throws Throwable {
        if (GET_MOB_RARITY == null || entityData == null) return null;
        return GET_MOB_RARITY.invoke(entityData);
    }

    public static int getRarityColor(Object mobRarity) {
        if (mobRarity == null) return 0xFFFFFF;
        try {
            Object format = GET_RARITY_TEXT_FORMAT.invoke(mobRarity);
            return chatFormattingToColor(format);
        } catch (Throwable t) {
            return 0xFFFFFF;
        }
    }

    public static boolean isRarityElite(Object mobRarity) {
        if (mobRarity == null || GET_RARITY_IS_ELITE == null) return false;
        try {
            Object result = GET_RARITY_IS_ELITE.invoke(mobRarity);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isRaritySpecial(Object mobRarity) {
        if (mobRarity == null || GET_RARITY_IS_SPECIAL == null) return false;
        try {
            Object result = GET_RARITY_IS_SPECIAL.invoke(mobRarity);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    public static java.util.List<String> getMobAffixIds(Object entityData) throws Throwable {
        if (GET_AFFIX_DATA == null || GET_AFFIXES_LIST == null || entityData == null) {
            return java.util.Collections.emptyList();
        }
        Object mobData = GET_AFFIX_DATA.invoke(entityData);
        if (mobData == null) return java.util.Collections.emptyList();
        Object affixes = GET_AFFIXES_LIST.invoke(mobData);
        if (affixes instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) affixes;
            java.util.List<String> result = new java.util.ArrayList<>();
            for (Object a : list) {
                try {
                    Object locName = GET_AFFIX_LOC_NAME.invoke(a);
                    if (locName != null) {
                        result.add(locName.toString());
                    }
                } catch (Throwable t) {
                    LOGGER.debug("Failed to get affix name: {}", t.getMessage());
                }
            }
            return result;
        }
        return java.util.Collections.emptyList();
    }

    public static java.util.List<Object> getMobAffixObjects(Object entityData) throws Throwable {
        if (GET_AFFIX_DATA == null || GET_AFFIXES_LIST == null || entityData == null) {
            return java.util.Collections.emptyList();
        }
        Object mobData = GET_AFFIX_DATA.invoke(entityData);
        if (mobData == null) return java.util.Collections.emptyList();
        Object affixes = GET_AFFIXES_LIST.invoke(mobData);
        if (affixes instanceof java.util.List) {
            @SuppressWarnings("unchecked")
            java.util.List<Object> list = (java.util.List<Object>) affixes;
            return list;
        }
        return java.util.Collections.emptyList();
    }

    public static String getAffixLocName(Object affix) {
        if (affix == null || GET_AFFIX_LOC_NAME == null) return "";
        try {
            Object locName = GET_AFFIX_LOC_NAME.invoke(affix);
            if (locName == null) return "";
            if (locName instanceof net.minecraft.network.chat.Component comp) {
                return comp.getString();
            }
            return locName.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static String getAffixIcon(Object affix) {
        if (affix == null || GET_AFFIX_ICON == null) return "";
        try {
            Object icon = GET_AFFIX_ICON.invoke(affix);
            return icon != null ? icon.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean isAffixPrefix(Object affix) {
        if (affix == null || GET_AFFIX_TYPE == null) return true;
        try {
            Object slot = GET_AFFIX_TYPE.invoke(affix);
            if (slot == null) return true;
            String str = slot.toString();
            if (str != null && str.equalsIgnoreCase("suffix")) {
                return false;
            }
            return true;
        } catch (Throwable t) {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public static java.util.List<Object> getAffixStatsList(Object affix) {
        if (AFFIX_STATS_FIELD == null || affix == null) return java.util.Collections.emptyList();
        try {
            Object list = AFFIX_STATS_FIELD.get(affix);
            if (list instanceof java.util.List) return (java.util.List<Object>) list;
        } catch (Exception e) {
            LOGGER.debug("Failed to get affix stats: {}", e.getMessage());
        }
        return java.util.Collections.emptyList();
    }

    public static float getStatModMin(Object statMod) {
        if (GET_STAT_MOD_MIN == null || statMod == null) return 0f;
        try {
            Object result = GET_STAT_MOD_MIN.invoke(statMod);
            return result instanceof Number ? ((Number) result).floatValue() : 0f;
        } catch (Throwable t) {
            return 0f;
        }
    }

    public static String getStatModStatGuid(Object statMod) {
        if (GET_STAT_MOD_STAT == null || statMod == null) return null;
        try {
            Object result = GET_STAT_MOD_STAT.invoke(statMod);
            return result instanceof String ? (String) result : null;
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getStatModType(Object statMod) {
        if (GET_STAT_MOD_TYPE == null || statMod == null) return "FLAT";
        try {
            Object result = GET_STAT_MOD_TYPE.invoke(statMod);
            return result instanceof String ? (String) result : "FLAT";
        } catch (Throwable t) {
            return "FLAT";
        }
    }

    public static Object getStatFromRegistry(String guid) {
        if (guid == null || exileDBClass == null) return null;
        try {
            if (statsRegistry == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (statsRegistry == null) {
                        if (statsRegistryMethod == null) {
                            statsRegistryMethod = exileDBClass.getMethod("Stats");
                        }
                        statsRegistry = statsRegistryMethod.invoke(null);
                        if (statsRegistry == null) return null;
                    }
                }
            }
            if (statsRegistryGetMethod == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (statsRegistryGetMethod == null && statsRegistry != null) {
                        statsRegistryGetMethod = statsRegistry.getClass().getMethod("get", String.class);
                    }
                }
            }
            if (statsRegistryGetMethod != null && statsRegistry != null) {
                return statsRegistryGetMethod.invoke(statsRegistry, guid);
            }
            return null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            LOGGER.debug("Stats registry lookup error for {}: {}", guid,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.debug("Stats registry lookup error for {}: {}", guid, e.getMessage());
            synchronized (MethodHandlesUtil.class) {
                statsRegistry = null;
            }
            return null;
        }
    }

    public static String getStatLocName(Object stat) {
        if (stat == null || GET_STAT_LOC_NAME == null) return "";
        try {
            Object name = GET_STAT_LOC_NAME.invoke(stat);
            if (name instanceof net.minecraft.network.chat.Component comp) {
                return comp.getString();
            }
            return name != null ? name.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean getStatIsPercent(Object stat) {
        if (stat == null || GET_STAT_IS_PERC == null) return false;
        try {
            Object result = GET_STAT_IS_PERC.invoke(stat);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    public static Object getStatusEffectsData(Object entityData) throws Throwable {
        if (GET_STATUS_EFFECTS_DATA == null || entityData == null) return null;
        return GET_STATUS_EFFECTS_DATA.invoke(entityData);
    }

    public static boolean isStatusEffectsHandleAvailable() {
        return GET_STATUS_EFFECTS_DATA != null;
    }

    public static java.util.Map<String, Object> getExileEffectMap(Object statusEffectsData) throws Throwable {
        if (statusEffectsData == null || GET_EXILE_MAP == null) return java.util.Collections.emptyMap();
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) GET_EXILE_MAP.invoke(statusEffectsData);
            return map != null ? map : java.util.Collections.emptyMap();
        } catch (Throwable t) {
            return java.util.Collections.emptyMap();
        }
    }

    public static int getEffectTicksLeft(Object instanceData) {
        if (instanceData == null || GET_TICKS_LEFT == null) return 0;
        try {
            Object result = GET_TICKS_LEFT.invoke(instanceData);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getEffectStacks(Object instanceData) {
        if (instanceData == null || GET_STACKS == null) return 0;
        try {
            Object result = GET_STACKS.invoke(instanceData);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean isEffectInfinite(Object instanceData) {
        if (instanceData == null || GET_IS_INFINITE == null) return false;
        try {
            Object result = GET_IS_INFINITE.invoke(instanceData);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean shouldEffectRemove(Object instanceData) {
        if (instanceData == null || SHOULD_REMOVE == null) return true;
        try {
            Object result = SHOULD_REMOVE.invoke(instanceData);
            return result instanceof Boolean && (Boolean) result;
        } catch (Throwable t) {
            return true;
        }
    }

    private static volatile java.lang.reflect.Method exileRegistryGetMethod = null;
    private static volatile Object exileEffectsRegistry = null;
    private static volatile java.lang.reflect.Method exileEffectsMethod = null;

    public static Object getExileEffectFromDB(String effectId) {
        if (exileDBClass == null || effectId == null) return null;
        try {
            if (exileEffectsRegistry == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (exileEffectsRegistry == null) {
                        if (exileEffectsMethod == null) {
                            exileEffectsMethod = exileDBClass.getMethod("ExileEffects");
                        }
                        exileEffectsRegistry = exileEffectsMethod.invoke(null);
                        if (exileEffectsRegistry == null) return null;
                    }
                }
            }
            if (exileRegistryGetMethod == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (exileRegistryGetMethod == null && exileEffectsRegistry != null) {
                        exileRegistryGetMethod = exileEffectsRegistry.getClass().getMethod("get", String.class);
                    }
                }
            }
            if (exileRegistryGetMethod != null && exileEffectsRegistry != null) {
                return exileRegistryGetMethod.invoke(exileEffectsRegistry, effectId);
            }
            return null;
        } catch (java.lang.reflect.InvocationTargetException e) {
            LOGGER.debug("ExileDB.get({}) threw: {}", effectId,
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.debug("ExileDB lookup error for {}: {}", effectId, e.getMessage());
            synchronized (MethodHandlesUtil.class) {
                exileEffectsRegistry = null;
            }
            return null;
        }
    }

    public static ResourceLocation getExileEffectTexture(Object exileEffect) {
        if (exileEffect == null || GET_EFFECT_TEXTURE == null) return null;
        try {
            return (ResourceLocation) GET_EFFECT_TEXTURE.invoke(exileEffect);
        } catch (Throwable t) {
            return null;
        }
    }

    public static String getExileEffectName(Object exileEffect) {
        if (exileEffect == null || GET_EFFECT_LOC_NAME == null) return "";
        try {
            Object name = GET_EFFECT_LOC_NAME.invoke(exileEffect);
            return name != null ? name.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean isEffectNegative(Object exileEffect) {
        if (exileEffect == null || GET_EFFECT_TYPE == null) return false;
        try {
            Object type = GET_EFFECT_TYPE.invoke(exileEffect);
            if (type != null) {
                return "negative".equals(type.toString());
            }
        } catch (Throwable t) {
            // ignore
        }
        return false;
    }

    public static String getEffectDurationString(Object instanceData) {
        if (instanceData == null || GET_DURATION_STRING == null) return "";
        try {
            return (String) GET_DURATION_STRING.invoke(instanceData);
        } catch (Throwable t) {
            return "";
        }
    }

    // ========== Skill Hotbar Initialization ==========

    private static void initializeSkillHandles() {
        try {
            playerDataClass = Class.forName("com.robertx22.mine_and_slash.capability.player.PlayerData");
            spellClass = Class.forName("com.robertx22.mine_and_slash.database.data.spells.components.Spell");

            LOAD_PLAYER = lookupMethod(loadClass, "player", Player.class);
            GET_SKILL_GEM_INVENTORY = lookupMethod(playerDataClass, "getSkillGemInventory");

            Class<?> gemInventoryHelperClass = Class.forName("com.robertx22.mine_and_slash.capability.player.helper.GemInventoryHelper");
            GET_HOTBAR_GEM = lookupMethod(gemInventoryHelperClass, "getHotbarGem", int.class);

            Class<?> socketedGemClass = Class.forName("com.robertx22.mine_and_slash.capability.player.helper.SocketedGem");
            GET_SPELL = lookupMethod(socketedGemClass, "getSpell");

            GET_SPELL_ICON_LOC = lookupMethod(spellClass, "getIconLoc");
            GET_SPELL_GUID = lookupMethod(spellClass, "GUID");

            GET_COOLDOWNS = lookupMethod(entityDataClass, "getCooldowns");

            try {
                spellConfigClass = Class.forName("com.robertx22.mine_and_slash.database.data.spells.components.SpellConfiguration");
                SPELL_CONFIG_GETTER = lookupFieldGetter(spellClass, "config");
                SPELL_CONFIG_CHARGES_FIELD = spellConfigClass.getField("charges");
                SPELL_CONFIG_CHARGE_NAME_FIELD = spellConfigClass.getField("charge_name");
                SPELL_CONFIG_CHARGE_REGEN_FIELD = spellConfigClass.getField("charge_regen");

                Class<?> leveledValueClass = Class.forName("com.robertx22.mine_and_slash.database.data.value_calc.LeveledValue");
                MANA_COST_MIN_FIELD = leveledValueClass.getField("min");
            } catch (Exception e) {
                LOGGER.debug("Spell config handles not available: {}", e.getMessage());
            }

            GET_SUMMONED_DATA = lookupMethod(playerDataClass, "getSummonedData");
            Class<?> summonedDataClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.SummonedData");
            GET_SUMMONED_AMOUNT = lookupMethod(summonedDataClass, "getSummonedAmount", String.class);

            try {
                SUMMONED_TYPES_FIELD = summonedDataClass.getDeclaredField("summonedTypes");
                SUMMONED_TYPES_FIELD.setAccessible(true);
            } catch (Exception e) {
                LOGGER.debug("summonedTypes field not accessible: {}", e.getMessage());
            }

            try {
                Class<?> summonedPetDataClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.SummonedPetData");
                PET_SPELL_FIELD = summonedPetDataClass.getField("spell");
                PET_TICKS_FIELD = summonedPetDataClass.getField("ticks");
                GET_SUMMONED_PET_DATA = lookupFieldGetter(entityDataClass, "summonedPetData");
                summonEntityClass = Class.forName("com.robertx22.mine_and_slash.database.data.spells.summons.entity.SummonEntity");
            } catch (Exception e) {
                LOGGER.debug("SummonedPetData handles not available: {}", e.getMessage());
            }

            SPELL_CASTING_DATA_FIELD = playerDataClass.getField("spellCastingData");
            Class<?> spellCastingDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.spells.SpellCastingData");
            CHARGES_OBJ_FIELD = spellCastingDataClass.getField("charges");
            Class<?> chargeDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.spells.ChargeData");
            GET_CHARGES = lookupMethod(chargeDataClass, "getCharges", String.class);
            GET_CURRENT_TICKS_CHARGING = lookupMethod(chargeDataClass, "getCurrentTicksChargingOf", String.class);

            Class<?> cooldownsClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.CooldownsData");
            GET_COOLDOWN_TICKS = lookupMethod(cooldownsClass, "getCooldownTicks", String.class);
            GET_NEEDED_TICKS = lookupMethod(cooldownsClass, "getNeededTicks", String.class);
            GET_IS_ON_COOLDOWN = lookupMethod(cooldownsClass, "isOnCooldown", String.class);

            try {
                Class<?> blockChanceClass = Class.forName("com.robertx22.mine_and_slash.database.data.stats.types.defense.BlockChance");
                java.lang.reflect.Field blockCdField = blockChanceClass.getField("BLOCK_CD");
                Object val = blockCdField.get(null);
                if (val instanceof String s && !s.isEmpty()) {
                    blockCooldownKey = s;
                }
            } catch (Exception e) {
                LOGGER.debug("BlockChance.BLOCK_CD not available: {}", e.getMessage());
            }

            try {
                Class<?> spellKeybindClass = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
                try {
                    IS_ON_SECOND_HOTBAR_FIELD = spellKeybindClass.getField("IS_ON_SECONd_HOTBAR");
                } catch (NoSuchFieldException e) {
                    try {
                        IS_ON_SECOND_HOTBAR_FIELD = spellKeybindClass.getField("IS_ON_SECOND_HOTBAR");
                    } catch (NoSuchFieldException e2) {
                        for (java.lang.reflect.Field f : spellKeybindClass.getDeclaredFields()) {
                            if (f.getType() == boolean.class || f.getType() == Boolean.class) {
                                if (f.getName().toLowerCase(java.util.Locale.ROOT).contains("second")) {
                                    f.setAccessible(true);
                                    IS_ON_SECOND_HOTBAR_FIELD = f;
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("SpellKeybind not available: {}", e.getMessage());
            }

            try {
                Class<?> clientConfigsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
                CLIENT_CONFIG_GET_METHOD = clientConfigsClass.getMethod("getConfig");
            } catch (Exception e) {
                LOGGER.debug("ClientConfigs not available: {}", e.getMessage());
            }

            LOGGER.debug("Skill Hotbar handles initialized: player={}, spell={}, cooldown={}",
                    LOAD_PLAYER != null, GET_SPELL != null, GET_COOLDOWNS != null);
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize skill handles (non-critical): {}", e.getMessage());
        }
    }

    // ========== Potion Initialization ==========

    private static void initializePotionHandles() {
        try {
            slashPotionItemClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem");
            Class<?> potionTypeClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.items.SlashPotionItem$Type");

            GET_POTION_TYPE = lookupMethod(slashPotionItemClass, "getType");
            GET_POTION_RARITY = lookupMethod(slashPotionItemClass, "getRarity");

            POTION_TYPE_HP = getEnumValue(potionTypeClass, "HP");
            POTION_TYPE_MANA = getEnumValue(potionTypeClass, "MANA");

            Class<?> gearRarityClass = Class.forName("com.robertx22.mine_and_slash.database.data.rarities.GearRarity");
            RARITY_STAT_PERCENTS_FIELD = gearRarityClass.getField("stat_percents");
            Class<?> minMaxClass = Class.forName("com.robertx22.mine_and_slash.database.data.MinMax");
            STAT_PERCENTS_MAX_FIELD = minMaxClass.getField("max");

            LOGGER.debug("Potion handles initialized: type={}, rarity={}",
                    GET_POTION_TYPE != null, GET_POTION_RARITY != null);
        } catch (Exception e) {
            LOGGER.debug("Potion handles not available: {}", e.getMessage());
        }
    }

    // ========== Config Initialization ==========

    private static void initializeConfigHandles() {
        try {
            Class<?> neatConfigClass = Class.forName("com.robertx22.mine_and_slash.a_libraries.neat.NeatConfig");
            NEAT_CONFIG_DRAW_FIELD = neatConfigClass.getField("draw");
            LOGGER.debug("Config handles initialized: neatConfig={}", NEAT_CONFIG_DRAW_FIELD != null);
        } catch (Exception e) {
            LOGGER.debug("Config handles not available: {}", e.getMessage());
        }
    }

    // ========== Additional Effect Initialization ==========

    private static void initializeAdditionalEffectHandles() {
        try {
            if (exileEffectInstanceDataClass != null) {
                GET_SPELL_ID = lookupFieldGetter(exileEffectInstanceDataClass, "spell_id");
                GET_SELF_CAST = lookupFieldGetter(exileEffectInstanceDataClass, "self_cast");
                GET_CASTER_UUID = lookupFieldGetter(exileEffectInstanceDataClass, "caster_uuid");
            }
            if (exileEffectClass != null) {
                GET_EFFECT_TAGS = lookupFieldGetter(exileEffectClass, "tags");
                try {
                    Class<?> tagListClass = Class.forName("com.robertx22.mine_and_slash.tags.TagList");
                    GET_TAG_LIST_TAGS = lookupFieldGetter(tagListClass, "tags");
                } catch (Exception e) {
                    LOGGER.debug("TagList tags field not available: {}", e.getMessage());
                }
            }
            LOGGER.debug("Additional effect handles initialized: spellId={}, tags={}",
                    GET_SPELL_ID != null, GET_EFFECT_TAGS != null);
        } catch (Exception e) {
            LOGGER.debug("Additional effect handles not available: {}", e.getMessage());
        }
    }

    private static void initializeMercenaryHandles() {
        try {
            try {
                Class<?> mercManagerClass = Class.forName("com.robertx22.mine_and_slash.database.data.mercenary.MercenaryManager");
                GET_SERVER_MERCENARY = lookupMethod(mercManagerClass, "getMerc", Player.class);
            } catch (Throwable ignore) {}

            Class<?> mercEntityClass = Class.forName("com.robertx22.mine_and_slash.database.data.mercenary.entity.MercenaryEntity");
            GET_MERC_CLASS = lookupMethod(mercEntityClass, "getMercClass");

            Class<?> mercClassClass = Class.forName("com.robertx22.mine_and_slash.database.data.mercenary.MercenaryClass");
            GET_MERC_ICON_LOC = lookupMethod(mercClassClass, "getIconLoc");

            GET_MERC_DATA = lookupMethod(mercEntityClass, "getMercData");
            Class<?> mercDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.mercenary.MercenaryData");
            GET_EQUIPPED_SPELL = lookupMethod(mercDataClass, "getEquippedSpell", int.class);

            LOGGER.debug("Mercenary handles initialized: serverMerc={}, mercClass={}, mercData={}",
                    GET_SERVER_MERCENARY != null, GET_MERC_CLASS != null, GET_MERC_DATA != null);
        } catch (Exception e) {
            LOGGER.debug("Mercenary handles not available: {}", e.getMessage());
        }
    }

    // ========== Skill Hotbar Helpers ==========

    public static Object getSpell(Player player, int slot) {
        if (LOAD_PLAYER == null || player == null) return null;
        try {
            Object playerData = LOAD_PLAYER.invoke(player);
            if (playerData == null) return null;
            Object inv = GET_SKILL_GEM_INVENTORY.invoke(playerData);
            if (inv == null) return null;
            Object gem = GET_HOTBAR_GEM.invoke(inv, slot);
            if (gem == null) return null;
            return GET_SPELL.invoke(gem);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get spell at slot {}: {}", slot, t.getMessage());
            return null;
        }
    }

    public static ResourceLocation getSpellIcon(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell != null && GET_SPELL_ICON_LOC != null) {
                return (ResourceLocation) GET_SPELL_ICON_LOC.invoke(spell);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get spell icon at slot {}: {}", slot, t.getMessage());
        }
        return null;
    }

    public static String getSpellGuid(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell != null && GET_SPELL_GUID != null) {
                return (String) GET_SPELL_GUID.invoke(spell);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get spell GUID at slot {}: {}", slot, t.getMessage());
        }
        return null;
    }

    public static float getSpellCooldownPercent(Player player, int slot) {
        if (!isAvailable() || player == null) return 0;
        try {
            String guid = getSpellGuid(player, slot);
            if (guid == null || guid.isEmpty()) return 0;

            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            int current = (int) GET_COOLDOWN_TICKS.invoke(cds, guid);
            int needed = (int) GET_NEEDED_TICKS.invoke(cds, guid);

            if (needed > 0) {
                return Math.min((float) current / needed, 1.0f);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get cooldown at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellCooldownTicks(Player player, int slot) {
        if (!isAvailable() || player == null) return 0;
        try {
            String guid = getSpellGuid(player, slot);
            if (guid == null || guid.isEmpty()) return 0;

            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_COOLDOWN_TICKS.invoke(cds, guid);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get cooldown ticks at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellCooldownSeconds(Player player, int slot) {
        if (!isAvailable() || player == null) return 0;
        try {
            String guid = getSpellGuid(player, slot);
            if (guid == null || guid.isEmpty()) return 0;

            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            int ticks = (int) GET_COOLDOWN_TICKS.invoke(cds, guid);
            return ticks / 20;
        } catch (Throwable t) {
            LOGGER.debug("Failed to get cooldown seconds at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellNeededTicks(Player player, int slot) {
        if (!isAvailable() || player == null) return 0;
        try {
            String guid = getSpellGuid(player, slot);
            if (guid == null || guid.isEmpty()) return 0;

            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_NEEDED_TICKS.invoke(cds, guid);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get needed ticks at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static float getGlobalCooldownPercent(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            int current = (int) GET_COOLDOWN_TICKS.invoke(cds, "global_cooldown");
            int needed = (int) GET_NEEDED_TICKS.invoke(cds, "global_cooldown");

            if (needed > 0 && current > 0) {
                return Math.min((float) current / needed, 1.0f);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get global cooldown: {}", t.getMessage());
        }
        return 0;
    }

    public static int getGlobalCooldownTicks(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_COOLDOWN_TICKS.invoke(cds, "global_cooldown");
        } catch (Throwable t) {
            LOGGER.debug("Failed to get global cooldown ticks: {}", t.getMessage());
        }
        return 0;
    }

    public static int getGlobalCooldownNeededTicks(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_NEEDED_TICKS.invoke(cds, "global_cooldown");
        } catch (Throwable t) {
            LOGGER.debug("Failed to get global cooldown needed ticks: {}", t.getMessage());
        }
        return 0;
    }

    public static final String BLOCK_COOLDOWN_KEY = "block";

    public static boolean isBlockOnCooldown(Player player) {
        if (!isAvailable() || player == null) return false;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return false;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return false;

            if (GET_IS_ON_COOLDOWN != null) {
                return (boolean) GET_IS_ON_COOLDOWN.invoke(cds, blockCooldownKey);
            }
            return (int) GET_COOLDOWN_TICKS.invoke(cds, blockCooldownKey) > 0;
        } catch (Throwable t) {
            LOGGER.debug("Failed to check block cooldown: {}", t.getMessage());
        }
        return false;
    }

    public static int getBlockCooldownTicks(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_COOLDOWN_TICKS.invoke(cds, blockCooldownKey);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get block cooldown ticks: {}", t.getMessage());
        }
        return 0;
    }

    public static int getBlockCooldownNeededTicks(Player player) {
        if (!isAvailable() || player == null) return 0;
        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return 0;
            Object cds = GET_COOLDOWNS.invoke(data);
            if (cds == null) return 0;

            return (int) GET_NEEDED_TICKS.invoke(cds, blockCooldownKey);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get block cooldown needed ticks: {}", t.getMessage());
        }
        return 0;
    }

    public static int getSpellManaCost(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;

            if (SPELL_CONFIG_GETTER != null && MANA_COST_MIN_FIELD != null) {
                try {
                    Object config = SPELL_CONFIG_GETTER.invoke(spell);
                    if (config != null) {
                        java.lang.reflect.Field manaCostField = config.getClass().getField("mana_cost");
                        Object manaCost = manaCostField.get(config);
                        if (manaCost != null) {
                            return (int) MANA_COST_MIN_FIELD.getFloat(manaCost);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mana cost at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellEnergyCost(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;

            if (SPELL_CONFIG_GETTER != null && MANA_COST_MIN_FIELD != null) {
                try {
                    Object config = SPELL_CONFIG_GETTER.invoke(spell);
                    if (config != null) {
                        java.lang.reflect.Field eneCostField = config.getClass().getField("ene_cost");
                        Object eneCost = eneCostField.get(config);
                        if (eneCost != null) {
                            return (int) MANA_COST_MIN_FIELD.getFloat(eneCost);
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get energy cost at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public record HotbarSkillCosts(int totalMana, int totalEnergy) {}

    public static HotbarSkillCosts getTotalHotbarSkillCosts(Player player) {
        if (player == null) return new HotbarSkillCosts(0, 0);
        int totalMana = 0;
        int totalEnergy = 0;
        for (int slot = 0; slot < 16; slot++) {
            totalMana += getSpellManaCost(player, slot);
            totalEnergy += getSpellEnergyCost(player, slot);
        }
        return new HotbarSkillCosts(totalMana, totalEnergy);
    }

    public static int getSummonCount(Player player, int slot) {
        if (LOAD_PLAYER == null || player == null) return 0;
        try {
            String guid = getSpellGuid(player, slot);
            if (guid == null || guid.isEmpty()) return 0;

            Object playerData = LOAD_PLAYER.invoke(player);
            if (playerData == null) return 0;
            Object summonedData = GET_SUMMONED_DATA.invoke(playerData);
            if (summonedData == null) return 0;

            return (int) GET_SUMMONED_AMOUNT.invoke(summonedData, guid);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get summon count at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    /**
     * スペルGUIDからSpellオブジェクトを取得
     */
    public static Object getSpellByGuid(String guid) {
        if (guid == null || exileDBClass == null) return null;
        try {
            if (spellsRegistryMethod == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (spellsRegistryMethod == null) {
                        spellsRegistryMethod = exileDBClass.getMethod("Spells");
                        spellsRegistry = spellsRegistryMethod.invoke(null);
                        if (spellsRegistry != null) {
                            spellsRegistryGetMethod = spellsRegistry.getClass().getMethod("get", String.class);
                        }
                    }
                }
            }
            if (spellsRegistry != null && spellsRegistryGetMethod != null) {
                return spellsRegistryGetMethod.invoke(spellsRegistry, guid);
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get spell by guid {}: {}", guid, t.getMessage());
        }
        return null;
    }

    /**
     * プレイヤーが召喚している全ミニオンの表示情報を取得
     * ホットバー未登録のミニオン（装備プロック・パッシブ・タレント召喚等）も網羅
     */
    public static List<MinionDisplayInfo> getActiveMinions(Player player) {
        if (player == null || LOAD_PLAYER == null) {
            return Collections.emptyList();
        }

        List<MinionDisplayInfo> result = new ArrayList<>();
        try {
            Object playerData = LOAD_PLAYER.invoke(player);
            if (playerData == null) return Collections.emptyList();
            Object summonedData = GET_SUMMONED_DATA.invoke(playerData);
            if (summonedData == null) return Collections.emptyList();

            // 1. SummonedData から全召喚スペルとUUIDマップを取得
            Map<String, List<UUID>> summonedMap = null;
            if (SUMMONED_TYPES_FIELD != null) {
                @SuppressWarnings("unchecked")
                Map<String, List<UUID>> rawMap = (Map<String, List<UUID>>) SUMMONED_TYPES_FIELD.get(summonedData);
                summonedMap = rawMap;
            }

            // UUID -> spellGuid の逆引きマップを構築
            Map<UUID, String> uuidToSpell = new HashMap<>();
            if (summonedMap != null) {
                for (Map.Entry<String, List<UUID>> entry : summonedMap.entrySet()) {
                    String spell = entry.getKey();
                    List<UUID> uuids = entry.getValue();
                    if (spell != null && uuids != null) {
                        for (UUID uuid : uuids) {
                            if (uuid != null) {
                                uuidToSpell.put(uuid, spell);
                            }
                        }
                    }
                }
            }

            // 2. 周囲のミニオンエンティティから最低HP割合を取得
            Map<String, Float> minHpRatioMap = new HashMap<>();
            if (!uuidToSpell.isEmpty() && player.level() != null) {
                net.minecraft.world.phys.AABB searchBox = player.getBoundingBox().inflate(64.0);
                List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox);
                for (LivingEntity living : entities) {
                    if (living.isAlive() && !living.isRemoved()) {
                        String spell = uuidToSpell.get(living.getUUID());
                        if (spell != null) {
                            float curHp = living.getHealth();
                            float maxHp = living.getMaxHealth();
                            float ratio = maxHp > 0.0f ? Math.max(0.0f, Math.min(1.0f, curHp / maxHp)) : 1.0f;
                            minHpRatioMap.compute(spell, (k, v) -> v == null ? ratio : Math.min(v, ratio));
                        }
                    }
                }
            }

            // 3. MinionDisplayInfo のリストを生成
            if (summonedMap != null) {
                for (Map.Entry<String, List<UUID>> entry : summonedMap.entrySet()) {
                    String spellGuid = entry.getKey();
                    List<UUID> uuids = entry.getValue();
                    int count = uuids != null ? uuids.size() : 0;
                    if (count > 0 && spellGuid != null && !spellGuid.isEmpty()) {
                        Object spell = getSpellByGuid(spellGuid);
                        ResourceLocation icon = null;
                        String spellName = spellGuid;
                        if (spell != null && GET_SPELL_ICON_LOC != null) {
                            try {
                                icon = (ResourceLocation) GET_SPELL_ICON_LOC.invoke(spell);
                            } catch (Throwable ignored) {}
                        }

                        float healthRatio = minHpRatioMap.getOrDefault(spellGuid, 1.0f);

                        result.add(MinionDisplayInfo.of(
                                spellGuid,
                                spellName,
                                icon,
                                count,
                                -1,
                                -1,
                                true,
                                healthRatio
                        ));
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get active minions: {}", t.getMessage());
        }

        return result;
    }

    public static boolean getSpellUsesCharges(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell == null || SPELL_CONFIG_GETTER == null || SPELL_CONFIG_CHARGES_FIELD == null) return false;
            Object config = SPELL_CONFIG_GETTER.invoke(spell);
            if (config == null) return false;
            return SPELL_CONFIG_CHARGES_FIELD.getInt(config) > 0;
        } catch (Throwable t) {
            LOGGER.debug("Failed to check charges at slot {}: {}", slot, t.getMessage());
        }
        return false;
    }

    public static int getSpellCharges(Player player, int slot) {
        if (LOAD_PLAYER == null || player == null) return 0;
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;
            Object config = SPELL_CONFIG_GETTER.invoke(spell);
            if (config == null) return 0;
            String chargeName = (String) SPELL_CONFIG_CHARGE_NAME_FIELD.get(config);
            if (chargeName == null || chargeName.isEmpty()) return 0;

            Object playerData = LOAD_PLAYER.invoke(player);
            if (playerData == null) return 0;
            Object spellCastingData = SPELL_CASTING_DATA_FIELD.get(playerData);
            if (spellCastingData == null) return 0;
            Object chargeData = CHARGES_OBJ_FIELD.get(spellCastingData);
            if (chargeData == null) return 0;

            return (int) GET_CHARGES.invoke(chargeData, chargeName);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get charges at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellMaxCharges(Player player, int slot) {
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;
            Object config = SPELL_CONFIG_GETTER.invoke(spell);
            if (config == null) return 0;
            return SPELL_CONFIG_CHARGES_FIELD.getInt(config);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get max charges at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static float getSpellChargeRegenPercent(Player player, int slot) {
        if (LOAD_PLAYER == null || player == null) return 0;
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;
            Object config = SPELL_CONFIG_GETTER.invoke(spell);
            if (config == null) return 0;
            String chargeName = (String) SPELL_CONFIG_CHARGE_NAME_FIELD.get(config);
            int chargeRegen = SPELL_CONFIG_CHARGE_REGEN_FIELD.getInt(config);
            if (chargeName == null || chargeName.isEmpty() || chargeRegen <= 0) return 0;

            Object playerData = LOAD_PLAYER.invoke(player);
            if (playerData == null) return 0;
            Object spellCastingData = SPELL_CASTING_DATA_FIELD.get(playerData);
            if (spellCastingData == null) return 0;
            Object chargeData = CHARGES_OBJ_FIELD.get(spellCastingData);
            if (chargeData == null) return 0;

            int currentTicks = (int) GET_CURRENT_TICKS_CHARGING.invoke(chargeData, chargeName);
            // currentTicks は「回復完了までの残りTick数」であるため、そのまま割合にする
            return Math.max(0, Math.min((float) currentTicks / chargeRegen, 1.0f));
        } catch (Throwable t) {
            LOGGER.debug("Failed to get charge regen at slot {}: {}", slot, t.getMessage());
        }
        return 0;
    }

    public static int getSpellChargeRegenTicks(Player player, int slot) {
        if (LOAD_PLAYER == null || player == null) return 0;
        try {
            Object spell = getSpell(player, slot);
            if (spell == null) return 0;
            Object config = SPELL_CONFIG_GETTER.invoke(spell);
            if (config == null) return 0;
            return SPELL_CONFIG_CHARGE_REGEN_FIELD.getInt(config);
        } catch (Throwable t) {
            return 0;
        }
    }

    public static boolean isOnSecondHotbar() {
        if (IS_ON_SECOND_HOTBAR_FIELD == null) {
            try {
                Class<?> cls = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
                try {
                    IS_ON_SECOND_HOTBAR_FIELD = cls.getField("IS_ON_SECONd_HOTBAR");
                } catch (NoSuchFieldException e) {
                    IS_ON_SECOND_HOTBAR_FIELD = cls.getField("IS_ON_SECOND_HOTBAR");
                }
            } catch (Exception ignored) {
            }
        }
        if (IS_ON_SECOND_HOTBAR_FIELD == null) return false;
        try {
            return IS_ON_SECOND_HOTBAR_FIELD.getBoolean(null);
        } catch (Exception e) {
            return false;
        }
    }

    public static void setOnSecondHotbar(boolean value) {
        if (IS_ON_SECOND_HOTBAR_FIELD == null) {
            try {
                Class<?> cls = Class.forName("com.robertx22.mine_and_slash.mmorpg.registers.client.SpellKeybind");
                try {
                    IS_ON_SECOND_HOTBAR_FIELD = cls.getField("IS_ON_SECONd_HOTBAR");
                } catch (NoSuchFieldException e) {
                    IS_ON_SECOND_HOTBAR_FIELD = cls.getField("IS_ON_SECOND_HOTBAR");
                }
            } catch (Exception ignored) {
            }
        }
        if (IS_ON_SECOND_HOTBAR_FIELD != null) {
            try {
                IS_ON_SECOND_HOTBAR_FIELD.setBoolean(null, value);
            } catch (Exception ignored) {
            }
        }
    }

    private static long lastHotbarSwapCheckTime = 0;
    private static boolean cachedHotbarSwappingEnabled = false;
    private static final long HOTBAR_SWAP_CHECK_INTERVAL_MS = 1000;

    public static boolean isHotbarSwappingEnabled() {
        long now = System.currentTimeMillis();
        if (now - lastHotbarSwapCheckTime < HOTBAR_SWAP_CHECK_INTERVAL_MS) {
            return cachedHotbarSwappingEnabled;
        }
        lastHotbarSwapCheckTime = now;
        try {
            if (CLIENT_CONFIG_GET_METHOD == null) {
                Class<?> clientConfigsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
                CLIENT_CONFIG_GET_METHOD = clientConfigsClass.getMethod("getConfig");
            }
            if (CLIENT_CONFIG_GET_METHOD != null) {
                Object config = CLIENT_CONFIG_GET_METHOD.invoke(null);
                if (config != null) {
                    if (HOTBAR_SWAPPING_FIELD == null) {
                        HOTBAR_SWAPPING_FIELD = config.getClass().getField("HOTBAR_SWAPPING");
                    }
                    Object booleanValue = HOTBAR_SWAPPING_FIELD.get(config);
                    if (booleanValue != null) {
                        if (BOOLEAN_VALUE_GET_METHOD == null) {
                            BOOLEAN_VALUE_GET_METHOD = booleanValue.getClass().getMethod("get");
                        }
                        cachedHotbarSwappingEnabled = (Boolean) BOOLEAN_VALUE_GET_METHOD.invoke(booleanValue);
                        return cachedHotbarSwappingEnabled;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to check HOTBAR_SWAPPING config: {}", e.getMessage());
        }
        return cachedHotbarSwappingEnabled;
    }

    public static boolean setHotbarSwappingEnabled(boolean enabled) {
        try {
            if (CLIENT_CONFIG_GET_METHOD == null) {
                Class<?> clientConfigsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
                CLIENT_CONFIG_GET_METHOD = clientConfigsClass.getMethod("getConfig");
            }
            if (CLIENT_CONFIG_GET_METHOD != null) {
                Object config = CLIENT_CONFIG_GET_METHOD.invoke(null);
                if (config != null) {
                    if (HOTBAR_SWAPPING_FIELD == null) {
                        HOTBAR_SWAPPING_FIELD = config.getClass().getField("HOTBAR_SWAPPING");
                    }
                    Object booleanValue = HOTBAR_SWAPPING_FIELD.get(config);
                    if (booleanValue != null) {
                        java.lang.reflect.Method setMethod = booleanValue.getClass().getMethod("set", Object.class);
                        setMethod.invoke(booleanValue, enabled);
                        cachedHotbarSwappingEnabled = enabled;
                        lastHotbarSwapCheckTime = System.currentTimeMillis();

                        if (!enabled) {
                            setOnSecondHotbar(false);
                        }

                        try {
                            Class<?> clientConfigsClass = Class.forName("com.robertx22.mine_and_slash.config.forge.ClientConfigs");
                            java.lang.reflect.Field clientSpecField = clientConfigsClass.getField("clientSpec");
                            Object clientSpec = clientSpecField.get(null);
                            if (clientSpec != null) {
                                java.lang.reflect.Method saveMethod = clientSpec.getClass().getMethod("save");
                                saveMethod.invoke(clientSpec);
                            }
                        } catch (Throwable ignored) {
                        }
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to set HOTBAR_SWAPPING: {}", t.getMessage());
        }
        return false;
    }

    // ========== Potion Helpers ==========

    public static boolean isSlashPotionItem(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty() || slashPotionItemClass == null) return false;
        return slashPotionItemClass.isInstance(stack.getItem());
    }

    public static boolean isHpPotion(net.minecraft.world.item.ItemStack stack) {
        if (!isSlashPotionItem(stack) || GET_POTION_TYPE == null) return false;
        try {
            Object type = GET_POTION_TYPE.invoke(stack.getItem());
            return type == POTION_TYPE_HP;
        } catch (Throwable t) {
            return false;
        }
    }

    public static float getPotionRarityPriority(net.minecraft.world.item.ItemStack stack) {
        if (!isSlashPotionItem(stack) || GET_POTION_RARITY == null) return 0;
        try {
            Object rarity = GET_POTION_RARITY.invoke(stack.getItem());
            if (rarity != null && RARITY_STAT_PERCENTS_FIELD != null && STAT_PERCENTS_MAX_FIELD != null) {
                Object statPercents = RARITY_STAT_PERCENTS_FIELD.get(rarity);
                if (statPercents != null) {
                    return ((Number) STAT_PERCENTS_MAX_FIELD.get(statPercents)).floatValue();
                }
            }
        } catch (Throwable e) {
            LOGGER.debug("Failed to get potion rarity: {}", e.getMessage());
        }
        return 0;
    }

    public static net.minecraft.world.item.ItemStack findBestPotion(Player player, boolean isHp) {
        if (!isAvailable() || player == null) return net.minecraft.world.item.ItemStack.EMPTY;

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

    public static boolean isPotionOnCooldown(Player player, net.minecraft.world.item.ItemStack stack) {
        if (player == null || stack.isEmpty()) return false;
        return player.getCooldowns().isOnCooldown(stack.getItem());
    }

    // ========== Config Helpers ==========

    public static boolean setNeatHpBarEnabled(boolean enabled) {
        if (NEAT_CONFIG_DRAW_FIELD == null) return false;
        try {
            NEAT_CONFIG_DRAW_FIELD.setBoolean(null, enabled);
            LOGGER.info("Set M&S Neat HP Bar to: {}", enabled);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to set Neat HP Bar: {}", e.getMessage());
            return false;
        }
    }

    // ========== Entity Level Helper ==========

    public static int getEntityLevel(net.minecraft.world.entity.LivingEntity entity) {
        if (!isAvailable() || entity == null) return -1;
        try {
            Object entityData = LOAD_UNIT.invoke(entity);
            if (entityData != null && GET_LEVEL != null) {
                Object result = GET_LEVEL.invoke(entityData);
                return result instanceof Number ? ((Number) result).intValue() : -1;
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get entity level: {}", t.getMessage());
        }
        return -1;
    }

    // ========== Player ExileEffect Helpers ==========

    public static java.util.List<com.example.exile_overlay.api.data.ExileEffectInfo> getPlayerExileEffects(Player player) {
        java.util.List<com.example.exile_overlay.api.data.ExileEffectInfo> result = new java.util.ArrayList<>();
        if (!isAvailable() || player == null) return result;

        try {
            Object data = LOAD_UNIT.invoke(player);
            if (data == null) return result;

            Object statusData = getStatusEffectsData(data);
            if (statusData == null) return result;

            java.util.Map<String, Object> exileMap = getExileEffectMap(statusData);
            if (exileMap.isEmpty()) return result;

            for (java.util.Map.Entry<String, Object> entry : exileMap.entrySet()) {
                String effectId = entry.getKey();
                Object instanceData = entry.getValue();
                if (instanceData == null) continue;

                try {
                    if (shouldEffectRemove(instanceData)) continue;

                    int ticksLeft = getEffectTicksLeft(instanceData);
                    int stacks = getEffectStacks(instanceData);
                    boolean isInfinite = isEffectInfinite(instanceData);
                    String durationText = getEffectDurationString(instanceData);

                    String spellId = "";
                    if (GET_SPELL_ID != null) {
                        try { spellId = (String) GET_SPELL_ID.invoke(instanceData); } catch (Throwable ignored) {}
                        if (spellId == null) spellId = "";
                    }
                    boolean selfCast = false;
                    if (GET_SELF_CAST != null) {
                        try { selfCast = (boolean) GET_SELF_CAST.invoke(instanceData); } catch (Throwable ignored) {}
                    }
                    String casterUuid = "";
                    if (GET_CASTER_UUID != null) {
                        try { casterUuid = (String) GET_CASTER_UUID.invoke(instanceData); } catch (Throwable ignored) {}
                        if (casterUuid == null) casterUuid = "";
                    }

                    Object exileEffect = getExileEffectFromDB(effectId);
                    if (exileEffect == null) continue;

                    ResourceLocation texture = getExileEffectTexture(exileEffect);
                    String name = getExileEffectName(exileEffect);
                    boolean isNegative = isEffectNegative(exileEffect);
                    boolean isBeneficial = !isNegative;

                    java.util.Set<String> effectTags = java.util.Collections.emptySet();
                    if (GET_EFFECT_TAGS != null) {
                        try {
                            Object tagList = GET_EFFECT_TAGS.invoke(exileEffect);
                            if (tagList != null && GET_TAG_LIST_TAGS != null) {
                                @SuppressWarnings("unchecked")
                                java.util.Set<String> rawTags = (java.util.Set<String>) GET_TAG_LIST_TAGS.invoke(tagList);
                                if (rawTags != null) {
                                    effectTags = java.util.Collections.unmodifiableSet(new java.util.HashSet<>(rawTags));
                                }
                            }
                        } catch (Throwable ignored) {
                        }
                    }

                    result.add(new com.example.exile_overlay.api.data.ExileEffectInfo(
                            effectId, name, texture, ticksLeft, stacks,
                            isBeneficial, isNegative, isInfinite, durationText,
                            spellId, selfCast, casterUuid, effectTags));
                } catch (Exception inner) {
                    LOGGER.debug("Failed to process effect {}: {}", effectId, inner.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to get player ExileEffects", t);
        }
        return result;
    }

    // ========== Mob Info High-Level Helpers ==========

    public static com.example.exile_overlay.api.data.MobRarityInfo getMobRarityInfo(net.minecraft.world.entity.LivingEntity entity) {
        if (!isAvailable() || entity == null) return null;
        try {
            Object entityData = getEntityData(entity);
            if (entityData == null) return null;
            Object mobRarity = getMobRarityObj(entityData);
            if (mobRarity == null) return null;
            String rarityId = getRarityString(entityData);
            int color = getRarityColor(mobRarity);
            boolean elite = isRarityElite(mobRarity);
            boolean special = isRaritySpecial(mobRarity);
            return new com.example.exile_overlay.api.data.MobRarityInfo(
                    rarityId != null ? rarityId : "common", color, elite, special);
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob rarity: {}", t.getMessage());
            return null;
        }
    }

    public static java.util.List<com.example.exile_overlay.api.data.MobAffixInfo> getMobAffixesInfo(net.minecraft.world.entity.LivingEntity entity) {
        java.util.List<com.example.exile_overlay.api.data.MobAffixInfo> result = new java.util.ArrayList<>();
        if (!isAvailable() || entity == null) return result;
        try {
            Object entityData = getEntityData(entity);
            if (entityData == null) return result;
            java.util.List<Object> affixObjs = getMobAffixObjects(entityData);
            for (Object affix : affixObjs) {
                String locName = getAffixLocName(affix);
                String icon = getAffixIcon(affix);
                boolean isPrefix = isAffixPrefix(affix);
                java.util.List<com.example.exile_overlay.api.data.AffixStatInfo> statInfos = getAffixStatInfosInternal(affix);
                result.add(new com.example.exile_overlay.api.data.MobAffixInfo(locName, icon, statInfos, isPrefix));
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob affixes: {}", t.getMessage());
        }
        return result;
    }

    private static java.util.List<com.example.exile_overlay.api.data.AffixStatInfo> getAffixStatInfosInternal(Object affix) {
        java.util.List<com.example.exile_overlay.api.data.AffixStatInfo> result = new java.util.ArrayList<>();
        try {
            java.util.List<Object> statMods = getAffixStatsList(affix);
            for (Object statMod : statMods) {
                try {
                    float min = getStatModMin(statMod);
                    String statGuid = getStatModStatGuid(statMod);
                    String modTypeStr = getStatModType(statMod);
                    if (statGuid == null) continue;

                    Object stat = getStatFromRegistry(statGuid);
                    String statName = stat != null ? getStatLocName(stat) : statGuid;
                    boolean statIsPercent = stat != null && getStatIsPercent(stat);
                    boolean modIsPercent = "PERCENT".equals(modTypeStr) || "MORE".equals(modTypeStr);

                    result.add(new com.example.exile_overlay.api.data.AffixStatInfo(min, statName, statIsPercent || modIsPercent));
                } catch (Exception inner) {
                    LOGGER.debug("Failed to process affix stat: {}", inner.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get affix stat infos: {}", e.getMessage());
        }
        return result;
    }

    // ========== Effect Timer Helpers ==========

    private static int calcDisplayTicks(String cacheKey, int syncedTicksLeft) {
        long now = System.currentTimeMillis();
        long[] entry = effectTimerCache.computeIfAbsent(cacheKey, k -> new long[]{syncedTicksLeft, now, now});
        if (entry[0] != syncedTicksLeft) {
            entry[0] = syncedTicksLeft;
            entry[1] = now;
        }
        entry[2] = now; // 最終アクセス時刻を更新
        long elapsedTicks = (now - entry[1]) / 50;
        return Math.max(0, (int) (syncedTicksLeft - elapsedTicks));
    }

    private static void cleanupEffectTimers() {
        long now = System.currentTimeMillis();
        if (now - lastEffectCleanup < 30000) return;
        lastEffectCleanup = now;
        effectTimerCache.entrySet().removeIf(e -> {
            long[] val = e.getValue();
            long elapsedTicks = (now - val[1]) / 50;
            boolean expired = val[0] - elapsedTicks <= 0;
            boolean unaccessed = val.length > 2 && (now - val[2] > 60000); // 60秒参照されなかったエントリを破棄
            return expired || unaccessed;
        });
    }

    public static java.util.List<com.example.exile_overlay.api.data.MobEffectInfo> getMobStatusEffectsInfo(net.minecraft.world.entity.LivingEntity entity) {
        java.util.List<com.example.exile_overlay.api.data.MobEffectInfo> result = new java.util.ArrayList<>();
        if (!isAvailable() || entity == null) return result;

        cleanupEffectTimers();

        try {
            Object entityData = getEntityData(entity);
            if (entityData == null) return result;
            Object statusData = getStatusEffectsData(entityData);
            if (statusData == null) return result;

            java.util.Map<String, Object> exileMap = getExileEffectMap(statusData);

            for (java.util.Map.Entry<String, Object> entry : exileMap.entrySet()) {
                String effectId = entry.getKey();
                Object instanceData = entry.getValue();
                if (instanceData == null) continue;
                if (shouldEffectRemove(instanceData)) continue;

                try {
                    int ticksLeft = getEffectTicksLeft(instanceData);
                    int stacks = getEffectStacks(instanceData);
                    boolean isInfinite = isEffectInfinite(instanceData);

                    Object exileEffect = getExileEffectFromDB(effectId);
                    if (exileEffect == null) continue;

                    String name = getExileEffectName(exileEffect);
                    ResourceLocation texture = getExileEffectTexture(exileEffect);
                    boolean isNegative = isEffectNegative(exileEffect);

                    String cacheKey = entity.getUUID() + ":" + effectId;
                    int displayTicks = isInfinite ? ticksLeft : calcDisplayTicks(cacheKey, ticksLeft);

                    result.add(new com.example.exile_overlay.api.data.MobEffectInfo(
                            effectId, name, texture, displayTicks, stacks, isInfinite, isNegative));
                } catch (Exception inner) {
                    LOGGER.debug("Failed to process mob effect {}: {}", effectId, inner.getMessage());
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get mob status effects: {}", t.getMessage());
        }
        return result;
    }

    // === Profession Integration Methods ===

    public static Object loadPlayer(Player player) {
        if (LOAD_PLAYER == null || player == null) return null;
        try {
            return LOAD_PLAYER.invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getPlayerProfessions(Object playerData) {
        if (GET_PROFESSIONS_FIELD == null || playerData == null) return null;
        try {
            return GET_PROFESSIONS_FIELD.invoke(playerData);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int getProfessionExp(Object profData, String profId) {
        if (GET_PROF_EXP == null || profData == null || profId == null) return 0;
        try {
            Object result = GET_PROF_EXP.invoke(profData, profId);
            return result instanceof Number ? ((Number) result).intValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static int getProfessionMaxExp(Object profData, String profId) {
        if (GET_PROF_MAX_EXP == null || profData == null || profId == null) return 1;
        try {
            Object result = GET_PROF_MAX_EXP.invoke(profData, profId);
            return result instanceof Number ? ((Number) result).intValue() : 1;
        } catch (Throwable t) {
            return 1;
        }
    }

    public static int getProfessionLevel(Object profData, String profId) {
        if (GET_PROF_LEVEL == null || profData == null || profId == null) return 1;
        try {
            Object result = GET_PROF_LEVEL.invoke(profData, profId);
            return result instanceof Number ? ((Number) result).intValue() : 1;
        } catch (Throwable t) {
            return 1;
        }
    }

    /**
     * エンティティがMine and Slashの毒(Ailments.POISON)にかかっているかを判定
     */
    public static boolean isEntityPoisoned(LivingEntity entity) {
        if (entity == null || GET_AILMENT_DATA == null || GET_DOT_MAP == null) {
            return false;
        }
        try {
            Object entityData = getEntityData(entity);
            if (entityData == null) return false;
            Object ailmentData = GET_AILMENT_DATA.invoke(entityData);
            if (ailmentData == null) return false;
            Object datasObj = GET_DOT_MAP.invoke(ailmentData);
            if (datasObj instanceof Map<?, ?> datasMap) {
                for (Object oneData : datasMap.values()) {
                    if (oneData != null) {
                        try {
                            java.lang.reflect.Field dotMapField = oneData.getClass().getField("dotMap");
                            Object dotMapObj = dotMapField.get(oneData);
                            if (dotMapObj instanceof Map<?, ?> dotMap) {
                                Object poisonList = dotMap.get("poison");
                                if (poisonList instanceof List<?> list && !list.isEmpty()) {
                                    return true;
                                }
                            }
                        } catch (Throwable ignore) {}
                    }
                }
            }
        } catch (Throwable t) {
            // non-critical
        }
        return false;
    }

    /**
     * エンティティがMine and Slashの出血(Ailments.BLEED)にかかっているかを判定
     */
    public static boolean isEntityBleeding(LivingEntity entity) {
        if (entity == null || GET_AILMENT_DATA == null || GET_DOT_MAP == null) {
            return false;
        }
        try {
            Object entityData = getEntityData(entity);
            if (entityData == null) return false;
            Object ailmentData = GET_AILMENT_DATA.invoke(entityData);
            if (ailmentData == null) return false;
            Object datasObj = GET_DOT_MAP.invoke(ailmentData);
            if (datasObj instanceof Map<?, ?> datasMap) {
                for (Object oneData : datasMap.values()) {
                    if (oneData != null) {
                        try {
                            java.lang.reflect.Field dotMapField = oneData.getClass().getField("dotMap");
                            Object dotMapObj = dotMapField.get(oneData);
                            if (dotMapObj instanceof Map<?, ?> dotMap) {
                                Object bleedList = dotMap.get("bleed");
                                if (bleedList instanceof List<?> list && !list.isEmpty()) {
                                    return true;
                                }
                            }
                        } catch (Throwable ignore) {}
                    }
                }
            }
        } catch (Throwable t) {
            // non-critical
        }
        return false;
    }

    /**
     * スペルGUIDからスペルアイコンを取得
     */
    public static ResourceLocation getSpellIconByGuid(String guid) {
        if (guid == null || guid.isEmpty()) return null;
        try {
            Object spell = getSpellByGuid(guid);
            if (spell != null && GET_SPELL_ICON_LOC != null) {
                return (ResourceLocation) GET_SPELL_ICON_LOC.invoke(spell);
            }
        } catch (Throwable ignore) {}
        return null;
    }

    /**
     * 傭兵クラスIDからアイコンを取得
     */
    public static ResourceLocation getMercClassIconById(String classId) {
        if (classId == null || classId.isEmpty() || exileDBClass == null) return MERC_ICON_FALLBACK;
        try {
            if (mercRegistryMethod == null) {
                synchronized (MethodHandlesUtil.class) {
                    if (mercRegistryMethod == null) {
                        mercRegistryMethod = exileDBClass.getMethod("Mercenaries");
                        mercRegistry = mercRegistryMethod.invoke(null);
                        if (mercRegistry != null) {
                            mercRegistryGetMethod = mercRegistry.getClass().getMethod("get", String.class);
                        }
                    }
                }
            }
            if (mercRegistry != null && mercRegistryGetMethod != null) {
                Object mercClass = mercRegistryGetMethod.invoke(mercRegistry, classId);
                if (mercClass != null && GET_MERC_ICON_LOC != null) {
                    ResourceLocation loc = (ResourceLocation) GET_MERC_ICON_LOC.invoke(mercClass);
                    if (loc != null) return loc;
                }
            }
        } catch (Throwable ignore) {}
        return MERC_ICON_FALLBACK;
    }

    /**
     * サーバーサイドでプレイヤーのアクティブ傭兵を取得
     */
    public static LivingEntity getServerMercenary(Player player) {
        if (!isAvailable() || player == null) return null;
        try {
            if (GET_SERVER_MERCENARY != null) {
                Object result = GET_SERVER_MERCENARY.invoke(player);
                if (result instanceof LivingEntity living && living.isAlive()) {
                    return living;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Failed to get server mercenary via MercenaryManager: {}", t.getMessage());
        }
        return null;
    }

    /**
     * サーバー側で傭兵同期パケットを作成
     */
    public static MercenarySyncS2C createMercenarySyncPacket(ServerPlayer player) {
        if (player == null || !isAvailable()) return null;
        try {
            LivingEntity merc = getServerMercenary(player);
            if (merc == null || !merc.isAlive()) {
                return new MercenarySyncS2C();
            }

            String classId = "";
            if (GET_MERC_CLASS != null) {
                Object mercClass = GET_MERC_CLASS.invoke(merc);
                if (mercClass != null) {
                    try {
                        Method idMethod = mercClass.getClass().getMethod("GUID");
                        classId = (String) idMethod.invoke(mercClass);
                    } catch (Throwable ignore) {}
                }
            }
            if (classId.isEmpty()) {
                try {
                    Method getClassId = merc.getClass().getMethod("getClassId");
                    classId = (String) getClassId.invoke(merc);
                } catch (Throwable ignore) {}
            }

            String name = merc.getName() != null ? merc.getName().getString() : "Mercenary";
            float health = 0;
            float maxHealth = 0;
            float energyShield = 0;
            float maxEnergyShield = 0;
            int level = 1;
            Object cooldowns = null;

            try {
                health = getCurrentHealth(merc);
                maxHealth = getMaxHealth(merc);
                energyShield = getCurrentMagicShield(merc);
            } catch (Throwable ignore) {}

            if (LOAD_UNIT != null) {
                Object entityData = LOAD_UNIT.invoke(merc);
                if (entityData != null) {
                    if (GET_LEVEL != null) {
                        try {
                            level = getLevel(entityData);
                        } catch (Throwable ignore) {}
                    }

                    try {
                        if (MAGIC_SHIELD_TYPE != null && GET_MAXIMUM_RESOURCE != null) {
                            maxEnergyShield = getMaximumResource(entityData, MAGIC_SHIELD_TYPE);
                        }
                    } catch (Throwable ignore) {}

                    if (GET_COOLDOWNS != null) {
                        cooldowns = GET_COOLDOWNS.invoke(entityData);
                    }
                }
            }

            List<MercenarySyncS2C.SkillData> skillList = new ArrayList<>();
            if (GET_MERC_DATA != null && GET_EQUIPPED_SPELL != null) {
                Object mercData = GET_MERC_DATA.invoke(merc);
                if (mercData != null) {
                    for (int i = 0; i < 4; i++) {
                        Object spell = GET_EQUIPPED_SPELL.invoke(mercData, i);
                        if (spell != null) {
                            String spellId = "";
                            if (GET_SPELL_GUID != null) {
                                spellId = (String) GET_SPELL_GUID.invoke(spell);
                            }
                            boolean onCooldown = false;
                            float progress = 0.0f;
                            int remainingTicks = 0;
                            int totalTicks = 0;

                            if (cooldowns != null && spellId != null && !spellId.isEmpty()) {
                                if (GET_COOLDOWN_TICKS != null && GET_NEEDED_TICKS != null) {
                                    int spellLeft = (int) GET_COOLDOWN_TICKS.invoke(cooldowns, spellId);
                                    int spellNeed = (int) GET_NEEDED_TICKS.invoke(cooldowns, spellId);
                                    int gcdLeft = (int) GET_COOLDOWN_TICKS.invoke(cooldowns, "global_cooldown");
                                    int gcdNeed = (int) GET_NEEDED_TICKS.invoke(cooldowns, "global_cooldown");

                                    if (spellLeft > 0) {
                                        remainingTicks = spellLeft;
                                        totalTicks = spellNeed > 0 ? spellNeed : spellLeft;
                                    } else if (gcdLeft > 0) {
                                        remainingTicks = gcdLeft;
                                        totalTicks = gcdNeed > 0 ? gcdNeed : gcdLeft;
                                    }

                                    if (remainingTicks > 0) {
                                        onCooldown = true;
                                        progress = totalTicks > 0 ? (float) remainingTicks / (float) totalTicks : 1.0f;
                                    }
                                }
                            }
                            skillList.add(new MercenarySyncS2C.SkillData(spellId, onCooldown, progress, remainingTicks, totalTicks));
                        }
                    }
                }
            }

            return new MercenarySyncS2C(classId, name, level, health, maxHealth, energyShield, maxEnergyShield, skillList);
        } catch (Throwable t) {
            LOGGER.debug("Failed to create mercenary sync packet: {}", t.getMessage());
            return null;
        }
    }

    /**
     * 現在召喚中の自傭兵情報を取得
     * サーバー同期パケットのキャッシュから解決
     */
    public static MercenaryDisplayInfo getActiveMercenary(Player player) {
        if (!isAvailable() || player == null) return null;
        return MercenaryClientCache.get();
    }
}
