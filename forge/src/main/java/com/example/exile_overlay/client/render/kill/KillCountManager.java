package com.example.exile_overlay.client.render.kill;

/*
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/ **
 * キルカウント状態およびポップアニメーション管理クラス
 *
 * - セッション中のキル数をSingle Source of Truthとして一元管理
 * - プレイヤーがダメージを与えたエンティティの死亡追跡（クライアント・サーバー両対応）
 * - 二重カウント防止のTTL付き重複排除
 * - キル発生時のポップアニメーション倍率を時間ベース（フレームレート非依存）で計算
 * - ゼロアロケーション・スレッドセーフ
 * /
public final class KillCountManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/KillCountManager");
    private static final KillCountManager INSTANCE = new KillCountManager();

    private static final long ANIMATION_DURATION_MS = 350L;
    private static final float PEAK_SCALE = 1.35f;
    private static final long ATTACK_EXPIRY_MS = 6000L; // プレイヤーが攻撃してからキルとして認める猶予時間
    private static final long PROCESSED_EXPIRY_MS = 10000L;

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

    / **
     * プレイヤーがエンティティにダメージを与えた/攻撃したことを記録
     * /
    public void recordPlayerAttack(int entityId) {
        long now = System.currentTimeMillis();
        playerAttackedEntities.put(entityId, now);
    }

    / **
     * エンティティの死亡をチェックし、プレイヤーのキルであればカウント加算
     *
     * @param entity 対象エンティティ
     * @return キルとして新しくカウントされた場合true
     * /
    public synchronized boolean checkEntityDeath(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        int entityId = entity.getId();
        long now = System.currentTimeMillis();

        // 既に処理済みのエンティティはスキップ
        if (processedDeadEntities.containsKey(entityId)) {
            return false;
        }

        // プレイヤーが直近に攻撃したエンティティか確認
        Long attackTime = playerAttackedEntities.get(entityId);
        if (attackTime == null || (now - attackTime) > ATTACK_EXPIRY_MS) {
            return false;
        }

        // 死亡状態か確認 (HP <= 0, isDeadOrDying, または deathTime > 0)
        boolean isDead = entity.getHealth() <= 0.001f || entity.isDeadOrDying() || entity.deathTime > 0;
        if (!isDead) {
            return false;
        }

        // 処理済みマーク & 攻撃履歴から削除
        processedDeadEntities.put(entityId, now);
        playerAttackedEntities.remove(entityId);

        incrementKill();
        return true;
    }

    / **
     * キル数を1加算し、ポップアニメーションを開始
     * /
    public synchronized void incrementKill() {
        this.killCount++;
        this.lastKillTimeMs = System.currentTimeMillis();
        LOGGER.debug("Kill count incremented to {}", killCount);
    }

    / **
     * 現在のキル数を取得
     * /
    public synchronized int getKillCount() {
        return killCount;
    }

    / **
     * 現在のポップアニメーション拡大倍率を計算 (1.0f ~ 1.35f)
     * /
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

        // 最初の20%で一気に拡大 (1.0 -> 1.35)
        // 残り80%で滑らかに通常サイズに戻る (1.35 -> 1.0)
        float scaleDiff = PEAK_SCALE - 1.0f;
        if (progress < 0.2f) {
            float t = progress / 0.2f;
            return 1.0f + scaleDiff * (float) Math.sin(t * (Math.PI / 2.0));
        } else {
            float t = (progress - 0.2f) / 0.8f;
            // Ease-Out Quad減衰
            float decay = 1.0f - t;
            return 1.0f + scaleDiff * decay * decay;
        }
    }

    / **
     * 古いエンティティキャッシュのクリーンアップ（定期実行）
     * /
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

    / **
     * キルカウントをリセット
     * /
    public synchronized void reset() {
        this.killCount = 0;
        this.lastKillTimeMs = 0;
        this.playerAttackedEntities.clear();
        this.processedDeadEntities.clear();
        LOGGER.debug("Kill count reset to 0");
    }

    / **
     * キルカウントを特定の値に設定
     * /
    public synchronized void setKillCount(int count) {
        this.killCount = Math.max(0, count);
        this.lastKillTimeMs = System.currentTimeMillis();
    }
}
*/
