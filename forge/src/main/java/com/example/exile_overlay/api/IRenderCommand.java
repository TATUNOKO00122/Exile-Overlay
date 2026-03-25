package com.example.exile_overlay.api;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 個別のレンダリングコマンドを表すインターフェース
 * 
 * 【設計思想】
 * - 各HUD要素は独立したコマンドとして実装
 * - 優先度による描画順序の制御
 * - 可視性チェックの分離
 * 
 * 【パフォーマンス最適化】
 * - isVisible()で事前に描画対象か判定
 * - execute()内では無条件で描画のみ実行
 */
public interface IRenderCommand {
    
    /**
     * コマンドの一意なID
     * デバッグ・ログ・設定用
     */
    String getId();
    
    /**
     * 実際の描画処理を実行
     * 
     * @param graphics GUIグラフィックスコンテキスト
     * @param ctx レンダリングコンテキスト
     */
    void execute(GuiGraphics graphics, RenderContext ctx);
    
    /**
     * このコマンドが可視かどうかを判定
     * execute()呼び出し前にチェックされる
     * 
     * @param ctx レンダリングコンテキスト
     * @return 描画すべき場合true
     */
    default boolean isVisible(RenderContext ctx) {
        return true;
    }
    
    /**
     * 描画レイヤーを取得
     * 同じレイヤー内では優先度順に描画
     * 
     * @return このコマンドが属するレイヤー
     */
    default RenderLayer getLayer() {
        return RenderLayer.FILL;
    }
    
    /**
     * 同レイヤー内での描画優先度
     * 値が大きいほど手前に描画
     * 
     * @return 優先度（デフォルト100）
     */
    default int getPriority() {
        return 100;
    }
}
