package com.example.exile_overlay.client.config.position;

import java.util.Objects;

/**
 * HUD要素の位置設定
 * 
 * アンカー（画面の基準点）+ オフセット（相対座標）方式で位置を管理
 * 画面解像度が変わっても相対的な位置を維持する
 */
public class HudPosition {
    
    private final Anchor anchor;
    private final int offsetX;
    private final int offsetY;
    
    /**
     * デフォルトコンストラクタ（中央配置）
     */
    public HudPosition() {
        this(Anchor.BOTTOM_CENTER, 0, 0);
    }
    
    /**
     * 完全指定コンストラクタ
     * 
     * @param anchor アンカー位置
     * @param offsetX アンカーからのXオフセット
     * @param offsetY アンカーからのYオフセット
     */
    public HudPosition(Anchor anchor, int offsetX, int offsetY) {
        this.anchor = Objects.requireNonNull(anchor, "anchor cannot be null");
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }
    
    /**
     * 絶対座標からHudPositionを作成
     * 
     * @param x 画面上のX座標
     * @param y 画面上のY座標
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @return 計算されたHudPosition
     */
    public static HudPosition fromAbsolute(int x, int y, int screenWidth, int screenHeight) {
        Anchor anchor = Anchor.estimateFromPosition(x, y, screenWidth, screenHeight);
        int baseX = getBaseXForAnchor(anchor, screenWidth);
        int baseY = getBaseYForAnchor(anchor, screenHeight);
        return new HudPosition(anchor, x - baseX, y - baseY);
    }
    
    /**
     * 絶対座標を計算
     * 
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @return [x, y] 配列
     */
    public int[] resolve(int screenWidth, int screenHeight) {
        return anchor.resolve(screenWidth, screenHeight, offsetX, offsetY);
    }
    
    public int getResolvedX(int screenWidth, int screenHeight) {
        return resolve(screenWidth, screenHeight)[0];
    }
    
    public int getResolvedY(int screenWidth, int screenHeight) {
        return resolve(screenWidth, screenHeight)[1];
    }
    
    public Anchor getAnchor() {
        return anchor;
    }
    
    public int getOffsetX() {
        return offsetX;
    }
    
    public int getOffsetY() {
        return offsetY;
    }
    
    /**
     * 新しいオフセットで更新したHudPositionを返す（イミュータブル）
     */
    public HudPosition withOffset(int newOffsetX, int newOffsetY) {
        return new HudPosition(anchor, newOffsetX, newOffsetY);
    }
    
    /**
     * 新しいアンカーで更新したHudPositionを返す（イミュータブル）
     */
    public HudPosition withAnchor(Anchor newAnchor) {
        return new HudPosition(newAnchor, offsetX, offsetY);
    }
    
    private static int getBaseXForAnchor(Anchor anchor, int screenWidth) {
        return switch (anchor) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> screenWidth / 2;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> screenWidth;
        };
    }
    
    private static int getBaseYForAnchor(Anchor anchor, int screenHeight) {
        return switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case LEFT, CENTER, RIGHT -> screenHeight / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight;
        };
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HudPosition that = (HudPosition) o;
        return offsetX == that.offsetX && 
               offsetY == that.offsetY && 
               anchor == that.anchor;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(anchor, offsetX, offsetY);
    }
    
    @Override
    public String toString() {
        return String.format("HudPosition{anchor=%s, offset=(%d, %d)}", anchor, offsetX, offsetY);
    }
}
