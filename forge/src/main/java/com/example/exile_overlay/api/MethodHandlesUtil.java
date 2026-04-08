package com.example.exile_overlay.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * MethodHandlesを使用した高速リフレクションアクセスユーティリティ
 * 
 * 【パフォーマンス最適化】
 * - MethodHandleは通常のリフレクションより10-100倍高速
 * - 一度初期化したら再利用可能
 * - JVMのJIT最適化が効く
 * 
 * 【スレッド安全性】
 * - MethodHandleは不変でスレッド安全
 * - 複数スレッドから安全に呼び出し可能
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
    private static MethodHandle GET_LAST_DAMAGE_TAKEN = null;
    private static MethodHandle GET_DAMAGE_EVENT_ELEMENT = null;
    private static MethodHandle GET_ELEMENTS_FORMAT = null;

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
    private static MethodHandle GET_STATUS_EFFECTS_DATA = null;
    private static MethodHandle EXILE_EFFECTS_GET = null;

    // === ResourceType enum values ===
    private static Object MANA_TYPE = null;
    private static Object MAGIC_SHIELD_TYPE = null;
    private static Object ENERGY_TYPE = null;
    private static Object BLOOD_TYPE = null;
    
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
            
            GET_LAST_DAMAGE_TAKEN = lookupFieldGetter(entityDataClass, "lastDamageTaken");
            GET_DAMAGE_EVENT_ELEMENT = lookupMethod(damageEventClass, "getElement");
            GET_ELEMENTS_FORMAT = lookupFieldGetter(elementsClass, "format");
            
            MANA_TYPE = getEnumValue(resourceTypeClass, "mana");
            MAGIC_SHIELD_TYPE = getEnumValue(resourceTypeClass, "magic_shield");
            ENERGY_TYPE = getEnumValue(resourceTypeClass, "energy");
            BLOOD_TYPE = getEnumValue(resourceTypeClass, "blood");
            
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

            GET_STATUS_EFFECTS_DATA = lookupMethod(entityDataClass, "getStatusEffectsData");
            EXILE_EFFECTS_GET = lookupMethod(exileDBClass, "ExileEffects");

            LOGGER.debug("Mob info MethodHandles initialized: rarity={}, affix={}, status={}",
                    GET_MOB_RARITY != null, GET_AFFIX_DATA != null, GET_STATUS_EFFECTS_DATA != null);
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
        if (GET_CURRENT_HEALTH == null || entity == null) return 0f;
        Object result = GET_CURRENT_HEALTH.invoke(entity);
        // M&SのgetCurrentHealthはintを返すため、Numberとして扱う
        return result instanceof Number ? ((Number) result).floatValue() : 0f;
    }

    public static float getMaxHealth(LivingEntity entity) throws Throwable {
        if (GET_MAX_HEALTH == null || entity == null) return 1f;
        Object result = GET_MAX_HEALTH.invoke(entity);
        return result instanceof Number ? ((Number) result).floatValue() : 1f;
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
            case "LIGHT_PURPLE" -> 0xFF55FF;  // Elemental/All - 薄紫
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
            return locName != null ? locName.toString() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    public static Object getStatusEffectsData(Object entityData) throws Throwable {
        if (GET_STATUS_EFFECTS_DATA == null || entityData == null) return null;
        return GET_STATUS_EFFECTS_DATA.invoke(entityData);
    }

    public static java.util.Map<String, Object> getExileEffectMap(Object statusEffectsData) throws Throwable {
        if (statusEffectsData == null) return java.util.Collections.emptyMap();
        try {
            java.lang.reflect.Field exileMapField = statusEffectsData.getClass().getField("exileMap");
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) exileMapField.get(statusEffectsData);
            return map != null ? map : java.util.Collections.emptyMap();
        } catch (Exception e) {
            return java.util.Collections.emptyMap();
        }
    }

    public static int getEffectTicksLeft(Object instanceData) {
        if (instanceData == null) return 0;
        try {
            java.lang.reflect.Field f = instanceData.getClass().getField("ticks_left");
            return f.getInt(instanceData);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getEffectStacks(Object instanceData) {
        if (instanceData == null) return 0;
        try {
            java.lang.reflect.Field f = instanceData.getClass().getField("stacks");
            return f.getInt(instanceData);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isEffectInfinite(Object instanceData) {
        if (instanceData == null) return false;
        try {
            java.lang.reflect.Field f = instanceData.getClass().getField("is_infinite");
            return f.getBoolean(instanceData);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldEffectRemove(Object instanceData) {
        if (instanceData == null) return true;
        try {
            java.lang.reflect.Method m = instanceData.getClass().getMethod("shouldRemove");
            return (boolean) m.invoke(instanceData);
        } catch (Exception e) {
            return true;
        }
    }

    public static Object getExileEffectFromDB(String effectId) {
        if (exileDBClass == null || effectId == null) return null;
        try {
            if (EXILE_EFFECTS_GET == null) {
                EXILE_EFFECTS_GET = lookupMethod(exileDBClass, "ExileEffects");
            }
            Object registry = EXILE_EFFECTS_GET.invoke(null);
            if (registry == null) return null;
            java.lang.reflect.Method getMethod = registry.getClass().getMethod("get", String.class);
            return getMethod.invoke(registry, effectId);
        } catch (Throwable t) {
            return null;
        }
    }

    public static ResourceLocation getExileEffectTexture(Object exileEffect) {
        if (exileEffect == null) return null;
        try {
            java.lang.reflect.Method m = exileEffect.getClass().getMethod("getTexture");
            return (ResourceLocation) m.invoke(exileEffect);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getExileEffectName(Object exileEffect) {
        if (exileEffect == null) return "";
        try {
            java.lang.reflect.Method m = exileEffect.getClass().getMethod("locName");
            Object name = m.invoke(exileEffect);
            return name != null ? name.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isEffectNegative(Object exileEffect) {
        if (exileEffect == null) return false;
        try {
            java.lang.reflect.Field f = exileEffect.getClass().getField("type");
            Object type = f.get(exileEffect);
            if (type != null) {
                return type.toString().contains("negative");
            }
        } catch (Exception e) {
            // ignore
        }
        return false;
    }

    public static String getEffectDurationString(Object instanceData) {
        if (instanceData == null) return "";
        try {
            java.lang.reflect.Method m = instanceData.getClass().getMethod("getDurationString");
            return (String) m.invoke(instanceData);
        } catch (Exception e) {
            return "";
        }
    }
}
