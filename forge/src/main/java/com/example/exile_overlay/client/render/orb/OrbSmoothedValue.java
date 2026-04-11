package com.example.exile_overlay.client.render.orb;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * オーブの表示値を滑らかに補間するクラス
 *
 * 【設計思想】
 * 指数イージング（exponential easing）でターゲット値に向かって表示値を追従させる。
 * EffectRenderHelper.ANIMATION_SPEED = 0.2f と同等の感度。
 * 各オーブIDごとに独立した状態を管理。
 *
 * 【パフォーマンス最適化】
 * - per-frame zero allocation: フィールド再利用
 * - ConcurrentHashMapでスレッドセーフ（tick thread vs render thread）
 */
public class OrbSmoothedValue {

    private static final float SMOOTHING_SPEED = 0.2f;
    private static final float SNAP_THRESHOLD = 0.002f;

    private static final Map<String, State> states = new ConcurrentHashMap<>();

    private OrbSmoothedValue() {}

    public static float getSmoothedPercent(String orbId, float targetPercent) {
        State state = states.computeIfAbsent(orbId, k -> new State());
        if (!state.initialized) {
            state.displayed = targetPercent;
            state.initialized = true;
            return targetPercent;
        }

        float diff = targetPercent - state.displayed;
        if (Math.abs(diff) < SNAP_THRESHOLD) {
            state.displayed = targetPercent;
            return targetPercent;
        }

        state.displayed += diff * SMOOTHING_SPEED;
        return state.displayed;
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
    }
}
