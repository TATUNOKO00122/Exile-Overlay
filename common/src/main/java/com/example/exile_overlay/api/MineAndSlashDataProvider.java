package com.example.exile_overlay.api;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * Mine and Slash (M&S) MOD用のデータプロバイダー
 * HJUD方式を参考に、シンプルかつ確実なデータ取得を実現
 * 
 * 【スロットマッピング】
 * Mine and SlashのデータをHUDスロットに以下のようにマッピングします:
 * 
 * - ORB_1: M&S Health（体力）
 * - ORB_1_OVERLAY: M&S Magic Shield（魔法シールド）
 * - ORB_2: M&S Mana（マナ）または Blood（ブラッド魔法）
 * - ORB_3: M&S Energy（エネルギー/スタミナ）
 */
public class MineAndSlashDataProvider extends AbstractModDataProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PRIORITY = 100;
    private static final long CACHE_DURATION_MS = 250;

    // M&S利用可能性フラグ
    private static Boolean mnsAvailable = null;

    // クラスキャッシュ
    private static Class<?> loadClass = null;
    private static Class<?> entityDataClass = null;
    private static Class<?> resourcesDataClass = null;
    private static Class<?> resourceTypeClass = null;
    private static Class<?> unitClass = null;
    private static Class<?> healthUtilsClass = null;

    // メソッドキャッシュ
    private static Method loadUnitMethod = null;
    private static Method getResourcesMethod = null;
    private static Method getManaMethod = null;
    private static Method getMagicShieldMethod = null;
    private static Method getEnergyMethod = null;
    private static Method getBloodMethod = null;
    private static Method getMaximumResourceMethod = null;
    private static Method getExpMethod = null;
    private static Method getExpRequiredMethod = null;
    private static Method getLevelMethod = null;
    private static Method getUnitMethod = null;
    private static Method isBloodMageMethod = null;
    private static Method getCurrentHealthMethod = null;
    private static Method getMaxHealthMethod = null;

    // ResourceType enum values
    private static Object MANA_TYPE = null;
    private static Object MAGIC_SHIELD_TYPE = null;
    private static Object ENERGY_TYPE = null;
    private static Object BLOOD_TYPE = null;

    // シンプルなキャッシュ（HJUD方式）
    private static WeakReference<Player> cachedPlayer = null;
    private static Object cachedEntityData = null;
    private static long cacheTime = 0;

    // 初期化状態追跡
    private static boolean initialized = false;

    static {
        initialize();
    }

    private static void initialize() {
        if (initialized) return;
        
        try {
            // クラスの存在確認
            loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            entityDataClass = Class.forName("com.robertx22.mine_and_slash.capability.entity.EntityData");
            resourcesDataClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourcesData");
            resourceTypeClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
            unitClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit");
            healthUtilsClass = Class.forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");

            // メソッド取得（個別にtry-catch）
            try {
                loadUnitMethod = loadClass.getMethod("Unit", Entity.class);
            } catch (Exception e) {
                LOGGER.error("Failed to get Load.Unit method: {}", e.getMessage());
            }

            try {
                getResourcesMethod = entityDataClass.getMethod("getResources");
            } catch (Exception e) {
                LOGGER.error("Failed to get getResources method: {}", e.getMessage());
            }

            try {
                getManaMethod = resourcesDataClass.getMethod("getMana");
            } catch (Exception e) {
                LOGGER.error("Failed to get getMana method: {}", e.getMessage());
            }

            try {
                getMagicShieldMethod = resourcesDataClass.getMethod("getMagicShield");
            } catch (Exception e) {
                LOGGER.error("Failed to get getMagicShield method: {}", e.getMessage());
            }

            try {
                getEnergyMethod = resourcesDataClass.getMethod("getEnergy");
            } catch (Exception e) {
                LOGGER.error("Failed to get getEnergy method: {}", e.getMessage());
            }

            try {
                getBloodMethod = resourcesDataClass.getMethod("getBlood");
            } catch (Exception e) {
                LOGGER.error("Failed to get getBlood method: {}", e.getMessage());
            }

            try {
                getMaximumResourceMethod = entityDataClass.getMethod("getMaximumResource", resourceTypeClass);
            } catch (Exception e) {
                LOGGER.error("Failed to get getMaximumResource method: {}", e.getMessage());
            }

            try {
                getExpMethod = entityDataClass.getMethod("getExp");
            } catch (Exception e) {
                LOGGER.error("Failed to get getExp method: {}", e.getMessage());
            }

            try {
                getExpRequiredMethod = entityDataClass.getMethod("getExpRequiredForLevelUp");
            } catch (Exception e) {
                LOGGER.error("Failed to get getExpRequiredForLevelUp method: {}", e.getMessage());
            }

            try {
                getLevelMethod = entityDataClass.getMethod("getLevel");
            } catch (Exception e) {
                LOGGER.error("Failed to get getLevel method: {}", e.getMessage());
            }

            try {
                getUnitMethod = entityDataClass.getMethod("getUnit");
            } catch (Exception e) {
                LOGGER.error("Failed to get getUnit method: {}", e.getMessage());
            }

            try {
                isBloodMageMethod = unitClass.getMethod("isBloodMage");
            } catch (Exception e) {
                LOGGER.error("Failed to get isBloodMage method: {}", e.getMessage());
            }

            try {
                getCurrentHealthMethod = healthUtilsClass.getMethod("getCurrentHealth", LivingEntity.class);
            } catch (Exception e) {
                LOGGER.error("Failed to get getCurrentHealth method: {}", e.getMessage());
            }

            try {
                getMaxHealthMethod = healthUtilsClass.getMethod("getMaxHealth", LivingEntity.class);
            } catch (Exception e) {
                LOGGER.error("Failed to get getMaxHealth method: {}", e.getMessage());
            }

            // ResourceType enum values
            try {
                MANA_TYPE = resourceTypeClass.getField("mana").get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to get MANA_TYPE: {}", e.getMessage());
            }

            try {
                MAGIC_SHIELD_TYPE = resourceTypeClass.getField("magic_shield").get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to get MAGIC_SHIELD_TYPE: {}", e.getMessage());
            }

            try {
                ENERGY_TYPE = resourceTypeClass.getField("energy").get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to get ENERGY_TYPE: {}", e.getMessage());
            }

            try {
                BLOOD_TYPE = resourceTypeClass.getField("blood").get(null);
            } catch (Exception e) {
                LOGGER.error("Failed to get BLOOD_TYPE: {}", e.getMessage());
            }

            initialized = true;
            LOGGER.info("Mine and Slash integration initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Mine and Slash initialization failed: {}", e.getMessage());
        }
    }

    public MineAndSlashDataProvider() {
        setCacheDuration(CACHE_DURATION_MS);
    }

    @Override
    public boolean isAvailable() {
        if (mnsAvailable == null) {
            mnsAvailable = loadClass != null && loadUnitMethod != null;
            if (mnsAvailable) {
                LOGGER.info("Mine and Slash detected, enabling integration.");
            } else {
                LOGGER.info("Mine and Slash not found, using vanilla fallbacks.");
            }
        }
        return mnsAvailable;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getId() {
        return "mine_and_slash";
    }

    /**
     * HJUD方式のシンプルなキャッシュでEntityDataを取得
     */
    private Object getCachedEntityData(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }

        long now = System.currentTimeMillis();
        Player cached = cachedPlayer != null ? cachedPlayer.get() : null;

        if (cached == player && cachedEntityData != null && (now - cacheTime) < CACHE_DURATION_MS) {
            return cachedEntityData;
        }

        try {
            cachedEntityData = loadUnitMethod.invoke(null, player);
            cachedPlayer = new WeakReference<>(player);
            cacheTime = now;
            return cachedEntityData;
        } catch (Exception e) {
            LOGGER.debug("Error getting M&S EntityData: {}", e.getMessage());
            return null;
        }
    }

    private Object getResources(Player player) {
        Object data = getCachedEntityData(player);
        if (data != null && getResourcesMethod != null) {
            try {
                return getResourcesMethod.invoke(data);
            } catch (Exception e) {
                LOGGER.debug("Error getting M&S Resources: {}", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public float getValue(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return type.getDefaultValueTyped();
        }

        try {
            switch (type) {
                case ORB_1_CURRENT:
                    if (getCurrentHealthMethod != null) {
                        Object result = getCurrentHealthMethod.invoke(null, player);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 0f;
                
                case ORB_1_OVERLAY_CURRENT:
                    if (getMagicShieldMethod != null) {
                        Object resources = getResources(player);
                        if (resources != null) {
                            Object result = getMagicShieldMethod.invoke(resources);
                            if (result instanceof Number) {
                                return ((Number) result).floatValue();
                            }
                        }
                    }
                    return 0f;
                
                case ORB_2_CURRENT:
                    Object resources = getResources(player);
                    if (resources != null) {
                        boolean isBlood = getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey());
                        Method method = isBlood ? getBloodMethod : getManaMethod;
                        if (method != null) {
                            Object result = method.invoke(resources);
                            if (result instanceof Number) {
                                return ((Number) result).floatValue();
                            }
                        }
                    }
                    return 0f;
                
                case ORB_3_CURRENT:
                    resources = getResources(player);
                    if (resources != null && getEnergyMethod != null) {
                        Object result = getEnergyMethod.invoke(resources);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 0f;
                
                case LEVEL:
                    Object data = getCachedEntityData(player);
                    if (data != null && getLevelMethod != null) {
                        Object result = getLevelMethod.invoke(data);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 0f;
                
                case EXP:
                    data = getCachedEntityData(player);
                    if (data != null && getExpMethod != null) {
                        Object result = getExpMethod.invoke(data);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 0f;
                
                default:
                    return type.getDefaultValueTyped();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting M&S value for {}: {}", type.getKey(), e.getMessage());
            return type.getDefaultValueTyped();
        }
    }

    @Override
    public float getMaxValue(Player player, DataType type) {
        if (!isAvailable() || player == null) {
            return type == DataType.ORB_1_MAX || type == DataType.ORB_1_OVERLAY_MAX || 
                   type == DataType.ORB_2_MAX || type == DataType.ORB_3_MAX ? 1f : type.getDefaultValueTyped();
        }

        try {
            Object data = getCachedEntityData(player);
            if (data == null) {
                return 1f;
            }

            switch (type) {
                case ORB_1_MAX:
                    if (getMaxHealthMethod != null) {
                        Object result = getMaxHealthMethod.invoke(null, player);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 1f;
                
                case ORB_1_OVERLAY_MAX:
                    if (getMaximumResourceMethod != null && MAGIC_SHIELD_TYPE != null) {
                        Object result = getMaximumResourceMethod.invoke(data, MAGIC_SHIELD_TYPE);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 1f;
                
                case ORB_2_MAX:
                    if (getMaximumResourceMethod != null) {
                        boolean isBlood = getAttribute(player, DataType.ORB_2_IS_BLOOD.getKey());
                        Object resourceType = isBlood ? BLOOD_TYPE : MANA_TYPE;
                        if (resourceType != null) {
                            Object result = getMaximumResourceMethod.invoke(data, resourceType);
                            if (result instanceof Number) {
                                return ((Number) result).floatValue();
                            }
                        }
                    }
                    return 1f;
                
                case ORB_3_MAX:
                    if (getMaximumResourceMethod != null && ENERGY_TYPE != null) {
                        Object result = getMaximumResourceMethod.invoke(data, ENERGY_TYPE);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 1f;
                
                case EXP_REQUIRED:
                    if (getExpRequiredMethod != null) {
                        Object result = getExpRequiredMethod.invoke(data);
                        if (result instanceof Number) {
                            return ((Number) result).floatValue();
                        }
                    }
                    return 1f;
                
                default:
                    return type.getDefaultValueTyped();
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting M&S max value for {}: {}", type.getKey(), e.getMessage());
            return 1f;
        }
    }

    @Override
    public boolean getAttribute(Player player, String attributeKey) {
        if (!isAvailable() || player == null) {
            return false;
        }

        if (DataType.ORB_2_IS_BLOOD.getKey().equals(attributeKey)) {
            try {
                Object data = getCachedEntityData(player);
                if (data != null && getUnitMethod != null) {
                    Object unit = getUnitMethod.invoke(data);
                    if (unit != null && isBloodMageMethod != null) {
                        Object result = isBloodMageMethod.invoke(unit);
                        if (result instanceof Boolean) {
                            return (Boolean) result;
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("Error checking blood mage status: {}", e.getMessage());
            }
        }
        return false;
    }

    /**
     * M&SデータをModDataとして取得
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
}
