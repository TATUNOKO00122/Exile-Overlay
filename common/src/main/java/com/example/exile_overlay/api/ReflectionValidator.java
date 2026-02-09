package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * リフレクション操作の検証と安全性を確保するクラス
 * 
 * 【セキュリティ機能】
 * - ホワイトリストによるパッケージ制限
 * - 初期化時検証（早期エラー検出）
 * - MethodHandleキャッシュによるパフォーマンス最適化
 * 
 * 【使用方法】
 * ReflectionValidator.validateAndCache(className, methodName, paramTypes)
 *     .ifPresent(handle -> handle.invoke(player));
 */
public class ReflectionValidator {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 許可されたパッケージのホワイトリスト
    private static final Set<String> ALLOWED_PACKAGES = ConcurrentHashMap.newKeySet();
    
    // 検証済みMethodHandleキャッシュ
    private final ConcurrentHashMap<String, ValidationResult> validationCache = new ConcurrentHashMap<>();
    
    static {
        // デフォルトで許可するパッケージ
        ALLOWED_PACKAGES.add("com.robertx22.mine_and_slash");
        ALLOWED_PACKAGES.add("net.minecraft");
        ALLOWED_PACKAGES.add("com.example.exile_overlay");
    }
    
    /**
     * ホワイトリストにパッケージを追加
     * 
     * @param packageName 許可するパッケージ名
     */
    public static void addAllowedPackage(String packageName) {
        ALLOWED_PACKAGES.add(packageName);
        LOGGER.info("Added package to reflection whitelist: {}", packageName);
    }
    
    /**
     * ホワイトリストを取得（コピー）
     */
    public static Set<String> getAllowedPackages() {
        return Set.copyOf(ALLOWED_PACKAGES);
    }
    
    /**
     * クラス名がホワイトリストに含まれるかチェック
     */
    public boolean isClassAllowed(String className) {
        return ALLOWED_PACKAGES.stream()
                .anyMatch(pkg -> className.startsWith(pkg));
    }
    
    /**
     * リフレクションメソッドを検証してキャッシュ
     * 
     * @param className クラス名
     * @param methodName メソッド名
     * @param returnType 戻り値の型
     * @param paramTypes パラメータ型
     * @return 検証結果
     */
    public ValidationResult validateAndCache(
            String className, 
            String methodName, 
            Class<?> returnType,
            Class<?>... paramTypes) {
        
        String cacheKey = buildCacheKey(className, methodName);
        
        // キャッシュをチェック
        ValidationResult cached = validationCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // ホワイトリストチェック
        if (!isClassAllowed(className)) {
            ValidationResult result = ValidationResult.failed(
                className, methodName, 
                "Class not in whitelist. Allowed packages: " + ALLOWED_PACKAGES
            );
            validationCache.put(cacheKey, result);
            LOGGER.warn("Reflection blocked for non-whitelisted class: {}", className);
            return result;
        }
        
        // リフレクション検証
        try {
            Class<?> clazz = Class.forName(className);
            MethodHandle handle = MethodHandles.publicLookup()
                    .findStatic(clazz, methodName, MethodType.methodType(returnType, paramTypes));
            
            ValidationResult result = ValidationResult.success(className, methodName, handle);
            validationCache.put(cacheKey, result);
            
            LOGGER.debug("Reflection validated and cached: {}.{}", className, methodName);
            return result;
            
        } catch (ClassNotFoundException e) {
            ValidationResult result = ValidationResult.failed(
                className, methodName, "Class not found: " + e.getMessage()
            );
            validationCache.put(cacheKey, result);
            LOGGER.error("Reflection validation failed - Class not found: {}.{}", className, methodName);
            return result;
            
        } catch (NoSuchMethodException e) {
            ValidationResult result = ValidationResult.failed(
                className, methodName, "Method not found: " + e.getMessage()
            );
            validationCache.put(cacheKey, result);
            LOGGER.error("Reflection validation failed - Method not found: {}.{}", className, methodName);
            return result;
            
        } catch (IllegalAccessException e) {
            ValidationResult result = ValidationResult.failed(
                className, methodName, "Method not accessible: " + e.getMessage()
            );
            validationCache.put(cacheKey, result);
            LOGGER.error("Reflection validation failed - Illegal access: {}.{}", className, methodName);
            return result;
        }
    }
    
    /**
     * Float値を返すstaticメソッドを検証（簡易版）
     */
    public ValidationResult validateFloatMethod(String className, String methodName) {
        return validateAndCache(className, methodName, float.class, Player.class);
    }
    
    /**
     * キャッシュをクリア
     */
    public void clearCache() {
        validationCache.clear();
        LOGGER.debug("Reflection validation cache cleared");
    }
    
    /**
     * キャッシュサイズを取得
     */
    public int getCacheSize() {
        return validationCache.size();
    }
    
    private String buildCacheKey(String className, String methodName) {
        return className + "#" + methodName;
    }
    
    /**
     * 検証結果を表すクラス
     */
    public static class ValidationResult {
        private final String className;
        private final String methodName;
        private final MethodHandle methodHandle;
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(String className, String methodName, 
                                MethodHandle methodHandle, boolean valid, 
                                String errorMessage) {
            this.className = className;
            this.methodName = methodName;
            this.methodHandle = methodHandle;
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        static ValidationResult success(String className, String methodName, MethodHandle handle) {
            return new ValidationResult(className, methodName, handle, true, null);
        }
        
        static ValidationResult failed(String className, String methodName, String error) {
            return new ValidationResult(className, methodName, null, false, error);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public MethodHandle getMethodHandle() {
            return methodHandle;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getClassName() {
            return className;
        }
        
        public String getMethodName() {
            return methodName;
        }
        
        @Override
        public String toString() {
            if (valid) {
                return String.format("ValidationResult[VALID: %s.%s]", className, methodName);
            } else {
                return String.format("ValidationResult[FAILED: %s.%s - %s]", 
                    className, methodName, errorMessage);
            }
        }
    }
}
