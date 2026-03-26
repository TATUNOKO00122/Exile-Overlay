package com.example.exile_overlay.api;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TTL（生存時間）付きエラーログキャッシュ
 * 
 * 【設計思想】
 * - 古いエラー履歴を自動的に削除
 * - 最大エントリ数によるメモリ保護
 * - 共有Executorによるスレッドリーク防止
 */
public class ErrorLogCache {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorLogCache.class);
    
    private static final SharedCleanupScheduler SHARED_SCHEDULER = new SharedCleanupScheduler();
    
    private final Map<String, ErrorEntry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final int maxEntries;
    private volatile boolean registered = false;
    
    /**
     * 共有クリーンアップスケジューラー
     * 全ErrorLogCacheインスタンスのクリーンアップを一元管理
     */
    private static class SharedCleanupScheduler {
        private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HudErrorLog-Cleanup");
            t.setDaemon(true);
            return t;
        });
        
        private final List<Runnable> cleanupTasks = new CopyOnWriteArrayList<>();
        
        void register(Runnable cleanupTask) {
            cleanupTasks.add(cleanupTask);
            if (cleanupTasks.size() == 1) {
                executor.scheduleAtFixedRate(this::runAll, 1, 1, TimeUnit.MINUTES);
            }
        }
        
        void unregister(Runnable cleanupTask) {
            cleanupTasks.remove(cleanupTask);
        }
        
        private void runAll() {
            for (Runnable task : cleanupTasks) {
                try {
                    task.run();
                } catch (Exception e) {
                    LOGGER.debug("Error during cleanup task: {}", e.getMessage());
                }
            }
        }
        
        void shutdown() {
            executor.shutdown();
        }
    }
    
    /**
     * エラーエントリ内部クラス
     */
    private static class ErrorEntry {
        final List<HudError> errors = new ArrayList<>();
        final Instant createdAt = Instant.now();
        volatile Instant lastAccessed = Instant.now();
        
        boolean isExpired(Duration ttl) {
            return Duration.between(lastAccessed, Instant.now()).compareTo(ttl) > 0;
        }
        
        void touch() {
            lastAccessed = Instant.now();
        }
    }
    
    /**
     * デフォルトコンストラクタ
     * TTL: 5分、最大エントリ: 100
     */
    public ErrorLogCache() {
        this(Duration.ofMinutes(5), 100);
    }
    
    /**
     * @param ttl エラー履歴の生存時間
     * @param maxEntries 最大エントリ数
     */
    public ErrorLogCache(Duration ttl, int maxEntries) {
        this.ttl = ttl;
        this.maxEntries = maxEntries;
        
        SHARED_SCHEDULER.register(this::cleanup);
        registered = true;
    }
    
    /**
     * エラーを追加
     */
    public void add(String commandId, HudError error) {
        if (cache.size() >= maxEntries && !cache.containsKey(commandId)) {
            removeOldestEntry();
        }
        
        ErrorEntry entry = cache.computeIfAbsent(commandId, k -> new ErrorEntry());
        entry.errors.add(error);
        entry.touch();
        
        while (entry.errors.size() > 10) {
            entry.errors.remove(0);
        }
    }
    
    /**
     * エラー履歴を取得
     */
    public List<HudError> get(String commandId) {
        ErrorEntry entry = cache.get(commandId);
        if (entry == null) {
            return List.of();
        }
        
        entry.touch();
        return List.copyOf(entry.errors);
    }
    
    /**
     * 特定のコマンドのエラー履歴をクリア
     */
    public void clear(String commandId) {
        cache.remove(commandId);
    }
    
    /**
     * 全エラー履歴をクリア
     */
    public void clearAll() {
        cache.clear();
    }
    
    /**
     * エントリ数を取得
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * クリーンアップ実行
     */
    private void cleanup() {
        try {
            int beforeSize = cache.size();
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttl));
            int removed = beforeSize - cache.size();
            
            if (removed > 0) {
                LOGGER.debug("Cleaned up {} expired error entries", removed);
            }
        } catch (Exception e) {
            LOGGER.error("Error during cleanup", e);
        }
    }
    
    /**
     * 最も古いエントリを削除
     */
    private void removeOldestEntry() {
        Instant oldest = Instant.now();
        String oldestKey = null;
        
        for (Map.Entry<String, ErrorEntry> entry : cache.entrySet()) {
            if (entry.getValue().createdAt.isBefore(oldest)) {
                oldest = entry.getValue().createdAt;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
            LOGGER.debug("Removed oldest error entry: {}", oldestKey);
        }
    }
    
    /**
     * シャットダウン（リソース解放）
     */
    public void shutdown() {
        if (registered) {
            SHARED_SCHEDULER.unregister(this::cleanup);
            registered = false;
        }
    }
    
    /**
     * アプリケーション終了時に呼び出す
     */
    public static void shutdownAll() {
        SHARED_SCHEDULER.shutdown();
    }
}
