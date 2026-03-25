package com.example.exile_overlay.api;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * 描画レイヤーを定義する列挙型
 * 
 * 【設計思想】
 * - レイヤーごとに最適な描画設定を適用
 * - ブレンドモード、深度テスト等を自動設定
 * - バッチ処理による描画効率化
 * 
 * 【描画順序】
 * 1. BACKGROUND - 最背面、不透明
 * 2. FILL - シェーダー描画（ブレンド有効）
 * 3. FRAME - フレーム描画
 * 4. OVERLAY - 半透明エフェクト
 * 5. TEXT - テキスト描画（フォントキャッシュ利用）
 * 6. DEBUG - デバッグ情報（最前面）
 */
public enum RenderLayer {
    
    /**
     * 背景レイヤー
     * - 最背面に描画
     * - 不透明テクスチャ
     * - ブレンド無効（パフォーマンス最適化）
     */
    BACKGROUND(0, false, false),
    
    /**
     * フィルレイヤー
     * - オーブの液面など
     * - ブレンド有効（アルファ合成）
     * - シェーダー使用
     */
    FILL(100, true, false),
    
    /**
     * フレームレイヤー
     * - UI枠線、背景フレーム
     * - ブレンド設定はコンテンツ依存
     */
    FRAME(200, false, false),
    
    /**
     * オーバーレイレイヤー
     * - 半透明エフェクト
     * - 反射、グロー効果
     * - アドティブブレンド可能
     */
    OVERLAY(300, true, false),
    
    /**
     * テキストレイヤー
     * - フォント描画
     * - フォントテクスチャキャッシュ利用
     * - サブピクセルレンダリング対応
     */
    TEXT(400, true, false),
    
    /**
     * デバッグレイヤー
     * - 最前面に描画
     * - デバッグ情報、枠線等
     */
    DEBUG(500, true, false);
    
    private final int order;
    private final boolean enableBlend;
    private final boolean depthTest;
    
    RenderLayer(int order, boolean enableBlend, boolean depthTest) {
        this.order = order;
        this.enableBlend = enableBlend;
        this.depthTest = depthTest;
    }
    
    /**
     * 描画順序を取得
     * 値が小さいほど奥に描画
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * ブレンドを有効にするか
     */
    public boolean isEnableBlend() {
        return enableBlend;
    }
    
    /**
     * 深度テストを有効にするか
     */
    public boolean isDepthTest() {
        return depthTest;
    }
    
    /**
     * このレイヤーの描画前にOpenGL状態を設定
     */
    public void setupRenderState() {
        if (enableBlend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.disableBlend();
        }
        
        if (depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }
    }
    
    /**
     * このレイヤーの描画後にOpenGL状態を復元
     */
    public void cleanupRenderState() {
        // デフォルト状態に戻す
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }
}
