package com.example.exile_overlay.client.render.orb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.example.exile_overlay.client.config.OrbSmoothConfig;

/**
 * オーブの表示値を滑らかに補間するクラス。
 * 実時間（Delta Time）ベースの指数イージングでFPSに依存せず追従する。
 * 各オーIDごとに状態を独立管理。ConcurrentHashMap + synchronized(State)でスレッド安全。
 */
public class OrbSmoothedValue {

    private static final float SNAP_THRESHOLD = 0.002f;

    private static final Map<String, State> states = new ConcurrentHashMap<>();

    private OrbSmoothedValue() {}

    public static float getSmoothedPercent(String orbId, float targetPercent) {
        long now = System.currentTimeMillis();
        State state = states.computeIfAbsent(orbId, k -> new State());
        synchronized (state) {
            if (!state.initialized) {
                state.displayed = targetPercent;
                state.initialized = true;
                state.lastUpdateTime = now;
                return targetPercent;
            }

            long dtMs = now - state.lastUpdateTime;
            state.lastUpdateTime = now;

            // 時間の逆行などの不整合対策
            if (dtMs < 0) {
                dtMs = 0;
            }

            float diff = targetPercent - state.displayed;
            if (Math.abs(diff) < SNAP_THRESHOLD) {
                state.displayed = targetPercent;
                return targetPercent;
            }

            OrbSmoothConfig config = OrbSmoothConfig.getInstance();
            boolean isIncrease = diff > 0;
            boolean isEnabled = isIncrease ? config.isSmoothIncrease() : config.isSmoothDecrease();
            
            if (!isEnabled) {
                state.displayed = targetPercent;
                return targetPercent;
            }

            float smoothingK = isIncrease ? config.getIncreaseSpeed() : config.getDecreaseSpeed();
            
            // dtベースの指数イージング率を計算
            float factor = (float) (1.0 - Math.exp(-smoothingK * (dtMs / 1000.0)));
            if (factor > 1.0f) {
                factor = 1.0f;
            }

            state.displayed += diff * factor;
            return state.displayed;
        }
    }

    public static void reset(String orbId) {
        states.remove(orbId);
    }

    public static void resetAll() {
        states.clear();
    }

    private static class State {
        float displayed;
        boolean initialized;
        long lastUpdateTime;
    }
}
