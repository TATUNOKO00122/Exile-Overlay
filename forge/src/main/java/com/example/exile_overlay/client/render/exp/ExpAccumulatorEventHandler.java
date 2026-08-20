package com.example.exile_overlay.client.render.exp;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 累積獲得EXPポップアップ用のクライアントイベントハンドラー
 *
 * - M&S のアクションバー経験値メッセージを受信・パースして ExpAccumulatorManager へ通知
 * - M&S のデフォルトアクションバーEXP表示の抑制・キャンセル判定
 * - クライアント側ティックでの表示タイマー減衰更新
 * - ログイン・ログアウト時の状態初期化
 */
@OnlyIn(Dist.CLIENT)
public class ExpAccumulatorEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ExpAccumulatorEventHandler");

    // M&S 公式の経験値メッセージ翻訳キー (com.robertx22.mine_and_slash.uncommon.localization.Gui.EXP_GAIN_PERCENT)
    private static final String MNS_EXP_GAIN_KEY = "mmorpg.gui.exp_gain_percent";

    // M&SのEXPメッセージ判定用正規表現パターン (例: "+120 Exp (45.2%)", "+50 Salvaging Exp")
    private static final Pattern MNS_EXP_PATTERN =
            Pattern.compile("^\\+(\\d+)\\s*(.*?)\\s*(?:Exp|EXP|exp)(?:\\s*\\(([0-9.]+)\\s*%\\))?$");

    /**
     * クライアントティック毎のタイマー減衰処理
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        try {
            ExpAccumulatorManager.getInstance().tick();
        } catch (Exception e) {
            LOGGER.error("Error updating ExpAccumulatorManager tick", e);
        }
    }



    /**
     * メッセージが M&S の経験値メッセージかどうかを判定
     */
    public static boolean isMnsExpMessage(Component component) {
        return parseMnsExpMessage(component) != null;
    }

    /**
     * M&S の経験値メッセージをパースして結果を返す
     */
    public static ParsedExpMessage parseMnsExpMessage(Component component) {
        if (component == null) {
            return null;
        }

        // 1. TranslatableContents からの直接抽出 (M&S 公式フォーマット)
        if (component.getContents() instanceof TranslatableContents translatable) {
            String key = translatable.getKey().toLowerCase(java.util.Locale.ROOT);
            if (key.contains("exp_gain_percent") || key.contains("mine_and_slash.gui.exp") || key.contains("prof_exp_gain") || MNS_EXP_GAIN_KEY.equals(key)) {
                Object[] args = translatable.getArgs();
                if (args != null && args.length >= 2) {
                    try {
                        int gained = parseGainedArg(args[0]);
                        Component profComp = parseProfArg(args[1]);
                        float percentage = args.length >= 3 ? parsePercentageArg(args[2]) : 0.0f;
                        return new ParsedExpMessage(gained, profComp, percentage);
                    } catch (Exception e) {
                        LOGGER.debug("Failed to parse TranslatableContents args: {}", e.getMessage());
                    }
                }
            }
        }

        // 2. 文字列正規表現によるパース (フォールバック)
        String text = component.getString().trim();
        Matcher matcher = MNS_EXP_PATTERN.matcher(text);
        if (matcher.matches()) {
            try {
                int gained = Integer.parseInt(matcher.group(1));
                String profText = matcher.group(2).trim();
                Component profComp = profText.isEmpty() ? Component.empty() : Component.literal(profText);
                String pctStr = matcher.group(3);
                float percentage = pctStr != null ? Float.parseFloat(pctStr) : 0.0f;
                return new ParsedExpMessage(gained, profComp, percentage);
            } catch (Exception e) {
                LOGGER.debug("Failed to parse regex matched string: {}", e.getMessage());
            }
        }

        return null;
    }

    private static int parseGainedArg(Object arg) {
        if (arg instanceof Number number) {
            return number.intValue();
        } else if (arg instanceof Component comp) {
            return Integer.parseInt(comp.getString().trim());
        } else if (arg != null) {
            return Integer.parseInt(arg.toString().trim());
        }
        return 0;
    }

    private static Component parseProfArg(Object arg) {
        if (arg instanceof Component comp) {
            return comp;
        } else if (arg != null) {
            String s = arg.toString().trim();
            return s.isEmpty() ? Component.empty() : Component.literal(s);
        }
        return Component.empty();
    }

    private static float parsePercentageArg(Object arg) {
        if (arg instanceof Number number) {
            return number.floatValue();
        }
        String s = (arg instanceof Component comp) ? comp.getString() : (arg != null ? arg.toString() : "0");
        s = s.replace("%", "").trim();
        return Float.parseFloat(s);
    }

    /**
     * ログイン時の初期化
     */
    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        try {
            ExpAccumulatorManager.getInstance().reset();
            LOGGER.debug("Player logged in, reset ExpAccumulatorManager cache");
        } catch (Exception e) {
            LOGGER.error("Error resetting on login", e);
        }
    }

    /**
     * ワールド切断・ログアウト時のリセット処理
     */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        try {
            ExpAccumulatorManager.getInstance().reset();
            LOGGER.debug("Player logged out, reset ExpAccumulatorManager cache");
        } catch (Exception e) {
            LOGGER.error("Error resetting on logout", e);
        }
    }

    /**
     * パース結果を格納するレコード
     */
    public record ParsedExpMessage(int gained, Component profComponent, float percentage) {
    }
}
