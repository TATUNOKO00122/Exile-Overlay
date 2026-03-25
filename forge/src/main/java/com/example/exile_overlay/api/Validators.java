package com.example.exile_overlay.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 境界値検証ユーティリティクラス
 * 
 * 【設計思想】
 * - 検証ロジックの一元管理
 * - 複数の検証結果を一括で収集
 * - Result型との統合
 * 
 * 【使用例】
 * ValidationResult result = Validators.validateAll(
 *     () -> Validators.validateRange("size", config.getSize(), 1, 200),
 *     () -> Validators.validateNotNaN("percent", config.getPercent()),
 *     () -> Validators.validateColor("color", config.getColor())
 * );
 * 
 * if (!result.isValid()) {
 *     LOGGER.error("Validation failed: {}", result.getErrors());
 * }
 */
public final class Validators {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Validators.class);
    
    // 定数
    public static final int MAX_ORB_SIZE = 200;
    public static final int MIN_ORB_SIZE = 10;
    public static final int MAX_COORDINATE = 10000;
    public static final int MIN_COORDINATE = -10000;
    
    private Validators() {
        // ユーティリティクラス
    }
    
    /**
     * 複数の検証を一括実行
     * 
     * @param validators 検証関数の配列
     * @return 統合された検証結果
     */
    @SafeVarargs
    public static ValidationResult validateAll(ValidationFunction... validators) {
        List<String> errors = new ArrayList<>();
        
        for (ValidationFunction validator : validators) {
            try {
                ValidationResult result = validator.validate();
                if (!result.isValid()) {
                    errors.addAll(result.getErrors());
                }
            } catch (Exception e) {
                errors.add("Validation threw exception: " + e.getMessage());
                LOGGER.warn("Validation exception", e);
            }
        }
        
        return errors.isEmpty() 
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }
    
    /**
     * 範囲検証（int）
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の値
     * @param min 最小値（含む）
     * @param max 最大値（含む）
     * @return 検証結果
     */
    public static ValidationResult validateRange(String fieldName, int value, int min, int max) {
        if (value < min) {
            return ValidationResult.failure(
                String.format("%s must be >= %d, got %d", fieldName, min, value)
            );
        }
        if (value > max) {
            return ValidationResult.failure(
                String.format("%s must be <= %d, got %d", fieldName, max, value)
            );
        }
        return ValidationResult.success();
    }
    
    /**
     * 範囲検証（float）
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の値
     * @param min 最小値（含む）
     * @param max 最大値（含む）
     * @return 検証結果
     */
    public static ValidationResult validateRange(String fieldName, float value, float min, float max) {
        if (value < min) {
            return ValidationResult.failure(
                String.format("%s must be >= %.2f, got %.2f", fieldName, min, value)
            );
        }
        if (value > max) {
            return ValidationResult.failure(
                String.format("%s must be <= %.2f, got %.2f", fieldName, max, value)
            );
        }
        return ValidationResult.success();
    }
    
    /**
     * NaN/Infinity検証（float）
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の値
     * @return 検証結果
     */
    public static ValidationResult validateNotNaN(String fieldName, float value) {
        if (Float.isNaN(value)) {
            return ValidationResult.failure(fieldName + " is NaN");
        }
        if (Float.isInfinite(value)) {
            return ValidationResult.failure(fieldName + " is infinite");
        }
        return ValidationResult.success();
    }
    
    /**
     * NaN/Infinity検証（double）
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の値
     * @return 検証結果
     */
    public static ValidationResult validateNotNaN(String fieldName, double value) {
        if (Double.isNaN(value)) {
            return ValidationResult.failure(fieldName + " is NaN");
        }
        if (Double.isInfinite(value)) {
            return ValidationResult.failure(fieldName + " is infinite");
        }
        return ValidationResult.success();
    }
    
    /**
     * 色値検証（ARGB）
     * 
     * @param fieldName フィールド名
     * @param color ARGB色値
     * @return 検証結果
     */
    public static ValidationResult validateColor(String fieldName, int color) {
        // ARGB形式なので、すべてのビットが有効
        // ただし、極端な値（完全透明など）は警告
        int alpha = (color >> 24) & 0xFF;
        
        if (alpha < 0 || alpha > 255) {
            return ValidationResult.failure(
                String.format("%s has invalid alpha channel: %d", fieldName, alpha)
            );
        }
        
        if (alpha == 0) {
            // 警告レベル（完全透明）
            LOGGER.debug("{} is fully transparent (alpha=0)", fieldName);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 座標検証
     * 
     * @param fieldNameX X座標フィールド名
     * @param fieldNameY Y座標フィールド名
     * @param x X座標
     * @param y Y座標
     * @return 検証結果
     */
    public static ValidationResult validateCoordinates(String fieldNameX, String fieldNameY, int x, int y) {
        return validateAll(
            () -> validateRange(fieldNameX, x, MIN_COORDINATE, MAX_COORDINATE),
            () -> validateRange(fieldNameY, y, MIN_COORDINATE, MAX_COORDINATE)
        );
    }
    
    /**
     * 画面内座標検証
     * 
     * @param fieldNameX X座標フィールド名
     * @param fieldNameY Y座標フィールド名
     * @param x X座標
     * @param y Y座標
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @param margin 許容マージン（画面外も許容するピクセル数）
     * @return 検証結果
     */
    public static ValidationResult validateScreenCoordinates(
            String fieldNameX, String fieldNameY,
            int x, int y,
            int screenWidth, int screenHeight,
            int margin) {
        return validateAll(
            () -> validateRange(fieldNameX, x, -margin, screenWidth + margin),
            () -> validateRange(fieldNameY, y, -margin, screenHeight + margin)
        );
    }
    
    /**
     * スケール値検証
     * 
     * @param fieldName フィールド名
     * @param scale スケール値
     * @return 検証結果
     */
    public static ValidationResult validateScale(String fieldName, float scale) {
        ValidationResult nanCheck = validateNotNaN(fieldName, scale);
        if (!nanCheck.isValid()) {
            return nanCheck;
        }
        
        if (scale <= 0) {
            return ValidationResult.failure(
                String.format("%s must be positive, got %.2f", fieldName, scale)
            );
        }
        
        if (scale > 10.0f) {
            LOGGER.warn("{} has unusually large value: {}", fieldName, scale);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 割合値検証（0.0〜1.0）
     * 
     * @param fieldName フィールド名
     * @param ratio 割合値
     * @return 検証結果
     */
    public static ValidationResult validateRatio(String fieldName, float ratio) {
        ValidationResult nanCheck = validateNotNaN(fieldName, ratio);
        if (!nanCheck.isValid()) {
            return nanCheck;
        }
        
        return validateRange(fieldName, ratio, 0.0f, 1.0f);
    }
    
    /**
     * 非null検証
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の値
     * @return 検証結果
     */
    public static ValidationResult validateNotNull(String fieldName, Object value) {
        if (value == null) {
            return ValidationResult.failure(fieldName + " cannot be null");
        }
        return ValidationResult.success();
    }
    
    /**
     * 文字列非空検証
     * 
     * @param fieldName フィールド名
     * @param value 検証対象の文字列
     * @return 検証結果
     */
    public static ValidationResult validateNotEmpty(String fieldName, String value) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.failure(fieldName + " cannot be empty");
        }
        return ValidationResult.success();
    }
    
    /**
     * 検証関数のインターフェース
     */
    @FunctionalInterface
    public interface ValidationFunction {
        ValidationResult validate();
    }
    
    /**
     * 検証結果クラス
     */
    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        private ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors != null ? List.copyOf(errors) : List.of();
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }
        
        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
        
        /**
         * 別の結果と結合
         */
        public ValidationResult combine(ValidationResult other) {
            if (this.valid && other.valid) {
                return success();
            }
            
            List<String> combinedErrors = new ArrayList<>();
            combinedErrors.addAll(this.errors);
            combinedErrors.addAll(other.errors);
            return failure(combinedErrors);
        }
        
        /**
         * Result型に変換
         */
        public <T> Result<T, HudError> toResult(String elementId, HudError.ErrorType errorType) {
            if (valid) {
                return null; // 成功時は使わない
            }
            return Result.failure(new HudError(
                elementId,
                errorType,
                getErrorMessage()
            ));
        }
        
        @Override
        public String toString() {
            return valid ? "ValidationResult{valid}" : "ValidationResult{errors=" + errors + "}";
        }
    }
}
