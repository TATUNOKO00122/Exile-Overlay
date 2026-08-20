package com.example.exile_overlay.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;

public class XaeroWaypointHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(XaeroWaypointHelper.class);
    
    private static boolean xaeroAvailable = false;
    private static Method getCustomWaypointsMethod = null;
    private static Constructor<?> waypointConstructor = null;
    private static Class<?> waypointClass = null;
    private static String initError = null;

    static {
        try {
            Class<?> managerClass = Class.forName("xaero.common.minimap.waypoints.WaypointsManager");
            waypointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
            getCustomWaypointsMethod = managerClass.getMethod("getCustomWaypoints", String.class);
            // 1.20.1 ForgeのWaypointコンストラクタは: Waypoint(int x, int y, int z, String name, String symbol, int colorId)
            waypointConstructor = waypointClass.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class);
            xaeroAvailable = true;
            LOGGER.info("exile_overlay: Xaero's Minimap integration initialized successfully.");
        } catch (Throwable t) {
            initError = t.toString();
            LOGGER.info("exile_overlay: Xaero's Minimap not detected or unsupported version. Error: " + initError);
        }
    }

    public static boolean isXaeroAvailable() {
        return xaeroAvailable;
    }

    @SuppressWarnings("unchecked")
    public static void addWaypoint(Player player, BlockPos pos, String name, String symbol, int colorId) {
        if (!xaeroAvailable) {
            LOGGER.warn("exile_overlay: Xaero's Minimap is not loaded or initialization failed. InitError: {}", initError);
            return;
        }
        try {
            Hashtable<Object, Object> customWaypoints = (Hashtable<Object, Object>) getCustomWaypointsMethod.invoke(null, "exile_overlay");
            if (customWaypoints != null) {
                // 重複追加防止
                for (Map.Entry<Object, Object> entry : customWaypoints.entrySet()) {
                    Object wp = entry.getValue();
                    int wpX = getCoordinate(wp, "x");
                    int wpY = getCoordinate(wp, "y");
                    int wpZ = getCoordinate(wp, "z");
                    if (wpX == pos.getX() && wpY == pos.getY() && wpZ == pos.getZ()) {
                        return; // 既に同じ座標のウェイポイントが存在する
                    }
                }
                
                Object newWaypoint = waypointConstructor.newInstance(pos.getX(), pos.getY(), pos.getZ(), name, symbol, colorId);
                String key = name + "_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                customWaypoints.put(key, newWaypoint);
                LOGGER.info("exile_overlay: Waypoint registered to Xaero successfully at {}", pos);
            } else {
                LOGGER.error("exile_overlay: getCustomWaypoints returned null.");
            }
        } catch (Throwable t) {
            LOGGER.error("exile_overlay: Failed to add Xaero waypoint", t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void removeWaypoint(BlockPos pos) {
        if (!xaeroAvailable) return;
        try {
            Hashtable<Object, Object> customWaypoints = (Hashtable<Object, Object>) getCustomWaypointsMethod.invoke(null, "exile_overlay");
            if (customWaypoints != null) {
                Object toRemoveKey = null;
                for (Map.Entry<Object, Object> entry : customWaypoints.entrySet()) {
                    Object wp = entry.getValue();
                    int wpX = getCoordinate(wp, "x");
                    int wpY = getCoordinate(wp, "y");
                    int wpZ = getCoordinate(wp, "z");
                    if (wpX == pos.getX() && wpY == pos.getY() && wpZ == pos.getZ()) {
                        toRemoveKey = entry.getKey();
                        break;
                    }
                }
                if (toRemoveKey != null) {
                    customWaypoints.remove(toRemoveKey);
                    LOGGER.info("exile_overlay: Removed Xaero waypoint at {}", pos);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("exile_overlay: Failed to remove Xaero waypoint", t);
        }
    }

    @SuppressWarnings("unchecked")
    public static void clearAllWaypoints() {
        if (!xaeroAvailable) return;
        try {
            Hashtable<Object, Object> customWaypoints = (Hashtable<Object, Object>) getCustomWaypointsMethod.invoke(null, "exile_overlay");
            if (customWaypoints != null && !customWaypoints.isEmpty()) {
                customWaypoints.clear();
                LOGGER.info("exile_overlay: Cleared all exile_overlay Xaero waypoints");
            }
        } catch (Throwable t) {
            LOGGER.error("exile_overlay: Failed to clear Xaero waypoints", t);
        }
    }

    private static int getCoordinate(Object waypoint, String coordName) throws Exception {
        try {
            // メソッド getX(), getY(), getZ() を試す
            String methodName = "get" + coordName.toUpperCase();
            Method m = waypoint.getClass().getMethod(methodName);
            return (int) m.invoke(waypoint);
        } catch (NoSuchMethodException e) {
            // フィールド x, y, z を試す
            java.lang.reflect.Field f = waypoint.getClass().getField(coordName);
            return f.getInt(coordName);
        }
    }
}
