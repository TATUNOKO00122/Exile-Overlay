package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.IRenderCommand;

import java.util.Collections;
import java.util.List;

/**
 * HUD要素の境界制限計算を行うクラス
 *
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
    public int applySnapX(int rawX, int elementWidth, boolean isCenterBased) {
        if (isCenterBased) {
            int minX = elementWidth / 2;
            int maxX = screenWidth - elementWidth / 2;
            if (minX > maxX) return screenWidth / 2;
            return Math.max(minX, Math.min(rawX, maxX));
        } else {
            int minX = 0;
            int maxX = screenWidth - elementWidth;
            if (minX > maxX) return screenWidth / 2 - elementWidth / 2;
            return Math.max(minX, Math.min(rawX, maxX));
        }
    }

    public int applySnapX(int rawX, int elementWidth, boolean isCenterBased, int expansionLeft, int expansionRight) {
        return applySnapX(rawX, elementWidth, isCenterBased);
    }

    public int applySnapY(int rawY, int elementHeight, boolean isBottomBased, boolean isCenterBased) {
        if (isBottomBased) {
            int minY = elementHeight;
            int maxY = screenHeight;
            if (minY > maxY) return screenHeight / 2;
            return Math.max(minY, Math.min(rawY, maxY));
        } else if (isCenterBased) {
            int minY = elementHeight / 2;
            int maxY = screenHeight - elementHeight / 2;
            if (minY > maxY) return screenHeight / 2;
            return Math.max(minY, Math.min(rawY, maxY));
        } else {
            int minY = 0;
            int maxY = screenHeight - elementHeight;
            if (minY > maxY) return screenHeight / 2 - elementHeight / 2;
            return Math.max(minY, Math.min(rawY, maxY));
        }
    }

    public int applySnapY(int rawY, int elementHeight, boolean isBottomBased, boolean isCenterBased,
                          int expansionTop, int expansionBottom) {
        return applySnapY(rawY, elementHeight, isBottomBased, isCenterBased);
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
                                    IRenderCommand.HudRenderMetadata metadata) {
        boolean isCenterX = !metadata.isTopLeftBased();
        boolean isBottomY = metadata.isBottomCenterBased();
        boolean isCenterY = metadata.isCenterBased();

        int clampedX = applySnapX(rawX, elementWidth, isCenterX);
        int clampedY = applySnapY(rawY, elementHeight, isBottomY, isCenterY);

        return new SnapResult(clampedX, clampedY,
                              Collections.emptyList(),
                              Collections.emptyList());
    }
}
