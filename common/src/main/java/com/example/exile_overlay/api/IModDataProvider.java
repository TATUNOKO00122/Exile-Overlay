package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * MODデータプロバイダーの基底インターフェース
 * 
 * このインターフェースは、HUD上の各スロットに「何を」表示するかを定義します。
 * スロットの「位置」と「意味」を分離することで、フレームワークとしての柔軟性を実現しています。
 * 
 * 【スロット配置ガイド】
 * 
 * ┌─────────────────────────────────────────────────────────────┐
 * │ HUDレイアウト（画面座標系） │
 * │ │
 * │ ┌─────┐ │
 * │ │ORB_3│ ← 左上サブスロット │
 * │ └─────┘ （デフォルト: Stamina/Energy） │
 * │ │
 * │ ┌─────────┐ │
 * │ │ORB_2 │ ← 右下メイン │
 * │ ┌─────────┐ │(Blood/ │ （デフォルト: │
 * │ │ORB_1 │ │ Mana) │ Mana / Blood） │
 * │ │(HP) │ └─────────┘ │
 * │ │+Overlay│ │
 * │ └─────────┘ │
 * │ ↑ 左下メイン（デフォルト: HP + Magic Shield overlay） │
 * │ │
 * └─────────────────────────────────────────────────────────────┘
 * 
 * 【実装ガイド】
 * 
 * 1. 各メソッドは「HUDのどのスロットに」データを供給するかを決定
 * - getOrb1Current() → ORB_1に表示される現在値
 * - getOrb2Max() → ORB_2に表示される最大値
 * 
 * 2. 具体的な「意味」（HPかManaか）は各MODが決定
 * - Mine and Slash: ORB_1=HP, ORB_2=Mana, ORB_1_OVERLAY=Magic Shield
 * - 他のMOD: ORB_1=Health, ORB_2=Blood Magic, など自由に設定可能
 * 
 * 3. 未使用のスロットはデフォルト値（0または1）を返す
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
