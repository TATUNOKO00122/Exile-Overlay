package com.example.exile_overlay.client.config.position;

/**
 * HUD位置のアンカー位置を定義
 */
public enum Anchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    LEFT,
    CENTER,
    RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;
    
    /**
     * アンカーからの相対座標を解決
     * 
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @param offsetX X方向オフセット
     * @param offsetY Y方向オフセット
     * @return [x, y] 配列
     */
    public int[] resolve(int screenWidth, int screenHeight, int offsetX, int offsetY) {
        int baseX = getBaseX(screenWidth);
        int baseY = getBaseY(screenHeight);
        return new int[]{baseX + offsetX, baseY + offsetY};
    }
    
    private int getBaseX(int screenWidth) {
        return switch (this) {
            case TOP_LEFT, LEFT, BOTTOM_LEFT -> 0;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> screenWidth / 2;
            case TOP_RIGHT, RIGHT, BOTTOM_RIGHT -> screenWidth;
        };
    }
    
    private int getBaseY(int screenHeight) {
        return switch (this) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 0;
            case LEFT, CENTER, RIGHT -> screenHeight / 2;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> screenHeight;
        };
    }
    
    /**
     * 座標から最も近いアンカーを推定
     */
    public static Anchor estimateFromPosition(int x, int y, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        boolean left = x < centerX - screenWidth / 4;
        boolean right = x > centerX + screenWidth / 4;
        boolean top = y < centerY - screenHeight / 4;
        boolean bottom = y > centerY + screenHeight / 4;
        
        if (top && left) return TOP_LEFT;
        if (top && !left && !right) return TOP_CENTER;
        if (top && right) return TOP_RIGHT;
        if (!top && !bottom && left) return LEFT;
        if (!top && !bottom && !left && !right) return CENTER;
        if (!top && !bottom && right) return RIGHT;
        if (bottom && left) return BOTTOM_LEFT;
        if (bottom && !left && !right) return BOTTOM_CENTER;
        return BOTTOM_RIGHT;
    }
}
