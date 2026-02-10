package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.render.orb.OrbType;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * フレーム間で再利用可能なレンダリング状態を保持するクラス
 * 
 * 【パフォーマンス最適化】
 * - リストオブジェクトの再利用によるGCプレッシャー軽減
 * - 毎フレームのメモリ割り当てをゼロに近づける
 */
public class RenderState {
    
    // 再利用可能なリストバッファ（最大16個まで）
    private static final int MAX_BUFFER_SIZE = 16;
    private final List<OrbType> visibleOrbsBuffer = new ArrayList<>(MAX_BUFFER_SIZE);
    
    // 可視状態のキャッシュ
    private Player lastPlayer = null;
    private long lastUpdateTick = -1;
    private boolean needsUpdate = true;
    
    /**
     * 可視オーブリストを取得（キャッシュ付き）
     * 
     * @param player 対象プレイヤー
     * @param currentTick 現在のゲームティック
     * @return 可視オーブのリスト（変更不可ではない！直接変更しないこと）
     */
    public List<OrbType> getVisibleOrbs(Player player, long currentTick) {
        if (player == null) {
            visibleOrbsBuffer.clear();
            return visibleOrbsBuffer;
        }
        
        // ティックベースの更新判定（4ティック毎）
        if (needsUpdate || player != lastPlayer || currentTick - lastUpdateTick >= 4) {
            updateVisibleOrbs(player);
            lastPlayer = player;
            lastUpdateTick = currentTick;
            needsUpdate = false;
        }
        
        return visibleOrbsBuffer;
    }
    
    /**
     * 可視オーブリストを強制更新
     */
    public void invalidate() {
        needsUpdate = true;
    }
    
    /**
     * 可視オーブリストを更新（リストをクリアして再構築）
     */
    private void updateVisibleOrbs(Player player) {
        visibleOrbsBuffer.clear();
        
        for (OrbType orb : OrbType.values()) {
            try {
                if (orb.getConfig().isVisible(player)) {
                    visibleOrbsBuffer.add(orb);
                }
            } catch (Exception e) {
                // 個別の失敗は無視して次へ
            }
        }
    }
    
    /**
     * 現在のバッファサイズを取得
     */
    public int getVisibleOrbCount() {
        return visibleOrbsBuffer.size();
    }
    
    /**
     * インデックスアクセス（範囲チェック付き）
     */
    public OrbType getOrbAt(int index) {
        if (index < 0 || index >= visibleOrbsBuffer.size()) {
            return null;
        }
        return visibleOrbsBuffer.get(index);
    }
}
