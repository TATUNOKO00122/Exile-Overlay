package com.example.exile_overlay.client.render.util;

/**
 * HUD描画用カラー計算・変換ユーティリティクラス
 */
public final class ColorUtil {

    private ColorUtil() {}

    /**
     * ARGB各成分 (0-255) から integer カラーコードを生成
     */
    public static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    /**
     * RGB各成分 (0-255) とアルファ値 (0-255) から integer カラーコードを生成
     */
    public static int rgba(int r, int g, int b, int a) {
        return argb(a, r, g, b);
    }

    /**
     * アルファ成分を抽出 (0-255)
     */
    public static int getAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }

    /**
     * レッド成分を抽出 (0-255)
     */
    public static int getRed(int argb) {
        return (argb >> 16) & 0xFF;
    }

    /**
     * グリーン成分を抽出 (0-255)
     */
    public static int getGreen(int argb) {
        return (argb >> 8) & 0xFF;
    }

    /**
     * ブルー成分を抽出 (0-255)
     */
    public static int getBlue(int argb) {
        return argb & 0xFF;
    }

    /**
     * アルファ倍率を適用した新しい ARGB カラーコードを返却
     */
    public static int applyAlphaMultiplier(int argb, float alphaMultiplier) {
        int alpha = (int) (getAlpha(argb) * Math.max(0.0f, Math.min(1.0f, alphaMultiplier)));
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * float 配列 [r, g, b, a] (各0.0~1.0) に変換
     */
    public static float[] toFloatArray(int argb) {
        return new float[]{
                getRed(argb) / 255.0f,
                getGreen(argb) / 255.0f,
                getBlue(argb) / 255.0f,
                getAlpha(argb) / 255.0f
        };
    }
}
