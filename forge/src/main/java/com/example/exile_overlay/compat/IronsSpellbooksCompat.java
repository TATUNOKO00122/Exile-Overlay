package com.example.exile_overlay.compat;

import io.redspace.ironsspellbooks.player.ClientMagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * Iron's Spells 'n Spellbooks との互換ブリッジクラス。
 * irons_spellbooks MODが有効な場合にのみ呼び出すこと（MOD未導入環境でのクラスロードエラー防止）。
 * ヌルポインタ例外防止のためプレイヤーインスタンスや属性の有無を検証する。
 */
public class IronsSpellbooksCompat {

    /**
     * 現在のマナ値を取得する
     * 
     * @param player プレイヤーインスタンス
     * @return 現在のマナ値、エラー時は0.0f
     */
    public static float getCurrentMana(Player player) {
        if (player == null) {
            return 0.0f;
        }
        try {
            // ClientMagicData からクライアントのマナを取得
            return (float) ClientMagicData.getPlayerMana();
        } catch (Throwable t) {
            return 0.0f;
        }
    }

    /**
     * 最大マナ値を取得する
     * 
     * @param player プレイヤーインスタンス
     * @return 最大マナ値、エラー時は0.0f
     */
    public static float getMaxMana(Player player) {
        if (player == null) {
            return 0.0f;
        }
        try {
            // AttributeRegistry から MAX_MANA 属性インスタンスを取得してプレイヤーからその値を取り出す
            var attrib = AttributeRegistry.MAX_MANA.get();
            if (attrib != null) {
                return (float) player.getAttributeValue(attrib);
            }
            return 0.0f;
        } catch (Throwable t) {
            return 0.0f;
        }
    }
}
