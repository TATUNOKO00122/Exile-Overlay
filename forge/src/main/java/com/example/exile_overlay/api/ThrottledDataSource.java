package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * データ更新頻度を制御するデータソースラッパー
 * 
 * 【パフォーマンス最適化】
 * - 更新頻度の低いデータを間引き
 * - System.currentTimeMillis()を使用せず、フレームカウンターベースで更新
 * - 毎フレームの無駄なデータ取得を防止
 * 
 * 【計算式】
 * 60FPS想定: 16.67ms/フレーム
 * 250ms間隔 → 15フレーム
 * 1000ms間隔 → 60フレーム
 */
public class ThrottledDataSource implements IDataSource {
    
    // 60FPSを想定した変換定数（16.67ms/フレーム）
    private static final float MS_PER_FRAME = 1000.0f / 60.0f;
    
    private final IDataSource delegate;
    private final int updateIntervalFrames;
    
    private int frameCounter = 0;
    private float cachedValue = DEFAULT_VALUE;
    private float cachedMaxValue = DEFAULT_VALUE;
    private boolean cacheValid = false;
    
    /**
     * コンストラクタ
     * 
     * @param delegate 元のデータソース
     * @param updateIntervalMs 更新間隔（ミリ秒）
     */
    public ThrottledDataSource(IDataSource delegate, long updateIntervalMs) {
        this.delegate = delegate;
        // ミリ秒をフレーム数に変換（最小1フレーム）
        this.updateIntervalFrames = Math.max(1, (int) (updateIntervalMs / MS_PER_FRAME));
    }
    
    @Override
    public float getValue(Player player) {
        updateIfNeeded(player);
        return cachedValue;
    }
    
    @Override
    public float getMaxValue(Player player) {
        updateIfNeeded(player);
        return cachedMaxValue;
    }
    
    /**
     * フレームカウンターベースで更新判定
     * System.currentTimeMillis()を使用しない
     */
    private void updateIfNeeded(Player player) {
        frameCounter++;
        
        if (frameCounter >= updateIntervalFrames || !cacheValid) {
            try {
                cachedValue = delegate.getValue(player);
                cachedMaxValue = delegate.getMaxValue(player);
                cacheValid = true;
            } catch (Exception e) {
                // 連続失敗時のみデフォルト値にフォールバック
                // frameCounterが大きくなりすぎた場合（約10回の更新間隔）
                if (frameCounter >= updateIntervalFrames * 10) {
                    cachedValue = DEFAULT_VALUE;
                    cachedMaxValue = DEFAULT_VALUE;
                    cacheValid = true;  // デフォルト値で有効化
                }
            }
            frameCounter = 0;
        }
    }
    
    @Override
    public boolean isAvailable() {
        return delegate.isAvailable();
    }
    
    @Override
    public String getId() {
        return delegate.getId() + "_throttled_" + updateIntervalFrames + "f";
    }
    
    /**
     * キャッシュを強制無効化
     */
    public void invalidate() {
        cacheValid = false;
        frameCounter = 0;
    }
    
    /**
     * 更新間隔を変更（ミリ秒）
     */
    public void setUpdateInterval(long intervalMs) {
        // 新しい間隔を設定
        int newInterval = Math.max(1, (int) (intervalMs / MS_PER_FRAME));
        // 現在のカウンターが新しい間隔を超えていれば即更新
        if (frameCounter >= newInterval) {
            invalidate();
        }
    }
    
    /**
     * 現在のフレーム間隔を取得
     */
    public int getUpdateIntervalFrames() {
        return updateIntervalFrames;
    }
}
