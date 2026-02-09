package com.example.exile_overlay.api;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * カスタムデータタイプを登録・管理するレジストリ
 * 標準のDataType enumに加えて、動的にデータタイプを追加可能
 */
public class DataTypeRegistry {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final DataTypeRegistry INSTANCE = new DataTypeRegistry();
    
    // カスタムデータタイプのマップ
    private final Map<String, CustomDataType> customTypes = new HashMap<>();
    
    private DataTypeRegistry() {
        // シングルトン
    }
    
    /**
     * レジストリインスタンスを取得
     */
    public static DataTypeRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * カスタムデータタイプを登録
     * 
     * @param key データキー（一意）
     * @param defaultValue デフォルト値
     * @param type データ型
     * @return 登録成功したかどうか
     */
    public boolean register(String key, Object defaultValue, Class<?> type) {
        if (key == null || key.isEmpty()) {
            LOGGER.warn("Attempted to register data type with null or empty key");
            return false;
        }
        
        if (customTypes.containsKey(key)) {
            LOGGER.warn("Data type with key '{}' is already registered", key);
            return false;
        }
        
        // 標準のDataTypeと競合しないかチェック
        if (DataType.fromKey(key) != null) {
            LOGGER.warn("Data type key '{}' conflicts with standard DataType", key);
            return false;
        }
        
        customTypes.put(key, new CustomDataType(key, defaultValue, type));
        LOGGER.debug("Registered custom data type: {} (type: {})", key, type.getSimpleName());
        return true;
    }
    
    /**
     * カスタムデータタイプを登録（推論型使用）
     */
    public <T> boolean register(String key, T defaultValue) {
        if (defaultValue == null) {
            LOGGER.warn("Cannot register data type with null default value: {}", key);
            return false;
        }
        return register(key, defaultValue, defaultValue.getClass());
    }
    
    /**
     * Float型データタイプを登録
     */
    public boolean registerFloat(String key, float defaultValue) {
        return register(key, defaultValue, Float.class);
    }
    
    /**
     * Int型データタイプを登録
     */
    public boolean registerInt(String key, int defaultValue) {
        return register(key, defaultValue, Integer.class);
    }
    
    /**
     * Boolean型データタイプを登録
     */
    public boolean registerBoolean(String key, boolean defaultValue) {
        return register(key, defaultValue, Boolean.class);
    }
    
    /**
     * String型データタイプを登録
     */
    public boolean registerString(String key, String defaultValue) {
        return register(key, defaultValue, String.class);
    }
    
    /**
     * カスタムデータタイプを取得
     */
    public CustomDataType get(String key) {
        return customTypes.get(key);
    }
    
    /**
     * キーが登録されているかチェック
     */
    public boolean has(String key) {
        return customTypes.containsKey(key) || DataType.fromKey(key) != null;
    }
    
    /**
     * デフォルト値を取得
     */
    public Object getDefaultValue(String key) {
        // まず標準のDataTypeをチェック
        DataType standardType = DataType.fromKey(key);
        if (standardType != null) {
            return standardType.getDefaultValue();
        }
        
        // カスタムデータタイプをチェック
        CustomDataType customType = customTypes.get(key);
        if (customType != null) {
            return customType.getDefaultValue();
        }
        
        return null;
    }
    
    /**
     * データ型を取得
     */
    public Class<?> getType(String key) {
        // まず標準のDataTypeをチェック
        DataType standardType = DataType.fromKey(key);
        if (standardType != null) {
            return standardType.getType();
        }
        
        // カスタムデータタイプをチェック
        CustomDataType customType = customTypes.get(key);
        if (customType != null) {
            return customType.getType();
        }
        
        return Object.class;
    }
    
    /**
     * 登録済みのカスタムデータタイプキーを取得
     */
    public Set<String> getCustomKeys() {
        return Collections.unmodifiableSet(customTypes.keySet());
    }
    
    /**
     * 全てのデータタイプキーを取得（標準 + カスタム）
     */
    public Set<String> getAllKeys() {
        Set<String> keys = new java.util.HashSet<>();
        
        // 標準のDataTypeキーを追加
        for (DataType type : DataType.values()) {
            keys.add(type.getKey());
        }
        
        // カスタムデータタイプキーを追加
        keys.addAll(customTypes.keySet());
        
        return Collections.unmodifiableSet(keys);
    }
    
    /**
     * カスタムデータタイプの登録を解除
     */
    public boolean unregister(String key) {
        if (customTypes.remove(key) != null) {
            LOGGER.debug("Unregistered custom data type: {}", key);
            return true;
        }
        return false;
    }
    
    /**
     * 全てのカスタムデータタイプをクリア
     */
    public void clear() {
        customTypes.clear();
        LOGGER.debug("Cleared all custom data types");
    }
    
    /**
     * カスタムデータタイプの数を取得
     */
    public int getCustomCount() {
        return customTypes.size();
    }
    
    /**
     * カスタムデータタイプクラス
     */
    public static class CustomDataType {
        private final String key;
        private final Object defaultValue;
        private final Class<?> type;
        
        CustomDataType(String key, Object defaultValue, Class<?> type) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.type = type;
        }
        
        public String getKey() {
            return key;
        }
        
        public Object getDefaultValue() {
            return defaultValue;
        }
        
        public Class<?> getType() {
            return type;
        }
        
        @SuppressWarnings("unchecked")
        public <T> T getDefaultValueTyped() {
            return (T) defaultValue;
        }
    }
}
