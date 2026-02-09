package com.example.exile_overlay.util;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.ref.WeakReference;

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
            // リフレクションを使用してLoad.Unit(player)を呼び出す
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

    public static float getCurrentMana(Player player) {
        if (!isMnsLoaded())
            return 0;
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getResources = data.getClass().getMethod("getResources");
                Object resources = getResources.invoke(data);
                if (resources != null) {
                    var getMana = resources.getClass().getMethod("getMana");
                    return ((Number) getMana.invoke(resources)).floatValue();
                }
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
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getResources = data.getClass().getMethod("getResources");
                Object resources = getResources.invoke(data);
                if (resources != null) {
                    var getMagicShield = resources.getClass().getMethod("getMagicShield");
                    return ((Number) getMagicShield.invoke(resources)).floatValue();
                }
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
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getResources = data.getClass().getMethod("getResources");
                Object resources = getResources.invoke(data);
                if (resources != null) {
                    var getEnergy = resources.getClass().getMethod("getEnergy");
                    return ((Number) getEnergy.invoke(resources)).floatValue();
                }
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
        Object data = getCachedEntityData(player);
        if (data != null) {
            try {
                var getResources = data.getClass().getMethod("getResources");
                Object resources = getResources.invoke(data);
                if (resources != null) {
                    var getBlood = resources.getClass().getMethod("getBlood");
                    return ((Number) getBlood.invoke(resources)).floatValue();
                }
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
}
