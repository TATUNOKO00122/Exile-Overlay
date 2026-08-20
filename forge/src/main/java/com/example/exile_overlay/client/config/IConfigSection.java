package com.example.exile_overlay.client.config;

/**
 * 設定セクションの共通インターフェース。
 * ExileOverlayConfigManager経由で一括管理できる。
 */
public interface IConfigSection {

    /**
     * @return セクションの一意識別子
     */
    String getSectionId();

    /**
     * ファイルから設定を読み込む
     */
    void load();

    /**
     * 設定をファイルに保存する
     */
    void save();
}
