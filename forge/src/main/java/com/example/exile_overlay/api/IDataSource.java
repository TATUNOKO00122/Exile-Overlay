package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * データソースの基底インターフェース。
 * レンダラーはこのインターフェースを通じてのみデータにアクセスし、具体的な取得実装は実装クラスに委譲する。
 */
public interface IDataSource {
    
    /**
     * データのデフォルト値（取得失敗時に使用）
     */
    float DEFAULT_VALUE = 0.0f;
    int DEFAULT_INT = 0;
    boolean DEFAULT_BOOL = false;
    String DEFAULT_STRING = "";
    
    /**
     * 現在値を取得
     * 
     * @param player 対象プレイヤー
     * @return 取得した値、失敗時はDEFAULT_VALUE
     */
    float getValue(Player player);
    
    /**
     * 最大値を取得
     * 
     * @param player 対象プレイヤー
     * @return 取得した最大値、失敗時はDEFAULT_VALUE
     */
    default float getMaxValue(Player player) {
        return DEFAULT_VALUE;
    }
    
    /**
     * データが利用可能かどうか
     * 
     * @return 利用可能な場合true
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * データソースの一意なID
     */
    String getId();
    
    /**
     * 空のデータソース（フォールバック用）
     */
    IDataSource EMPTY = new IDataSource() {
        @Override
        public float getValue(Player player) {
            return DEFAULT_VALUE;
        }
        
        @Override
        public String getId() {
            return "empty";
        }
    };
}
