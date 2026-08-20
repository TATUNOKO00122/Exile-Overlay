package com.example.exile_overlay.compat;

import com.example.exile_overlay.client.render.GatewayBossBarRenderer;
import com.example.exile_overlay.client.render.HudRenderManager;

/**
 * Gateways to Eternity との互換ブリッジクラス。
 * gateways MODが有効な場合にのみ呼び出すこと。
 */
public class GatewaysCompat {

    /**
     * GatewayBossBarRenderer を HudRenderManager に登録する
     * 
     * @param manager HUDレンダリングマネージャー
     */
    public static void register(HudRenderManager manager) {
        if (manager != null) {
            manager.registerCommand(new GatewayBossBarRenderer(), 88);
        }
    }
}
