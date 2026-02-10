package com.example.exile_overlay.api;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * サーキットブレイカーパターン実装
 * 
 * 連続した失敗を検出し、一時的に処理をブロックすることで
 * システムの保護と回復を実現する
 * 
 * 【状態遷移】
 * CLOSED -> OPEN: 失敗閾値到達
 * OPEN -> HALF_OPEN: タイムアウト経過
 * HALF_OPEN -> CLOSED: 成功検出
 * HALF_OPEN -> OPEN: 失敗検出
 */
public class CircuitBreaker {
    
    public enum State {
        CLOSED,      // 正常動作中
        OPEN,        // ブロック中
        HALF_OPEN    // 回復試行中
    }
    
    private final int failureThreshold;
    private final long timeoutMillis;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private volatile Instant lastFailureTime = null;
    
    /**
     * @param failureThreshold ブロックまでの失敗閾値
     * @param timeoutMillis ブロック解除までのタイムアウト（ミリ秒）
     */
    public CircuitBreaker(int failureThreshold, long timeoutMillis) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.timeoutMillis = Math.max(1000, timeoutMillis);
    }
    
    /**
     * 実行が許可されているかチェック
     */
    public boolean allowExecution() {
        State currentState = state;
        
        switch (currentState) {
            case CLOSED:
                return true;
                
            case OPEN:
                // タイムアウト経過チェック
                if (lastFailureTime != null) {
                    long elapsed = Duration.between(lastFailureTime, Instant.now()).toMillis();
                    if (elapsed >= timeoutMillis) {
                        // HALF_OPENに遷移
                        state = State.HALF_OPEN;
                        failureCount.set(0);
                        return true;
                    }
                }
                return false;
                
            case HALF_OPEN:
                // HALF_OPEN状態では1回だけ実行を許可
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * 成功を記録
     */
    public void recordSuccess() {
        State currentState = state;
        
        if (currentState == State.HALF_OPEN) {
            // 回復成功 -> CLOSED
            state = State.CLOSED;
        }
        
        failureCount.set(0);
        lastFailureTime = null;
    }
    
    /**
     * 失敗を記録
     */
    public void recordFailure() {
        int failures = failureCount.incrementAndGet();
        lastFailureTime = Instant.now();
        
        State currentState = state;
        
        if (currentState == State.HALF_OPEN) {
            // 回復失敗 -> OPENに戻る
            state = State.OPEN;
        } else if (failures >= failureThreshold) {
            // 閾値到達 -> OPEN
            state = State.OPEN;
        }
    }
    
    /**
     * 現在の状態を取得
     */
    public State getState() {
        return state;
    }
    
    /**
     * 現在の失敗カウントを取得
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * リセット（手動復旧用）
     */
    public void reset() {
        state = State.CLOSED;
        failureCount.set(0);
        lastFailureTime = null;
    }
    
    @Override
    public String toString() {
        return String.format("CircuitBreaker{state=%s, failures=%d/%d}", 
            state, failureCount.get(), failureThreshold);
    }
}
