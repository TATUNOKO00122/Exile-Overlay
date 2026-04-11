package com.example.exile_overlay.util;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * TopDownView MOD連携ヘルパー
 *
 * 【設計思想】
 * topdown_view MODが有効な場合、マウスカーソル下のエンティティを
 * TargetHighlightStateから取得する。FPS視点のクロスヘアレイキャストの代わりとして機能する。
 *
 * 【スレッド安全性】
 * - MethodHandleは不変でスレッド安全
 * - TargetHighlightStateのフィールドはvolatile宣言済み
 */
public class TopDownViewHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Boolean loaded = null;

    private static MethodHandle targetHighlightStateInstance;
    private static MethodHandle modStatusInstance;
    private static MethodHandle getCurrentTarget;
    private static MethodHandle isEnabled;
    private static boolean initialized = false;

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                Class.forName("com.topdownview.TopDownViewMod");
                loaded = true;
                LOGGER.info("TopDownView detected, enabling target integration.");
            } catch (ClassNotFoundException e) {
                loaded = false;
                LOGGER.debug("TopDownView not found, using vanilla crosshair targeting.");
            }
        }
        return loaded;
    }

    private static void init() {
        if (initialized || !isLoaded()) return;
        try {
            Class<?> targetHighlightStateClass = Class.forName("com.topdownview.state.TargetHighlightState");
            Class<?> modStatusClass = Class.forName("com.topdownview.state.ModStatus");

            targetHighlightStateInstance = MethodHandles.publicLookup().findStaticGetter(
                    targetHighlightStateClass, "INSTANCE", targetHighlightStateClass);
            modStatusInstance = MethodHandles.publicLookup().findStaticGetter(
                    modStatusClass, "INSTANCE", modStatusClass);

            getCurrentTarget = MethodHandles.publicLookup().findVirtual(
                    targetHighlightStateClass, "getCurrentTarget",
                    java.lang.invoke.MethodType.methodType(LivingEntity.class));

            isEnabled = MethodHandles.publicLookup().findVirtual(
                    modStatusClass, "isEnabled",
                    java.lang.invoke.MethodType.methodType(boolean.class));

            initialized = true;
            LOGGER.info("TopDownView integration initialized successfully.");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize TopDownView integration: {}", e.getMessage());
            initialized = false;
        }
    }

    /**
     * topdown_view MODが有効かつ有効化されている場合に、マウスカーソル下のターゲットエンティティを取得する。
     *
     * @return ターゲットエンティティ、MODが無効またはターゲットなしの場合はnull
     */
    public static LivingEntity getTarget() {
        init();
        if (!initialized) return null;

        try {
            Object modStatus = modStatusInstance.invoke();
            boolean enabled = (boolean) isEnabled.invoke(modStatus);
            if (!enabled) return null;

            Object state = targetHighlightStateInstance.invoke();
            return (LivingEntity) getCurrentTarget.invoke(state);
        } catch (Throwable e) {
            LOGGER.error("Failed to get TopDownView target: {}", e.getMessage());
            return null;
        }
    }
}
