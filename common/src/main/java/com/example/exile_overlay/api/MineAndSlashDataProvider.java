package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Mine and Slash (M&S) MOD用のデータプロバイダー
 * AbstractModDataProviderを継承し、キャッシュとエラーハンドリングを統一
 * リフレクションを使用してM&Sのデータを取得する
 */
public class MineAndSlashDataProvider extends AbstractModDataProvider {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PRIORITY = 100; // 高優先度
    private static final long CACHE_DURATION_MS = 250;
    
    // M&S利用可能性フラグ（遅延初期化）
    private static Boolean mnsAvailable = null;
    
    // MethodHandles（キャッシュ済み）
    private static final MethodHandle LOAD_UNIT;
    private static final MethodHandle GET_RESOURCES;
    private static final MethodHandle GET_MANA;
    private static final MethodHandle GET_MAGIC_SHIELD;
    private static final MethodHandle GET_ENERGY;
    private static final MethodHandle GET_BLOOD;
    private static final MethodHandle GET_MAXIMUM_RESOURCE;
    private static final MethodHandle GET_EXP;
    private static final MethodHandle GET_EXP_REQUIRED;
    private static final MethodHandle GET_LEVEL;
    private static final MethodHandle GET_UNIT;
    private static final MethodHandle IS_BLOOD_MAGE;
    private static final MethodHandle HEALTH_GET_CURRENT;
    private static final MethodHandle HEALTH_GET_MAX;
    
    // ResourceType enum values
    private static Object MANA_TYPE = null;
    private static Object MAGIC_SHIELD_TYPE = null;
    private static Object ENERGY_TYPE = null;
    private static Object BLOOD_TYPE = null;
    
    static {
        MethodHandle loadUnit = null;
        MethodHandle getResources = null;
        MethodHandle getMana = null;
        MethodHandle getMagicShield = null;
        MethodHandle getEnergy = null;
        MethodHandle getBlood = null;
        MethodHandle getMaximumResource = null;
        MethodHandle getExp = null;
        MethodHandle getExpRequired = null;
        MethodHandle getLevel = null;
        MethodHandle getUnit = null;
        MethodHandle isBloodMage = null;
        MethodHandle healthGetCurrent = null;
        MethodHandle healthGetMax = null;
        
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            
            // Load.Unitメソッド
            Class<?> loadClass = Class.forName("com.robertx22.mine_and_slash.uncommon.datasaving.Load");
            loadUnit = lookup.findStatic(loadClass, "Unit", 
                    MethodType.methodType(Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit"), Player.class));
            
            // Unitクラスのメソッド
            Class<?> unitClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit");
            getResources = lookup.findVirtual(unitClass, "getResources",
                    MethodType.methodType(Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Resources")));
            getMaximumResource = lookup.findVirtual(unitClass, "getMaximumResource",
                    MethodType.methodType(float.class, Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType")));
            getExp = lookup.findVirtual(unitClass, "getExp",
                    MethodType.methodType(float.class));
            getExpRequired = lookup.findVirtual(unitClass, "getExpRequiredForLevelUp",
                    MethodType.methodType(float.class));
            getLevel = lookup.findVirtual(unitClass, "getLevel",
                    MethodType.methodType(int.class));
            
            // Resourcesクラスのメソッド
            Class<?> resourcesClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Resources");
            getMana = lookup.findVirtual(resourcesClass, "getMana",
                    MethodType.methodType(float.class));
            getMagicShield = lookup.findVirtual(resourcesClass, "getMagicShield",
                    MethodType.methodType(float.class));
            getEnergy = lookup.findVirtual(resourcesClass, "getEnergy",
                    MethodType.methodType(float.class));
            getBlood = lookup.findVirtual(resourcesClass, "getBlood",
                    MethodType.methodType(float.class));
            
            // Unitクラスの追加メソッド
            getUnit = lookup.findVirtual(unitClass, "getUnit",
                    MethodType.methodType(Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.UnitCore")));
            
            // UnitCoreクラスのメソッド
            Class<?> unitCoreClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.UnitCore");
            isBloodMage = lookup.findVirtual(unitCoreClass, "isBloodMage",
                    MethodType.methodType(boolean.class));
            
            // ResourceType enum
            Class<?> resourceTypeClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType");
            MANA_TYPE = resourceTypeClass.getField("mana").get(null);
            MAGIC_SHIELD_TYPE = resourceTypeClass.getField("magic_shield").get(null);
            ENERGY_TYPE = resourceTypeClass.getField("energy").get(null);
            BLOOD_TYPE = resourceTypeClass.getField("blood").get(null);
            
            // HealthUtilsメソッド
            Class<?> healthUtilsClass = Class.forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");
            healthGetCurrent = lookup.findStatic(healthUtilsClass, "getCurrentHealth",
                    MethodType.methodType(float.class, Player.class));
            healthGetMax = lookup.findStatic(healthUtilsClass, "getMaxHealth",
                    MethodType.methodType(float.class, Player.class));
            
            LOGGER.info("Mine and Slash integration initialized successfully");
            
        } catch (Exception e) {
            LOGGER.debug("Mine and Slash not available or error during initialization: {}", e.getMessage());
        }
        
        LOAD_UNIT = loadUnit;
        GET_RESOURCES = getResources;
        GET_MANA = getMana;
        GET_MAGIC_SHIELD = getMagicShield;
        GET_ENERGY = getEnergy;
        GET_BLOOD = getBlood;
        GET_MAXIMUM_RESOURCE = getMaximumResource;
        GET_EXP = getExp;
        GET_EXP_REQUIRED = getExpRequired;
        GET_LEVEL = getLevel;
        GET_UNIT = getUnit;
        IS_BLOOD_MAGE = isBloodMage;
        HEALTH_GET_CURRENT = healthGetCurrent;
        HEALTH_GET_MAX = healthGetMax;
    }
    
    public MineAndSlashDataProvider() {
        setCacheDuration(CACHE_DURATION_MS);
    }
    
    @Override
    public boolean isAvailable() {
        if (mnsAvailable == null) {
            mnsAvailable = LOAD_UNIT != null;
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
     * M&SのUnitデータを取得（キャッシュ付き）
     */
    private Object getUnitData(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }
        
        // AbstractModDataProviderのキャッシュシステムを使用
        if (cache.isCacheValid(player)) {
            // キャッシュが有効な場合はnullを返して個別メソッドでキャッシュを使用
            return null;
        }
        
        try {
            return LOAD_UNIT.invoke(player);
        } catch (Throwable e) {
            logger.debug("Error accessing Mine and Slash unit data: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public float getCurrentHealth(Player player) {
        if (!isAvailable()) {
            return super.getCurrentHealth(player);
        }
        return fetchSafely(player, DataType.CURRENT_HEALTH, p -> {
            try {
                return (float) HEALTH_GET_CURRENT.invoke(p);
            } catch (Throwable e) {
                return p.getHealth();
            }
        });
    }
    
    @Override
    public float getMaxHealth(Player player) {
        if (!isAvailable()) {
            return super.getMaxHealth(player);
        }
        return fetchSafely(player, DataType.MAX_HEALTH, p -> {
            try {
                return (float) HEALTH_GET_MAX.invoke(p);
            } catch (Throwable e) {
                return p.getMaxHealth();
            }
        });
    }
    
    @Override
    public float getCurrentMana(Player player) {
        if (!isAvailable()) {
            return super.getCurrentMana(player);
        }
        return fetchSafely(player, DataType.CURRENT_MANA, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    Object resources = GET_RESOURCES.invoke(data);
                    if (resources != null) {
                        return (float) GET_MANA.invoke(resources);
                    }
                }
            } catch (Throwable e) {
                logger.debug("Error getting mana: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getMaxMana(Player player) {
        if (!isAvailable()) {
            return super.getMaxMana(player);
        }
        return fetchSafely(player, DataType.MAX_MANA, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null && MANA_TYPE != null) {
                    return (float) GET_MAXIMUM_RESOURCE.invoke(data, MANA_TYPE);
                }
            } catch (Throwable e) {
                logger.debug("Error getting max mana: {}", e.getMessage());
            }
            return 1.0f;
        });
    }
    
    @Override
    public float getCurrentMagicShield(Player player) {
        if (!isAvailable()) {
            return super.getCurrentMagicShield(player);
        }
        return fetchSafely(player, DataType.CURRENT_MAGIC_SHIELD, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    Object resources = GET_RESOURCES.invoke(data);
                    if (resources != null) {
                        return (float) GET_MAGIC_SHIELD.invoke(resources);
                    }
                }
            } catch (Throwable e) {
                logger.debug("Error getting magic shield: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getMaxMagicShield(Player player) {
        if (!isAvailable()) {
            return super.getMaxMagicShield(player);
        }
        return fetchSafely(player, DataType.MAX_MAGIC_SHIELD, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null && MAGIC_SHIELD_TYPE != null) {
                    return (float) GET_MAXIMUM_RESOURCE.invoke(data, MAGIC_SHIELD_TYPE);
                }
            } catch (Throwable e) {
                logger.debug("Error getting max magic shield: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getCurrentEnergy(Player player) {
        if (!isAvailable()) {
            return super.getCurrentEnergy(player);
        }
        return fetchSafely(player, DataType.CURRENT_ENERGY, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    Object resources = GET_RESOURCES.invoke(data);
                    if (resources != null) {
                        return (float) GET_ENERGY.invoke(resources);
                    }
                }
            } catch (Throwable e) {
                logger.debug("Error getting energy: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getMaxEnergy(Player player) {
        if (!isAvailable()) {
            return super.getMaxEnergy(player);
        }
        return fetchSafely(player, DataType.MAX_ENERGY, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null && ENERGY_TYPE != null) {
                    return (float) GET_MAXIMUM_RESOURCE.invoke(data, ENERGY_TYPE);
                }
            } catch (Throwable e) {
                logger.debug("Error getting max energy: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getCurrentBlood(Player player) {
        if (!isAvailable()) {
            return super.getCurrentBlood(player);
        }
        return fetchSafely(player, DataType.CURRENT_BLOOD, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    Object resources = GET_RESOURCES.invoke(data);
                    if (resources != null) {
                        return (float) GET_BLOOD.invoke(resources);
                    }
                }
            } catch (Throwable e) {
                logger.debug("Error getting blood: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getMaxBlood(Player player) {
        if (!isAvailable()) {
            return super.getMaxBlood(player);
        }
        return fetchSafely(player, DataType.MAX_BLOOD, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null && BLOOD_TYPE != null) {
                    return (float) GET_MAXIMUM_RESOURCE.invoke(data, BLOOD_TYPE);
                }
            } catch (Throwable e) {
                logger.debug("Error getting max blood: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public int getLevel(Player player) {
        if (!isAvailable()) {
            return super.getLevel(player);
        }
        return fetchSafely(player, DataType.LEVEL, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    return (int) GET_LEVEL.invoke(data);
                }
            } catch (Throwable e) {
                logger.debug("Error getting level: {}", e.getMessage());
            }
            return 1;
        });
    }
    
    @Override
    public float getExp(Player player) {
        if (!isAvailable()) {
            return super.getExp(player);
        }
        return fetchSafely(player, DataType.EXP, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    return (float) GET_EXP.invoke(data);
                }
            } catch (Throwable e) {
                logger.debug("Error getting exp: {}", e.getMessage());
            }
            return 0.0f;
        });
    }
    
    @Override
    public float getExpRequiredForLevelUp(Player player) {
        if (!isAvailable()) {
            return super.getExpRequiredForLevelUp(player);
        }
        return fetchSafely(player, DataType.EXP_REQUIRED, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    return (float) GET_EXP_REQUIRED.invoke(data);
                }
            } catch (Throwable e) {
                logger.debug("Error getting exp required: {}", e.getMessage());
            }
            return 1.0f;
        });
    }
    
    @Override
    public boolean isBloodMagicActive(Player player) {
        if (!isAvailable()) {
            return super.isBloodMagicActive(player);
        }
        return fetchSafely(player, DataType.BLOOD_MAGIC_ACTIVE, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data != null) {
                    Object unit = GET_UNIT.invoke(data);
                    if (unit != null) {
                        return (boolean) IS_BLOOD_MAGE.invoke(unit);
                    }
                }
            } catch (Throwable e) {
                logger.debug("Error checking blood magic: {}", e.getMessage());
            }
            return false;
        });
    }
    
    /**
     * M&SデータをModDataとして取得
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
}
