package com.example.exile_overlay.api;

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
     * staticメソッドをLookup
     */
    private static MethodHandle lookupStaticMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return PUBLIC_LOOKUP.findStatic(clazz, name, MethodType.methodType(float.class, paramTypes));
        } catch (Exception e) {
            LOGGER.error("Failed to lookup static method {}.{}: {} (type: {})", 
                clazz.getSimpleName(), name, e.getMessage(), e.getClass().getSimpleName());
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
}
