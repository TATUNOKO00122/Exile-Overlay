package com.example.exile_overlay.util;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * M&Sスキルスロットのキーバインドテキスト取得ユーティリティ
 * クライアント専用
 */
@OnlyIn(Dist.CLIENT)
public class SpellKeyHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpellKeyHelper.class);
    private static final String MNS_SPELL_KEY_PREFIX = "mmorpg.key.spell_";
    private static final long SEARCH_INTERVAL_MS = 5000L;

    private static KeyMapping[] cachedSpellKeys = null;
    private static long lastSearchTime = 0L;

    public static void invalidateCache() {
        cachedSpellKeys = null;
        lastSearchTime = 0L;
    }

    public static String getSpellKeyText(int slot) {
        long now = System.currentTimeMillis();
        if (cachedSpellKeys == null || (now - lastSearchTime > SEARCH_INTERVAL_MS)) {
            findSpellKeys();
            lastSearchTime = now;
        }

        if (cachedSpellKeys != null && slot >= 0 && slot < cachedSpellKeys.length) {
            KeyMapping key;

            boolean swappingEnabled = MethodHandlesUtil.isHotbarSwappingEnabled();
            if (swappingEnabled) {
                // ホットバー切り替え設定がONの場合：アクティブな側の4スロットのみキー表示
                boolean onSecond = MethodHandlesUtil.isOnSecondHotbar();
                boolean isFirstHalf = slot < 4;

                if (onSecond && isFirstHalf) return "";
                if (!onSecond && !isFirstHalf) return "";

                int keySlot = onSecond ? slot - 4 : slot;
                key = (keySlot >= 0 && keySlot < cachedSpellKeys.length) ? cachedSpellKeys[keySlot] : null;
            } else {
                // ホットバー切り替え設定がOFFの場合：全8スロットにそれぞれのバインドキーを表示
                key = cachedSpellKeys[slot];
            }

            if (isUnbound(key)) return "";

            if (key != null) {
                return formatKeyBinding(key);
            }
        }

        return String.valueOf(slot + 1);
    }

    /**
     * 言語設定に依存せず、内部キーコードと修飾キーから短縮キー文字列を生成する
     */
    public static String formatKeyBinding(KeyMapping key) {
        if (key == null || isUnbound(key)) return "";

        InputConstants.Key boundKey = key.getKey();
        String baseKey = null;

        if (boundKey.getType() == InputConstants.Type.MOUSE) {
            baseKey = "M" + (boundKey.getValue() + 1);
        } else if (boundKey.getType() == InputConstants.Type.KEYSYM) {
            baseKey = formatKeySym(boundKey.getValue());
        }

        if (baseKey == null) {
            baseKey = boundKey.getDisplayName().getString().toUpperCase(Locale.ROOT);
        }

        KeyModifier modifier = key.getKeyModifier();
        if (modifier == KeyModifier.SHIFT) {
            return "s+" + baseKey;
        } else if (modifier == KeyModifier.CONTROL) {
            return "c+" + baseKey;
        } else if (modifier == KeyModifier.ALT) {
            return "a+" + baseKey;
        }

        return baseKey;
    }

    private static String formatKeySym(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            return String.valueOf((char) ('A' + (keyCode - GLFW.GLFW_KEY_A)));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            return String.valueOf((char) ('0' + (keyCode - GLFW.GLFW_KEY_0)));
        }
        if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F25) {
            return "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
        }
        if (keyCode >= GLFW.GLFW_KEY_KP_0 && keyCode <= GLFW.GLFW_KEY_KP_9) {
            return "N" + (keyCode - GLFW.GLFW_KEY_KP_0);
        }

        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "Sp";
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> "Ent";
            case GLFW.GLFW_KEY_BACKSPACE -> "Bksp";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            case GLFW.GLFW_KEY_ESCAPE -> "Esc";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
            case GLFW.GLFW_KEY_UP -> "↑";
            case GLFW.GLFW_KEY_DOWN -> "↓";
            case GLFW.GLFW_KEY_LEFT -> "←";
            case GLFW.GLFW_KEY_RIGHT -> "→";
            case GLFW.GLFW_KEY_INSERT -> "Ins";
            case GLFW.GLFW_KEY_DELETE -> "Del";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            case GLFW.GLFW_KEY_HOME -> "Home";
            case GLFW.GLFW_KEY_END -> "End";
            case GLFW.GLFW_KEY_PRINT_SCREEN -> "PSc";
            case GLFW.GLFW_KEY_SCROLL_LOCK -> "SLk";
            case GLFW.GLFW_KEY_NUM_LOCK -> "NLk";
            case GLFW.GLFW_KEY_PAUSE -> "Pau";
            case GLFW.GLFW_KEY_KP_DIVIDE -> "N/";
            case GLFW.GLFW_KEY_KP_MULTIPLY -> "N*";
            case GLFW.GLFW_KEY_KP_SUBTRACT -> "N-";
            case GLFW.GLFW_KEY_KP_ADD -> "N+";
            case GLFW.GLFW_KEY_KP_DECIMAL -> "N.";
            case GLFW.GLFW_KEY_KP_EQUAL -> "N=";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "`";
            case GLFW.GLFW_KEY_MINUS -> "-";
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "[";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "]";
            case GLFW.GLFW_KEY_BACKSLASH -> "\\";
            case GLFW.GLFW_KEY_SEMICOLON -> ";";
            case GLFW.GLFW_KEY_APOSTROPHE -> "'";
            case GLFW.GLFW_KEY_COMMA -> ",";
            case GLFW.GLFW_KEY_PERIOD -> ".";
            case GLFW.GLFW_KEY_SLASH -> "/";
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> "Alt";
            default -> null;
        };
    }

    private static boolean isUnbound(KeyMapping key) {
        if (key == null) return true;
        if (key.isUnbound()) return true;
        var boundKey = key.getKey();
        return boundKey.getType() == InputConstants.Type.KEYSYM && boundKey.getValue() == -1;
    }

    private static void findSpellKeys() {
        try {
            var mc = Minecraft.getInstance();
            if (mc.options == null || mc.options.keyMappings == null) return;
            var allKeys = mc.options.keyMappings;

            KeyMapping[] foundKeys = new KeyMapping[8];

            for (var key : allKeys) {
                String name = key.getName();
                if (name.startsWith(MNS_SPELL_KEY_PREFIX)) {
                    try {
                        int num = Integer.parseInt(name.substring(MNS_SPELL_KEY_PREFIX.length()));
                        if (num >= 1 && num <= 8) {
                            foundKeys[num - 1] = key;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            boolean anyFound = false;
            for (var k : foundKeys) {
                if (k != null) {
                    anyFound = true;
                    break;
                }
            }

            if (anyFound) {
                cachedSpellKeys = foundKeys;
            }
        } catch (Exception e) {
            LOGGER.warn("Error finding spell keys", e);
        }
    }
}
