package com.example.exile_overlay.api;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

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
 * 
 * 【スレッド安全性】
 * - AtomicReferenceで状態を一元管理
 * - CAS操作で原子性を保証
 */
public class CircuitBreaker {
    
    public enum State {
        CLOSED,      // 正常動作中
        OPEN,        // ブロック中
        HALF_OPEN    // 回復試行中
    }
    
    private record StateSnapshot(State state, int failureCount, Instant lastFailureTime) {}
    
    private final int failureThreshold;
    private final long timeoutMillis;
    private final AtomicReference<StateSnapshot> snapshot;
    
    /**
     * @param failureThreshold ブロックまでの失敗閾値
     * @param timeoutMillis ブロック解除までのタイムアウト（ミリ秒）
     */
    public CircuitBreaker(int failureThreshold, long timeoutMillis) {
        this.failureThreshold = Math.max(1, failureThreshold);
        this.timeoutMillis = Math.max(1000, timeoutMillis);
        this.snapshot = new AtomicReference<>(new StateSnapshot(State.CLOSED, 0, null));
    }
    
    /**
     * 実行が許可されているかチェック
     */
    public boolean allowExecution() {
        StateSnapshot current = snapshot.get();
        
        return switch (current.state()) {
            case CLOSED -> true;
            
            case OPEN -> {
                if (current.lastFailureTime() != null) {
                    long elapsed = Duration.between(current.lastFailureTime(), Instant.now()).toMillis();
                    if (elapsed >= timeoutMillis) {
                        StateSnapshot halfOpen = new StateSnapshot(State.HALF_OPEN, 0, null);
                        if (snapshot.compareAndSet(current, halfOpen)) {
                            yield true;
                        }
                        yield allowExecution();
                    }
                }
                yield false;
            }
            
            case HALF_OPEN -> true;
        };
    }
    
    /**
     * 成功を記録
     */
    public void recordSuccess() {
        StateSnapshot current;
        StateSnapshot closed;
        
        do {
            current = snapshot.get();
            closed = new StateSnapshot(State.CLOSED, 0, null);
        } while (!snapshot.compareAndSet(current, closed));
    }
    
    /**
     * 失敗を記録
     */
    public void recordFailure() {
        Instant now = Instant.now();
        StateSnapshot current;
        StateSnapshot updated;
        
        do {
            current = snapshot.get();
            int newFailureCount = current.failureCount() + 1;
            
            if (current.state() == State.HALF_OPEN) {
                updated = new StateSnapshot(State.OPEN, newFailureCount, now);
            } else if (newFailureCount >= failureThreshold) {
                updated = new StateSnapshot(State.OPEN, newFailureCount, now);
            } else {
                updated = new StateSnapshot(current.state(), newFailureCount, now);
            }
        } while (!snapshot.compareAndSet(current, updated));
    }
    
    /**
     * 現在の状態を取得
     */
    public State getState() {
        return snapshot.get().state();
    }
    
    /**
     * 現在の失敗カウントを取得
     */
    public int getFailureCount() {
        return snapshot.get().failureCount();
    }
    
    /**
     * リセット（手動復旧用）
     */
    public void reset() {
        snapshot.set(new StateSnapshot(State.CLOSED, 0, null));
    }
    
    @Override
    public String toString() {
        StateSnapshot s = snapshot.get();
        return String.format("CircuitBreaker{state=%s, failures=%d/%d}", 
            s.state(), s.failureCount(), failureThreshold);
    }
}
