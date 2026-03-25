package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * MODデータをキー・バリュー形式で保持するクラス
 * 
 * 【スロットベース設計】
 * このクラスはHUDスロットに表示されるデータを管理します。
 * DataTypeは「どのスロットに」データを表示するかを定義します。
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ
 * - ORB_2: 画面右下のメインスロット
 * - ORB_3: 画面左上のサブスロット
 */
public class ModData {

    private final Map<String, Object> data = new HashMap<>();
    private final long timestamp;
    private final String providerId;

    public ModData(String providerId) {
        this.providerId = providerId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 値を設定
     */
    public ModData set(String key, Object value) {
        if (value != null) {
            data.put(key, value);
        }
        return this;
    }

    /**
     * DataTypeを使用して値を設定
     */
    public ModData set(DataType type, Object value) {
        return set(type.getKey(), value);
    }

    /**
     * 値を取得
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * DataTypeを使用して値を取得
     */
    public Object get(DataType type) {
        Object value = data.get(type.getKey());
        if (value == null) {
            return type.getDefaultValue();
        }
        return value;
    }

    /**
     * 型指定で値を取得
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * DataTypeとデフォルト値を使用して値を取得
     */
    @SuppressWarnings("unchecked")
    public <T> T get(DataType type, T defaultValue) {
        Object value = data.get(type.getKey());
        if (value != null && type.getType().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }

    /**
     * Float値を取得
     */
    public float getFloat(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0.0f;
    }

    /**
     * Float値を取得（デフォルト値指定）
     */
    public float getFloat(String key, float defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return defaultValue;
    }

    /**
     * Int値を取得
     */
    public int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * Int値を取得（デフォルト値指定）
     */
    public int getInt(String key, int defaultValue) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    /**
     * Boolean値を取得
     */
    public boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * Boolean値を取得（デフォルト値指定）
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }

    /**
     * String値を取得
     */
    public String getString(String key) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return "";
    }

    /**
     * キーが存在するかチェック
     */
    public boolean has(String key) {
        return data.containsKey(key);
    }

    /**
     * DataTypeが存在するかチェック
     */
    public boolean has(DataType type) {
        return data.containsKey(type.getKey());
    }

    /**
     * 全データを取得（読み取り専用）
     */
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * 全キーを取得
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(data.keySet());
    }

    /**
     * データが空かチェック
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * データサイズを取得
     */
    public int size() {
        return data.size();
    }

    /**
     * データをクリア
     */
    public void clear() {
        data.clear();
    }

    /**
     * タイムスタンプを取得
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * データが古いかチェック（指定ミリ秒以上経過）
     */
    public boolean isStale(long maxAgeMs) {
        return (System.currentTimeMillis() - timestamp) > maxAgeMs;
    }

    /**
     * プロバイダーIDを取得
     */
    public String getProviderId() {
        return providerId;
    }

    /**
     * データをマージ
     */
    public ModData merge(ModData other) {
        if (other != null) {
            this.data.putAll(other.data);
        }
        return this;
    }

    /**
     * 全てのDataType値を一括取得
     * 
     * @param provider データプロバイダー
     * @param player   対象プレイヤー
     * @return プレイヤーデータを含むModData
     */
    public static ModData fromPlayer(IModDataProvider provider, Player player) {
        ModData modData = new ModData(provider.getId());

        // 全てのDataType値を汎用的に取得
        for (DataType type : DataType.values()) {
            if (type.getType() == Float.class || type.getType() == Integer.class) {
                modData.set(type, provider.getValue(player, type));
                // 最大値も取得（DataTypeに最大値の概念がある場合）
                // 現状の設計では DataType 自体に MAX 系が定義されているので個別設定
                float max = provider.getMaxValue(player, type);
                // 最大値がデフォルト1.0f出ない場合のみ保存、あるいは特定キーで保存する設計も検討可能だが
                // 現状は type 自体が MAX を含んでいるため、ループ内で完結する
            } else if (type.getType() == Boolean.class) {
                modData.set(type, provider.getAttribute(player, type.getKey()));
            }
        }

        return modData;
    }

    @Override
    public String toString() {
        return "ModData{" +
                "provider='" + providerId + '\'' +
                ", entries=" + data.size() +
                ", timestamp=" + timestamp +
                '}';
    }
}
