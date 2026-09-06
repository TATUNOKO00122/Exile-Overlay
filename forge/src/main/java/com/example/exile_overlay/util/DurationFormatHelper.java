package com.example.exile_overlay.util;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;

/**
 * 残り時間のフォーマットユーティリティ。
 * 設定に応じて「0:00」形式（m:ss / h:mm:ss）または従来の単位表記（30s / 1m / 1h）で表示する。
 */
public final class DurationFormatHelper {

    private DurationFormatHelper() {}

    /**
     * 残り秒数を「0:00」表記（m:ss または h:mm:ss）にフォーマットする。
     */
    public static String formatDurationColon(int seconds) {
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
     * 残り秒数を従来の単位表記（h / m / s）にフォーマットする。
     */
    public static String formatDurationLegacy(int seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        if (seconds >= 3600) {
            return (seconds / 3600) + "h";
        }
        if (seconds >= 60) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }

    /**
     * 残りティック数を「0:00」表記にフォーマットする。
     */
    public static String formatTicksColon(int ticks) {
        if (ticks <= 0) {
            return "0:00";
        }
        return formatDurationColon((ticks + 19) / 20);
    }

    /**
     * 残りティック数を従来の単位表記にフォーマットする。
     */
    public static String formatTicksLegacy(int ticks) {
        if (ticks <= 0) {
            return "0s";
        }
        return formatDurationLegacy((ticks + 19) / 20);
    }

    /**
     * 残り秒数を現在の設定に応じた表記にフォーマットする。
     */
    public static String formatDuration(int seconds) {
        if (EquipmentDisplayConfig.getInstance().isBuffDurationColonFormat()) {
            return formatDurationColon(seconds);
        }
        return formatDurationLegacy(seconds);
    }

    /**
     * 残りティック数を現在の設定に応じた表記にフォーマットする（20 ticks = 1秒、切り上げ）。
     */
    public static String formatTicks(int ticks) {
        if (EquipmentDisplayConfig.getInstance().isBuffDurationColonFormat()) {
            return formatTicksColon(ticks);
        }
        return formatTicksLegacy(ticks);
    }
}
