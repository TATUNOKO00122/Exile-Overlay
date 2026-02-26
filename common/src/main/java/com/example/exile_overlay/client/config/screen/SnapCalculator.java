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
     * @param expansionLeft 左方向の拡張領域
     * @param expansionRight 右方向の拡張領域
     * @return スナップ後のX座標
     */
    public int applySnapX(int rawX, int elementWidth, boolean isCenterBased, int expansionLeft, int expansionRight) {
        if (isCenterBased) {
            // 中心基準: 実際の左端 = 中心 - elementWidth/2 - expansionLeft
            int actualLeftEdge = rawX - elementWidth / 2 - expansionLeft;
            if (Math.abs(actualLeftEdge) <= snapDistance) {
                activeSnapGuidesX.add(0);
                return elementWidth / 2 + expansionLeft;
            }
            
            // 右端スナップ: 実際の右端 = 中心 + elementWidth/2 + expansionRight
            int actualRightEdge = rawX + elementWidth / 2 + expansionRight;
            if (Math.abs(actualRightEdge - screenWidth) <= snapDistance) {
                activeSnapGuidesX.add(screenWidth);
                return screenWidth - elementWidth / 2 - expansionRight;
            }
        } else {
            // 左上基準: expansionを含めた実際の端で判定
            int actualLeftEdge = rawX - expansionLeft;
            if (Math.abs(actualLeftEdge) <= snapDistance) {
                activeSnapGuidesX.add(0);
                return expansionLeft;
            }
            
            int actualRightEdge = rawX + elementWidth + expansionRight;
            if (Math.abs(actualRightEdge - screenWidth) <= snapDistance) {
                activeSnapGuidesX.add(screenWidth);
                return screenWidth - elementWidth - expansionRight;
            }
        }
        
        return rawX;
    }
    
    /**
     * Y座標にスナップを適用
     * 
     * @param rawY 生のY座標
     * @param elementHeight 要素の高さ
     * @param isBottomBased trueの場合Yは底辺
     * @param isCenterBased trueの場合Yは中心
     * @param expansionTop 上方向の拡張領域
     * @param expansionBottom 下方向の拡張領域
     * @return スナップ後のY座標
     */
    public int applySnapY(int rawY, int elementHeight, boolean isBottomBased, boolean isCenterBased,
                          int expansionTop, int expansionBottom) {
        if (isBottomBased) {
            // 底辺中心基準: 実際の底辺 = rawY + expansionBottom
            int actualBottom = rawY + expansionBottom;
            if (Math.abs(actualBottom - screenHeight) <= snapDistance) {
                activeSnapGuidesY.add(screenHeight);
                return screenHeight - expansionBottom;
            }
            
            // 上端スナップ: 実際の上端 = 底辺 - elementHeight - expansionTop
            int actualTop = rawY - elementHeight - expansionTop;
            if (Math.abs(actualTop) <= snapDistance) {
                activeSnapGuidesY.add(0);
                return elementHeight + expansionTop;
            }
        } else if (isCenterBased) {
            // 中心基準: 実際の上端 = 中心 - elementHeight/2 - expansionTop
            int actualTop = rawY - elementHeight / 2 - expansionTop;
            if (Math.abs(actualTop) <= snapDistance) {
                activeSnapGuidesY.add(0);
                return elementHeight / 2 + expansionTop;
            }
            
            // 下端スナップ: 実際の下端 = 中心 + elementHeight/2 + expansionBottom
            int actualBottom = rawY + elementHeight / 2 + expansionBottom;
            if (Math.abs(actualBottom - screenHeight) <= snapDistance) {
                activeSnapGuidesY.add(screenHeight);
                return screenHeight - elementHeight / 2 - expansionBottom;
            }
        } else {
            // 上端基準: expansionを含めた実際の端で判定
            int actualTop = rawY - expansionTop;
            if (Math.abs(actualTop) <= snapDistance) {
                activeSnapGuidesY.add(0);
                return expansionTop;
            }
            
            int actualBottom = rawY + elementHeight + expansionBottom;
            if (Math.abs(actualBottom - screenHeight) <= snapDistance) {
                activeSnapGuidesY.add(screenHeight);
                return screenHeight - elementHeight - expansionBottom;
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
        boolean isCenterY = metadata.isCenterBased();
        
        IHudRenderer.Insets expansion = metadata.getExpansion();
        
        int snappedX = applySnapX(rawX, elementWidth, isCenterX, expansion.left, expansion.right);
        int snappedY = applySnapY(rawY, elementHeight, isBottomY, isCenterY, expansion.top, expansion.bottom);
        
        return new SnapResult(snappedX, snappedY, 
                              new ArrayList<>(activeSnapGuidesX), 
                              new ArrayList<>(activeSnapGuidesY));
    }
}
