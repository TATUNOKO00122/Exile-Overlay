package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * MODデータプロバイダーの基底インターフェース。
 * HUDの各スロットに「何を」表示するかを定義する。
 * スロットの「位置」と「意味」を分離し、フレームワークとしての柔軟性を実現。
 *
 * 各メソッドは「HUDのどのスロットに」データを供給するかを決定する。
 * 具体的な「意味」（HPかManaか）は各MODが決定する。未使用スロットはデフォルト値（0または1）を返す。
 */
public interface IModDataProvider {

    /**
     * このプロバイダーが利用可能かどうか
     */
    boolean isAvailable();

    /**
     * プロバイダーの優先度
     */
    default int getPriority() {
        return 100;
    }

    /**
     * プロバイダーの一意なID
     */
    String getId();

    /**
     * 指定されたデータタイプの値を取得する
     * 
     * @param player 対象プレイヤー
     * @param type   データタイプ（HEALTH, MANA, SHIELD等）
     * @return 取得した値
     */
    float getValue(Player player, DataType type);

    /**
     * 指定されたデータタイプの最大値を取得する
     */
    default float getMaxValue(Player player, DataType type) {
        return 1.0f;
    }

    /**
     * 指定された要素の追加属性（Bloodモード等）を確認する
     */
    default boolean getAttribute(Player player, String attributeKey) {
        return false;
    }

    /**
     * 汎用的な整数データ（レベル等）を取得
     */
    default int getInt(Player player, DataType type) {
        return (int) getValue(player, type);
    }
}
