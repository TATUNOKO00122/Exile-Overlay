package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.IHudRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HUD要素のスナップ計算を行うクラス
 * 
 * 【責任】
 * - 画面端への吸着計算
 * - スナップガイド位置の管理
 * - 座標系（中心基準/左上基準/底辺中心基準）を考慮した計算
 */
public class SnapCalculator {
    
    private final int snapDistance;
    private final int screenWidth;
    private final int screenHeight;
    private final List<Integer> activeSnapGuidesX = new ArrayList<>();
    private final List<Integer> activeSnapGuidesY = new ArrayList<>();
    
    public SnapCalculator(int snapDistance, int screenWidth, int screenHeight) {
        this.snapDistance = snapDistance;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }
    
    /**
     * スナップガイドをクリア
     */
    public void clearGuides() {
        activeSnapGuidesX.clear();
        activeSnapGuidesY.clear();
    }
    
    /**
     * X座標にスナップを適用
     * 
     * @param rawX 生のX座標
     * @param elementWidth 要素の幅
     * @param isCenterBased trueの場合Xは中心、falseの場合Xは左端
     * @return スナップ後のX座標
     */
    public int applySnapX(int rawX, int elementWidth, boolean isCenterBased) {
        if (isCenterBased) {
            // 中心基準: 左端スナップ時は中心が elementWidth/2 の位置に
            int leftEdgeX = rawX - elementWidth / 2;
            if (Math.abs(leftEdgeX) <= snapDistance) {
                activeSnapGuidesX.add(0);
                return elementWidth / 2;
            }
            
            // 右端スナップ: 中心が width - elementWidth/2 の位置に
            int rightEdgeX = rawX + elementWidth / 2;
            if (Math.abs(rightEdgeX - screenWidth) <= snapDistance) {
                activeSnapGuidesX.add(screenWidth);
                return screenWidth - elementWidth / 2;
            }
        } else {
            // 左上基準: 従来通り
            if (Math.abs(rawX) <= snapDistance) {
                activeSnapGuidesX.add(0);
                return 0;
            }
            
            if (Math.abs(rawX + elementWidth - screenWidth) <= snapDistance) {
                activeSnapGuidesX.add(screenWidth);
                return screenWidth - elementWidth;
            }
        }
        
        return rawX;
    }
    
    /**
     * Y座標にスナップを適用
     * 
     * @param rawY 生のY座標
     * @param elementHeight 要素の高さ
     * @param isBottomBased trueの場合Yは底辺、falseの場合Yは上端
     * @return スナップ後のY座標
     */
    public int applySnapY(int rawY, int elementHeight, boolean isBottomBased) {
        if (isBottomBased) {
            // 底辺基準: 下端スナップのみ有効
            // 底辺が画面下端に一致
            if (Math.abs(rawY - screenHeight) <= snapDistance) {
                activeSnapGuidesY.add(screenHeight);
                return screenHeight;
            }
            
            // 上端スナップ: 底辺が elementHeight の位置（上端が0）
            int topEdgeY = rawY - elementHeight;
            if (Math.abs(topEdgeY) <= snapDistance) {
                activeSnapGuidesY.add(0);
                return elementHeight;
            }
        } else {
            // 上端基準: 従来通り
            if (Math.abs(rawY) <= snapDistance) {
                activeSnapGuidesY.add(0);
                return 0;
            }
            
            if (Math.abs(rawY + elementHeight - screenHeight) <= snapDistance) {
                activeSnapGuidesY.add(screenHeight);
                return screenHeight - elementHeight;
            }
        }
        
        return rawY;
    }
    
    /**
     * アクティブなX方向スナップガイドを取得
     */
    public List<Integer> getActiveSnapGuidesX() {
        return Collections.unmodifiableList(activeSnapGuidesX);
    }
    
    /**
     * アクティブなY方向スナップガイドを取得
     */
    public List<Integer> getActiveSnapGuidesY() {
        return Collections.unmodifiableList(activeSnapGuidesY);
    }
    
    /**
     * スナップ結果を保持するレコード
     */
    public record SnapResult(int x, int y, List<Integer> guidesX, List<Integer> guidesY) {
    }
    
    /**
     * 一括でスナップ計算を実行
     * 
     * @param rawX 生のX座標
     * @param rawY 生のY座標
     * @param elementWidth 要素の幅
     * @param elementHeight 要素の高さ
     * @param metadata レンダリングメタデータ
     * @return スナップ結果
     */
    public SnapResult calculateSnap(int rawX, int rawY, int elementWidth, int elementHeight,
                                    IHudRenderer.HudRenderMetadata metadata) {
        clearGuides();
        
        boolean isCenterX = !metadata.isTopLeftBased();
        boolean isBottomY = metadata.isBottomCenterBased();
        
        int snappedX = applySnapX(rawX, elementWidth, isCenterX);
        int snappedY = applySnapY(rawY, elementHeight, isBottomY);
        
        return new SnapResult(snappedX, snappedY, 
                              new ArrayList<>(activeSnapGuidesX), 
                              new ArrayList<>(activeSnapGuidesY));
    }
}
