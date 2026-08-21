package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 設定画面の各タブ定義インターフェース。
 */
public interface IConfigTab {

    /**
     * @return タブボタンに表示される名称
     */
    Component getTitle();

    /**
     * タブに含まれる設定エントリのリストを構築して返す
     *
     * @param screen 親の設定画面インスタンス
     * @return 設定エントリのリスト
     */
    List<ConfigEntry> buildEntries(ConfigScreen screen);
}
