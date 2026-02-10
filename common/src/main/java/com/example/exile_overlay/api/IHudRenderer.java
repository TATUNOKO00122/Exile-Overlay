package com.example.exile_overlay.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * HUDレンダラーの基底インターフェース
 * 
 * 【依存性逆転の原則】
 * - 全てのHUD要素はこのインターフェースを実装
 * - レンダリングエンジンは実装の詳細を知らない
 */
public interface IHudRenderer {
    
    /**
     * HUD要素を描画
     * 
     * @param graphics GUIグラフィックスコンテキスト
     * @param ctx レンダリングコンテキスト
     */
    void render(GuiGraphics graphics, RenderContext ctx);
    
    /**
     * このレンダラーが有効かどうか
     * 
     * @param ctx レンダリングコンテキスト
     * @return 描画すべき場合true
     */
    default boolean isVisible(RenderContext ctx) {
        return true;
    }
    
    /**
     * レンダラーの優先度（値が高いほど手前に描画）
     */
    default int getPriority() {
        return 100;
    }
    
    /**
     * レンダラーの一意なID
     */
    String getId();
}
