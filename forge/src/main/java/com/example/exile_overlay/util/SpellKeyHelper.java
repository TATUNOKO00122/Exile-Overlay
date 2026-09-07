package com.example.exile_overlay.util;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.settings.KeyModifier;
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
                var boundKey = key.getKey();
                if (boundKey.getType() == InputConstants.Type.MOUSE) {
                    String mouseText = "M" + (boundKey.getValue() + 1);
                    KeyModifier modifier = key.getKeyModifier();
                    if (modifier == KeyModifier.SHIFT) {
                        return "s+" + mouseText;
                    } else if (modifier == KeyModifier.CONTROL) {
                        return "c+" + mouseText;
                    } else if (modifier == KeyModifier.ALT) {
                        return "a+" + mouseText;
                    }
                    return mouseText;
                }
                return key.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
            }
        }

        return String.valueOf(slot + 1);
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
