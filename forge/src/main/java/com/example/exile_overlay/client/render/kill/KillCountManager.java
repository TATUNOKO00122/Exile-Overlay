package com.example.exile_overlay.client.render.kill;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * キルカウント状態およびポップアニメーション管理クラス
 * セッション中のキル数管理と撃破時ポップアニメーション倍率の計算を担当
 */
public final class KillCountManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/KillCountManager");
    private static final KillCountManager INSTANCE = new KillCountManager();

    private static final long ANIMATION_DURATION_MS = 350L;
    private static final float PEAK_SCALE = 1.35f;
    private static final long ATTACK_EXPIRY_MS = 12000L;
    private static final long PROCESSED_EXPIRY_MS = 20000L;

    private int killCount = 0;
    private long lastKillTimeMs = 0;

    // プレイヤーが攻撃したエンティティ (EntityId -> 最終攻撃時刻)
    private final Map<Integer, Long> playerAttackedEntities = new ConcurrentHashMap<>();
    // 既にキルカウント処理済みのエンティティ (EntityId -> 処理時刻)
    private final Map<Integer, Long> processedDeadEntities = new ConcurrentHashMap<>();

    private KillCountManager() {
    }

    public static KillCountManager getInstance() {
        return INSTANCE;
    }

    public void recordPlayerAttack(int entityId) {
        long now = System.currentTimeMillis();
        playerAttackedEntities.put(entityId, now);
    }

    public synchronized boolean checkEntityDeath(LivingEntity entity) {
        return checkEntityDeath(entity, false);
    }

    public synchronized boolean checkEntityDeath(LivingEntity entity, boolean assumeDead) {
        if (entity == null) {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && entity.getId() == mc.player.getId()) {
            reset();
            return false;
        }

        int entityId = entity.getId();
        long now = System.currentTimeMillis();

        if (processedDeadEntities.containsKey(entityId)) {
            return false;
        }

        Long attackTime = playerAttackedEntities.get(entityId);
        if (attackTime == null || (now - attackTime) > ATTACK_EXPIRY_MS) {
            return false;
        }

        if (!assumeDead) {
            boolean isDead = entity.getHealth() <= 0.001f || entity.isDeadOrDying() || entity.deathTime > 0;
            if (!isDead) {
                return false;
            }
        }

        processedDeadEntities.put(entityId, now);
        playerAttackedEntities.remove(entityId);

        incrementKill();
        return true;
    }

    public synchronized void incrementKill() {
        this.killCount++;
        this.lastKillTimeMs = System.currentTimeMillis();
        LOGGER.debug("Kill count incremented to {}", killCount);
    }

    public synchronized int getKillCount() {
        return killCount;
    }

    public float getScaleMultiplier() {
        long lastTime = this.lastKillTimeMs;
        if (lastTime <= 0) {
            return 1.0f;
        }

        long elapsed = System.currentTimeMillis() - lastTime;
        if (elapsed >= ANIMATION_DURATION_MS) {
            return 1.0f;
        }

        float progress = (float) elapsed / (float) ANIMATION_DURATION_MS;

        // 最初の20%で拡大、残り80%で元に戻る
        float scaleDiff = PEAK_SCALE - 1.0f;
        if (progress < 0.2f) {
            float t = progress / 0.2f;
            return 1.0f + scaleDiff * (float) Math.sin(t * (Math.PI / 2.0));
        } else {
            float t = (progress - 0.2f) / 0.8f;
            float decay = 1.0f - t;
            return 1.0f + scaleDiff * decay * decay;
        }
    }

    public void cleanupOldEntries() {
        long now = System.currentTimeMillis();
        playerAttackedEntities.entrySet().removeIf(e -> (now - e.getValue()) > ATTACK_EXPIRY_MS);
        processedDeadEntities.entrySet().removeIf(e -> (now - e.getValue()) > PROCESSED_EXPIRY_MS);

        // 時間経過でキルカウンターをリセット
        long timeoutMs = KillCounterConfig.getInstance().getTimeoutSeconds() * 1000L;
        if (killCount > 0 && (now - lastKillTimeMs) > timeoutMs) {
            reset();
        }
    }

    public synchronized void reset() {
        this.killCount = 0;
        this.lastKillTimeMs = 0;
        this.playerAttackedEntities.clear();
        this.processedDeadEntities.clear();
        LOGGER.debug("Kill count reset to 0");
    }

    public synchronized void setKillCount(int count) {
        this.killCount = Math.max(0, count);
        this.lastKillTimeMs = System.currentTimeMillis();
    }
}
