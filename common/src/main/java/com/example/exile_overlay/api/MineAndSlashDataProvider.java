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
 * 
 * 【スロットマッピング】
 * Mine and SlashのデータをHUDスロットに以下のようにマッピングします:
 * 
 * - ORB_1: M&S Health（体力）
 * - ORB_1_OVERLAY: M&S Magic Shield（魔法シールド）
 * - ORB_2: M&S Mana（マナ）または Blood（ブラッド魔法）
 * - ORB_3: M&S Energy（エネルギー/スタミナ）
 * 
 * このマッピングにより、HUDは「何を表示するか」を知る必要なく、
 * 「どのスロットにデータを供給するか」のみを管理できます。
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
                    MethodType.methodType(Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit"),
                            Player.class));

            // Unitクラスのメソッド
            Class<?> unitClass = Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Unit");
            getResources = lookup.findVirtual(unitClass, "getResources",
                    MethodType.methodType(Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.Resources")));
            getMaximumResource = lookup.findVirtual(unitClass, "getMaximumResource",
                    MethodType.methodType(float.class,
                            Class.forName("com.robertx22.mine_and_slash.saveclasses.unit.ResourceType")));
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
            Class<?> healthUtilsClass = Class
                    .forName("com.robertx22.mine_and_slash.uncommon.utilityclasses.HealthUtils");
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

    // ========== 汎用データ取得の実装 ==========
    @Override
    public float getValue(Player player, DataType type) {
        if (!isAvailable())
            return super.getValue(player, type);

        return fetchSafely(player, type, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data == null)
                    return 0f;

                Object resources = GET_RESOURCES.invoke(data);

                return switch (type) {
                    case ORB_1_CURRENT -> (float) HEALTH_GET_CURRENT.invoke(p);
                    case ORB_1_OVERLAY_CURRENT -> resources != null ? (float) GET_MAGIC_SHIELD.invoke(resources) : 0f;
                    case ORB_2_CURRENT -> {
                        if (resources == null)
                            yield 0f;
                        yield getAttribute(p, DataType.ORB_2_IS_BLOOD.getKey()) ? (float) GET_BLOOD.invoke(resources)
                                : (float) GET_MANA.invoke(resources);
                    }
                    case ORB_3_CURRENT -> resources != null ? (float) GET_ENERGY.invoke(resources) : 0f;
                    case LEVEL -> (float) (int) GET_LEVEL.invoke(data);
                    case EXP -> (float) GET_EXP.invoke(data);
                    default -> (Float) type.getDefaultValue();
                };
            } catch (Throwable e) {
                return 0f;
            }
        });
    }

    @Override
    public float getMaxValue(Player player, DataType type) {
        if (!isAvailable())
            return super.getMaxValue(player, type);

        return fetchSafely(player, type, p -> {
            try {
                Object data = LOAD_UNIT.invoke(p);
                if (data == null)
                    return 1f;

                return switch (type) {
                    case ORB_1_MAX -> (float) HEALTH_GET_MAX.invoke(p);
                    case ORB_1_OVERLAY_MAX -> (float) GET_MAXIMUM_RESOURCE.invoke(data, MAGIC_SHIELD_TYPE);
                    case ORB_2_MAX -> getAttribute(p, DataType.ORB_2_IS_BLOOD.getKey())
                            ? (float) GET_MAXIMUM_RESOURCE.invoke(data, BLOOD_TYPE)
                            : (float) GET_MAXIMUM_RESOURCE.invoke(data, MANA_TYPE);
                    case ORB_3_MAX -> (float) GET_MAXIMUM_RESOURCE.invoke(data, ENERGY_TYPE);
                    case EXP_REQUIRED -> (float) GET_EXP_REQUIRED.invoke(data);
                    default -> (Float) type.getDefaultValue();
                };
            } catch (Throwable e) {
                return 1f;
            }
        });
    }

    @Override
    public boolean getAttribute(Player player, String attributeKey) {
        if (!isAvailable())
            return super.getAttribute(player, attributeKey);

        if (DataType.ORB_2_IS_BLOOD.getKey().equals(attributeKey)) {
            return fetchSafely(player, DataType.ORB_2_IS_BLOOD, p -> {
                try {
                    Object data = LOAD_UNIT.invoke(p);
                    if (data != null) {
                        Object unit = GET_UNIT.invoke(data);
                        return unit != null && (boolean) IS_BLOOD_MAGE.invoke(unit);
                    }
                } catch (Throwable ignored) {
                }
                return false;
            });
        }
        return super.getAttribute(player, attributeKey);
    }

    /**
     * M&SデータをModDataとして取得
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
}
