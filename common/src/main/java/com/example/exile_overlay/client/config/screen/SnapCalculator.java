package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.IHudRenderer;

import java.util.Collections;
import java.util.List;

/**
 * HUD要素の境界制限計算を行うクラス
 *
 * 【責任】
 * - 画面端を超えないよう座標を制限
 * - 座標系（中心基準/左上基準/底辺中心基準）を考慮した計算
 */
public class SnapCalculator {

    private final int screenWidth;
    private final int screenHeight;

    public SnapCalculator(int snapDistance, int screenWidth, int screenHeight) {
        // snapDistanceは互換性のために残すが使用しない
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    /**
     * X座標を画面境界内に制限
     *
     * @param rawX 生のX座標
     * @param elementWidth 要素の幅
     * @param isCenterBased trueの場合Xは中心、falseの場合Xは左端
     * @param expansionLeft 左方向の拡張領域
     * @param expansionRight 右方向の拡張領域
     * @return 境界制限後のX座標
     */
    public int applySnapX(int rawX, int elementWidth, boolean isCenterBased, int expansionLeft, int expansionRight) {
        if (isCenterBased) {
            // 中心基準: 中心は要素の半分以上右に、半分以下左に移動可能
            int minX = elementWidth / 2 + expansionLeft;
            int maxX = screenWidth - elementWidth / 2 - expansionRight;
            return Math.max(minX, Math.min(rawX, maxX));
        } else {
            // 左上基準: 左端は0以上、右端は画面幅以下に制限
            int minX = expansionLeft;
            int maxX = screenWidth - elementWidth - expansionRight;
            return Math.max(minX, Math.min(rawX, maxX));
        }
    }

    /**
     * Y座標を画面境界内に制限
     *
     * @param rawY 生のY座標
     * @param elementHeight 要素の高さ
     * @param isBottomBased trueの場合Yは底辺
     * @param isCenterBased trueの場合Yは中心
     * @param expansionTop 上方向の拡張領域
     * @param expansionBottom 下方向の拡張領域
     * @return 境界制限後のY座標
     */
    public int applySnapY(int rawY, int elementHeight, boolean isBottomBased, boolean isCenterBased,
                          int expansionTop, int expansionBottom) {
        if (isBottomBased) {
            // 底辺中心基準: 底辺は要素高さ以上、画面高さ以下に制限
            int minY = elementHeight + expansionTop;
            int maxY = screenHeight - expansionBottom;
            return Math.max(minY, Math.min(rawY, maxY));
        } else if (isCenterBased) {
            // 中心基準: 中心は要素の半分以上下に、半分以上上に移動可能
            int minY = elementHeight / 2 + expansionTop;
            int maxY = screenHeight - elementHeight / 2 - expansionBottom;
            return Math.max(minY, Math.min(rawY, maxY));
        } else {
            // 上端基準: 上端は0以上、下端は画面高さ以下に制限
            int minY = expansionTop;
            int maxY = screenHeight - elementHeight - expansionBottom;
            return Math.max(minY, Math.min(rawY, maxY));
        }
    }

    /**
     * アクティブなX方向スナップガイドを取得（常に空リスト）
     * 互換性のために残す
     */
    public List<Integer> getActiveSnapGuidesX() {
        return Collections.emptyList();
    }

    /**
     * アクティブなY方向スナップガイドを取得（常に空リスト）
     * 互換性のために残す
     */
    public List<Integer> getActiveSnapGuidesY() {
        return Collections.emptyList();
    }

    /**
     * スナップガイドをクリア（何もしない）
     * 互換性のために残す
     */
    public void clearGuides() {
        // スナップ機能が削除されたため何もしない
    }
    
    /**
     * スナップ結果を保持するレコード（互換性のために残す）
     */
    public record SnapResult(int x, int y, List<Integer> guidesX, List<Integer> guidesY) {
    }

    /**
     * 一括で境界制限計算を実行
     *
     * @param rawX 生のX座標
     * @param rawY 生のY座標
     * @param elementWidth 要素の幅
     * @param elementHeight 要素の高さ
     * @param metadata レンダリングメタデータ
     * @return 境界制限後の結果
     */
    public SnapResult calculateSnap(int rawX, int rawY, int elementWidth, int elementHeight,
                                    IHudRenderer.HudRenderMetadata metadata) {
        boolean isCenterX = !metadata.isTopLeftBased();
        boolean isBottomY = metadata.isBottomCenterBased();
        boolean isCenterY = metadata.isCenterBased();

        IHudRenderer.Insets expansion = metadata.getExpansion();

        int clampedX = applySnapX(rawX, elementWidth, isCenterX, expansion.left, expansion.right);
        int clampedY = applySnapY(rawY, elementHeight, isBottomY, isCenterY, expansion.top, expansion.bottom);

        return new SnapResult(clampedX, clampedY,
                              Collections.emptyList(),
                              Collections.emptyList());
    }
}
