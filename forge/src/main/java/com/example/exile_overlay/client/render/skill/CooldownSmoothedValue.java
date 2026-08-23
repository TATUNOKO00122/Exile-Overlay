package com.example.exile_overlay.client.render.skill;

/**
 * スキルクールダウンの表示値を滑らかに補間するクラス。
 * M&Sのクールダウンは20tick/秒の整数管理なので、高FPS環境ではカクつく。
 * 実時間ベースの自然減衰と単調減少ガードにより、高FPSで時計の秒针のように均等かつ滑らかに回転する。
 * スキル発動時の急増には瞬時スナップ。固定配列再利用でゼロアロケーション。
 */
public final class CooldownSmoothedValue {

    private static final int SLOT_COUNT = 8;



    private static class SlotState {
        float displayed = 0.0f;
        float lastTarget = 0.0f;
        long lastTimeMs = 0L;
        float decayRatePerSec = 0.0f;
        boolean active = false;
    }

    private static class ChargeProgressState {
        float lastValue = 0.0f;
        float targetValue = 0.0f;
        long lastTickTime = 0L;
    }

    private static final SlotState[] CD_STATES = new SlotState[SLOT_COUNT];
    private static final SlotState[] REGEN_STATES = new SlotState[SLOT_COUNT];
    private static final ChargeProgressState[] CHARGE_STATES = new ChargeProgressState[SLOT_COUNT];
    private static final SlotState GCD_STATE = new SlotState();

    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            CD_STATES[i] = new SlotState();
            REGEN_STATES[i] = new SlotState();
            CHARGE_STATES[i] = new ChargeProgressState();
        }
    }

    private CooldownSmoothedValue() {}

    /**
     * スロットのクールダウン割合を滑らかに補間して取得
     */
    public static float getSmoothedCooldown(int slot, float targetPercent, int neededTicks) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return targetPercent;
        }
        return computeSmoothed(CD_STATES[slot], targetPercent, neededTicks);
    }

    /**
     * チャージスキルのリジェネ割合を滑らかに補間して取得 (Tick間線形補間 LERP)
     * 20TPSのローカルTick更新の間を実時間で完全線形補間するため、カクつきや巻き戻りが物理的に発生しません。
     */
    public static float getSmoothedChargeRegen(int slot, float rawPercent) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            return rawPercent;
        }
        ChargeProgressState state = CHARGE_STATES[slot];
        long now = System.currentTimeMillis();

        if (rawPercent <= 0.0f) {
            state.lastValue = 0.0f;
            state.targetValue = 0.0f;
            state.lastTickTime = now;
            return 0.0f;
        }

        // M&SのTick値が更新された（またはスキル使用等で値が急変した）
        if (rawPercent != state.targetValue) {
            if (rawPercent > state.targetValue || state.targetValue <= 0.0f) {
                // スキル使用でリジェネが開始/増加した、または初回
                state.lastValue = rawPercent;
                state.targetValue = rawPercent;
            } else {
                // 通常のTick減少: 直前の目標値を開始値とし、新しい値を目標値にする
                state.lastValue = state.targetValue;
                state.targetValue = rawPercent;
            }
            state.lastTickTime = now;
        }

        // 1Tick = 50ms の実時間進行割合 (0.0f ~ 1.0f)
        float alpha = (now - state.lastTickTime) / 50.0f;
        if (alpha < 0.0f) alpha = 0.0f;
        if (alpha > 1.0f) alpha = 1.0f;

        // lastValue から targetValue へ滑らかに線形補間
        return state.lastValue + (state.targetValue - state.lastValue) * alpha;
    }

    /**
     * グローバルクールダウン（GCD）の割合を滑らかに補間して取得
     */
    public static float getSmoothedGcd(float targetPercent, int neededTicks) {
        return computeSmoothed(GCD_STATE, targetPercent, neededTicks);
    }

    /**
     * ワールド退出時やセッションリセット時に呼び出し
     */
    public static void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            resetState(CD_STATES[i]);
            resetState(REGEN_STATES[i]);
            CHARGE_STATES[i].lastValue = 0.0f;
            CHARGE_STATES[i].targetValue = 0.0f;
            CHARGE_STATES[i].lastTickTime = 0L;
        }
        resetState(GCD_STATE);
    }

    private static void resetState(SlotState state) {
        state.displayed = 0.0f;
        state.lastTarget = 0.0f;
        state.lastTimeMs = 0L;
        state.decayRatePerSec = 0.0f;
        state.active = false;
    }

    private static float computeSmoothed(SlotState state, float targetPercent, int neededTicks) {
        long now = System.currentTimeMillis();

        if (targetPercent <= 0.0f) {
            state.displayed = 0.0f;
            state.lastTarget = 0.0f;
            state.lastTimeMs = now;
            state.active = false;
            return 0.0f;
        }

        // 新規クールダウン発生判定 (未アクティブ時は閾値以上、またはtargetPercentが急増した場合)
        boolean isNewCooldown = (!state.active && targetPercent >= 0.03f)
                || (targetPercent > state.displayed + 0.15f)
                || (targetPercent >= 0.98f);

        if (isNewCooldown) {
            state.displayed = targetPercent;
            state.lastTarget = targetPercent;
            state.lastTimeMs = now;
            state.active = true;
            if (neededTicks > 0) {
                state.decayRatePerSec = 20.0f / (float) neededTicks;
            } else {
                state.decayRatePerSec = (targetPercent > 0.0f) ? (targetPercent / 3.0f) : 0.2f;
            }
            return targetPercent;
        }

        if (!state.active) {
            // アクティブでなく微小な残余パケットの場合は描画しない
            return 0.0f;
        }

        long dtMs = now - state.lastTimeMs;
        state.lastTimeMs = now;

        if (dtMs < 0) {
            dtMs = 0;
        } else if (dtMs > 300) {
            dtMs = 300;
        }

        float dtSec = dtMs / 1000.0f;

        // 減衰レートの算出
        float decayRate = state.decayRatePerSec;
        if (decayRate <= 0.0f && neededTicks > 0) {
            decayRate = 20.0f / (float) neededTicks;
            state.decayRatePerSec = decayRate;
        }
        if (decayRate <= 0.0f) {
            decayRate = (targetPercent > 0.0f) ? (targetPercent / 3.0f) : 0.2f;
        }

        // 実時間による自然減衰 (単調減少)
        state.displayed -= decayRate * dtSec;

        // サーバーパケットとの同期補正
        // 1. サーバー値が現在の表示値より小さい場合（サーバー側がより進んでいる / 短縮効果など）
        //    -> サーバー値に向かってスムーズに追従
        if (targetPercent < state.displayed) {
            float diff = targetPercent - state.displayed;
            float factor = (float) (1.0 - Math.exp(-10.0 * dtSec));
            state.displayed += diff * factor;
        }
        // 2. サーバー値が現在の表示値より大きい場合（サーバーパケットが遅れて届いて巻き戻っている）
        //    -> 巻き戻しは完全に無視して自然減衰を維持（単調減少ガード）
        //    ※ ただし大幅に乖離した場合（0.25以上）のみ緩やかに同期
        else if (targetPercent - state.displayed > 0.25f) {
            state.displayed += (targetPercent - state.displayed) * 0.1f;
        }

        state.lastTarget = targetPercent;

        if (state.displayed <= 0.001f) {
            state.displayed = 0.0f;
            state.active = false;
        }

        return state.displayed;
    }
}
