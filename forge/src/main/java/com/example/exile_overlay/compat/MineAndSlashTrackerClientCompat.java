package com.example.exile_overlay.compat;

import com.example.exile_overlay.client.render.HudRenderManager;
import com.example.exile_overlay.dmgtracker.gui.overlay.DamageTrackerOverlay;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Mine and Slash とのダメージトラッカー関連のクライアント専用互換ブリッジクラス。
 * 物理サーバー等の非クライアント環境でロードされないよう、クライアントサイドからのみ呼び出すこと。
 */
@OnlyIn(Dist.CLIENT)
public class MineAndSlashTrackerClientCompat {

    /**
     * DamageTrackerOverlay を HudRenderManager に登録する
     * 
     * @param manager HUDレンダリングマネージャー
     */
    public static void registerRenderer(HudRenderManager manager) {
        if (manager != null) {
            manager.registerCommand(new DamageTrackerOverlay(), 70);
        }
    }
}
