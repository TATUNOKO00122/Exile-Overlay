package com.example.exile_overlay.util;

/**
 * 残り時間のフォーマットユーティリティ。
 * ステータス効果等の残り時間を常に「0:00」形式（m:ss / h:mm:ss）で統一表示する。
 */
public final class DurationFormatHelper {

    private DurationFormatHelper() {}

    /**
     * 残り秒数を「0:00」表記（m:ss または h:mm:ss）にフォーマットする。
     *
     * @param seconds 残り秒数
     * @return フォーマット済み文字列 (例: "0:00", "0:05", "1:23", "1:02:03")
     */
    public static String formatDuration(int seconds) {
        if (seconds <= 0) {
            return "0:00";
        }
        int m = seconds / 60;
        int s = seconds % 60;
        if (m >= 60) {
            int h = m / 60;
            m %= 60;
            return h + (m < 10 ? ":0" : ":") + m + (s < 10 ? ":0" : ":") + s;
        }
        return m + (s < 10 ? ":0" : ":") + s;
    }

    /**
     * 残りティック数を「0:00」表記にフォーマットする（20 ticks = 1秒、切り上げ）。
     *
     * @param ticks 残りティック数
     * @return フォーマット済み文字列
     */
    public static String formatTicks(int ticks) {
        if (ticks <= 0) {
            return "0:00";
        }
        return formatDuration((ticks + 19) / 20);
    }
}
