package com.example.exile_overlay.api;

/**
 * データ更新頻度。
 * 各列挙子のフレーム間隔で60FPS想定で決めてある。
 * CRITICAL=毎フレーム、NORMAL=15f（約250ms）、SLOW=60f（約1s）、STATIC=300f（約5s）。
 */
public enum UpdateFrequency {
    
    /**
     * クリティカル: 毎フレーム更新
     * 使用例: HP, シールド（即座に反映が必要）
     */
    CRITICAL(0, 0),
    
    /**
     * 通常: 15フレーム間隔（約250ms@60FPS）
     * 使用例: Mana, Energy, Blood
     */
    NORMAL(15, 250),
    
    /**
     * 遅い: 60フレーム間隔（約1000ms@60FPS）
     * 使用例: 経験値, レベル
     */
    SLOW(60, 1000),
    
    /**
     * 静的: 300フレーム間隔（約5000ms@60FPS）
     * 使用例: 最大HP, 最大Mana（頻繁に変わらない）
     */
    STATIC(300, 5000);
    
    // 60FPSを想定した1フレームのミリ秒数
    private static final float MS_PER_FRAME = 1000.0f / 60.0f;
    
    private final int frameInterval;
    private final int millisInterval;
    
    UpdateFrequency(int frameInterval, int millisInterval) {
        this.frameInterval = frameInterval;
        this.millisInterval = millisInterval;
    }
    
    /**
     * フレーム間隔を取得
     * 0の場合は毎フレーム更新
     */
    public int getFrameInterval() {
        return frameInterval;
    }
    
    /**
     * ミリ秒間隔を取得
     */
    public int getMillisInterval() {
        return millisInterval;
    }
    
    /**
     * 更新が必要かどうかを判定
     * 
     * @param frameCounter 現在のフレームカウンター
     * @return 更新が必要な場合true
     */
    public boolean shouldUpdate(int frameCounter) {
        if (frameInterval == 0) {
            return true;  // CRITICALは毎フレーム
        }
        return frameCounter >= frameInterval;
    }
    
    /**
     * ミリ秒から最適なUpdateFrequencyを推定
     * 
     * @param millis ミリ秒
     * @return 推定されたUpdateFrequency
     */
    public static UpdateFrequency fromMillis(int millis) {
        if (millis <= 50) {
            return CRITICAL;
        } else if (millis <= 500) {
            return NORMAL;
        } else if (millis <= 2000) {
            return SLOW;
        } else {
            return STATIC;
        }
    }
    
    /**
     * フレーム数から最適なUpdateFrequencyを推定
     * 
     * @param frames フレーム数
     * @return 推定されたUpdateFrequency
     */
    public static UpdateFrequency fromFrames(int frames) {
        if (frames <= 1) {
            return CRITICAL;
        } else if (frames <= 30) {
            return NORMAL;
        } else if (frames <= 120) {
            return SLOW;
        } else {
            return STATIC;
        }
    }
}
