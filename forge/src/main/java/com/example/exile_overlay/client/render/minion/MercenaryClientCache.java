package com.example.exile_overlay.client.render.minion;

import com.example.exile_overlay.api.data.MercenaryDisplayInfo;

import java.util.concurrent.atomic.AtomicReference;

/**
 * サーバーから受信した傭兵の同期データを保持するクライアントキャッシュ
 */
public final class MercenaryClientCache {

    private static final long TTL_MILLIS = 3000L; // 3秒間有効
    private static final AtomicReference<CacheEntry> CACHE = new AtomicReference<>(null);

    public record CacheEntry(
            MercenaryDisplayInfo displayInfo,
            long timestamp
    ) {}

    private MercenaryClientCache() {}

    public static void update(MercenaryDisplayInfo info) {
        if (info == null) {
            CACHE.set(null);
        } else {
            CACHE.set(new CacheEntry(info, System.currentTimeMillis()));
        }
    }

    public static void clear() {
        CACHE.set(null);
    }

    public static MercenaryDisplayInfo get() {
        CacheEntry entry = CACHE.get();
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.timestamp() > TTL_MILLIS) {
            CACHE.compareAndSet(entry, null);
            return null;
        }
        return entry.displayInfo();
    }
}
