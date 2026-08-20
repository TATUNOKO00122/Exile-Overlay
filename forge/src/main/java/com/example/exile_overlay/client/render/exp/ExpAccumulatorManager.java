package com.example.exile_overlay.client.render.exp;

import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 累積獲得EXPポップアップの状態およびアニメーション管理クラス。
 * M&Sの公式経験値メッセージを受信した瞬間にイベント駆動で発火。
 * 差分ポーリングを廃止し、テレポート・ログイン時の誤発火を根本排除。
 * 職業名はM&Sから渡されるComponentをそのまま保持し言語・カスタム職業を問わず動的に解決。
 * ゼロアロケーション・スレッドセーフ。
 */
public final class ExpAccumulatorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExpAccumulatorManager");
    private static final ExpAccumulatorManager INSTANCE = new ExpAccumulatorManager();

    // アニメーション・表示時間定数 (ミリ秒)
    private static final long DISPLAY_DURATION_MS = 3500L; // 表示維持時間 (3.5秒)
    private static final long FADE_DURATION_MS = 1000L;    // フェードアウト時間 (最後の1秒)
    private static final long POP_DURATION_MS = 300L;      // ポップ拡大時間 (0.3秒)
    private static final float PEAK_SCALE = 1.30f;         // ポップ時の最大拡大倍率

    // デフォルトの生活職フォールバック表示名
    private static final Component DEFAULT_PROF_NAME = Component.literal("Salvaging");

    // ==================== 戦闘EXP (Combat Slot) ====================
    private int combatAccumulatedExp = 0;
    private float combatProgressPercentage = 0.0f;
    private long lastCombatGainedTimeMs = 0L;
    private long lastCombatPopTimeMs = 0L;

    // ==================== 生活職EXP (Profession Slot) ====================
    private Component activeProfDisplayName = DEFAULT_PROF_NAME;
    private int profAccumulatedExp = 0;
    private float profProgressPercentage = 0.0f;
    private long lastProfGainedTimeMs = 0L;
    private long lastProfPopTimeMs = 0L;

    private ExpAccumulatorManager() {
    }

    public static ExpAccumulatorManager getInstance() {
        return INSTANCE;
    }

    /**
     * クライアントティック毎の表示タイムアウト更新
     */
    public synchronized void tick() {
        long now = System.currentTimeMillis();

        if (combatAccumulatedExp > 0 && (now - lastCombatGainedTimeMs) >= DISPLAY_DURATION_MS) {
            combatAccumulatedExp = 0;
        }
        if (profAccumulatedExp > 0 && (now - lastProfGainedTimeMs) >= DISPLAY_DURATION_MS) {
            profAccumulatedExp = 0;
        }
    }

    /**
     * M&S の経験値メッセージを受信した際のエントリポイント
     */
    public synchronized void onMnsExpMessageReceived(int gained, Component profComponent, float percentage) {
        if (gained <= 0) {
            return;
        }

        if (profComponent == null || profComponent.getString().isBlank()) {
            // 職業名が空の場合は戦闘EXP
            onCombatExpGained(gained, percentage);
        } else {
            // 職業名が存在する場合は生活職EXP
            onProfExpGained(profComponent, gained, percentage);
        }
    }

    /**
     * 戦闘EXP獲得通知
     */
    public synchronized void onCombatExpGained(int gained, float percentage) {
        if (gained <= 0) return;

        long now = System.currentTimeMillis();
        this.combatAccumulatedExp += gained;
        this.combatProgressPercentage = Math.max(0.0f, Math.min(100.0f, percentage));
        this.lastCombatGainedTimeMs = now;
        this.lastCombatPopTimeMs = now;

        LOGGER.debug("Combat EXP gained: +{} (Total: {}, Progress: {}%)", gained, combatAccumulatedExp, combatProgressPercentage);
    }

    /**
     * 生活職EXP獲得通知
     */
    public synchronized void onProfExpGained(Component profDisplayName, int gained, float percentage) {
        if (gained <= 0 || profDisplayName == null) return;

        long now = System.currentTimeMillis();

        if (isSameProfession(profDisplayName) && isProfDisplaying()) {
            this.profAccumulatedExp += gained;
        } else {
            this.activeProfDisplayName = profDisplayName;
            this.profAccumulatedExp = gained;
        }

        this.profProgressPercentage = Math.max(0.0f, Math.min(100.0f, percentage));
        this.lastProfGainedTimeMs = now;
        this.lastProfPopTimeMs = now;

        LOGGER.debug("Profession [{}] EXP gained: +{} (Total: {}, Progress: {}%)",
                profDisplayName.getString(), gained, profAccumulatedExp, profProgressPercentage);
    }

    private boolean isSameProfession(Component other) {
        if (this.activeProfDisplayName == null || other == null) {
            return false;
        }
        return this.activeProfDisplayName.getString().equals(other.getString());
    }

    // ==================== ゲッター & アニメーション計算 ====================

    public synchronized boolean isCombatDisplaying() {
        if (combatAccumulatedExp <= 0) return false;
        return (System.currentTimeMillis() - lastCombatGainedTimeMs) < DISPLAY_DURATION_MS;
    }

    public synchronized int getCombatAccumulatedExp() {
        return combatAccumulatedExp;
    }

    public synchronized float getCombatProgressPercentage() {
        return combatProgressPercentage;
    }

    public float getCombatScaleMultiplier() {
        return calculateScaleMultiplier(lastCombatPopTimeMs);
    }

    public float getCombatAlpha() {
        return calculateAlpha(lastCombatGainedTimeMs);
    }

    public synchronized boolean isProfDisplaying() {
        if (profAccumulatedExp <= 0) return false;
        return (System.currentTimeMillis() - lastProfGainedTimeMs) < DISPLAY_DURATION_MS;
    }

    public synchronized int getProfAccumulatedExp() {
        return profAccumulatedExp;
    }

    public synchronized Component getActiveProfDisplayName() {
        return activeProfDisplayName != null ? activeProfDisplayName : DEFAULT_PROF_NAME;
    }

    public synchronized float getProfProgressPercentage() {
        return profProgressPercentage;
    }

    public float getProfScaleMultiplier() {
        return calculateScaleMultiplier(lastProfPopTimeMs);
    }

    public float getProfAlpha() {
        return calculateAlpha(lastProfGainedTimeMs);
    }

    private float calculateScaleMultiplier(long popTimeMs) {
        if (popTimeMs <= 0) return 1.0f;
        long elapsed = System.currentTimeMillis() - popTimeMs;
        if (elapsed >= POP_DURATION_MS) return 1.0f;

        float progress = (float) elapsed / (float) POP_DURATION_MS;
        float scaleDiff = PEAK_SCALE - 1.0f;
        if (progress < 0.25f) {
            float t = progress / 0.25f;
            return 1.0f + scaleDiff * (float) Math.sin(t * (Math.PI / 2.0));
        } else {
            float t = (progress - 0.25f) / 0.75f;
            float decay = 1.0f - t;
            return 1.0f + scaleDiff * decay * decay;
        }
    }

    private float calculateAlpha(long gainedTimeMs) {
        if (gainedTimeMs <= 0) return 0.0f;
        long elapsed = System.currentTimeMillis() - gainedTimeMs;
        if (elapsed >= DISPLAY_DURATION_MS) return 0.0f;

        long fadeStart = DISPLAY_DURATION_MS - FADE_DURATION_MS;
        if (elapsed > fadeStart) {
            float fadeProgress = (float) (elapsed - fadeStart) / (float) FADE_DURATION_MS;
            return Math.max(0.0f, Math.min(1.0f, 1.0f - fadeProgress));
        }
        return 1.0f;
    }

    /**
     * 全状態リセット (ログアウト時・ワールド切断時等)
     */
    public synchronized void reset() {
        this.combatAccumulatedExp = 0;
        this.combatProgressPercentage = 0.0f;
        this.lastCombatGainedTimeMs = 0L;
        this.lastCombatPopTimeMs = 0L;

        this.profAccumulatedExp = 0;
        this.profProgressPercentage = 0.0f;
        this.activeProfDisplayName = DEFAULT_PROF_NAME;
        this.lastProfGainedTimeMs = 0L;
        this.lastProfPopTimeMs = 0L;

        LOGGER.debug("ExpAccumulatorManager fully reset");
    }
}
