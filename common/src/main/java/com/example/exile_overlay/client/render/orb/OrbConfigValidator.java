package com.example.exile_overlay.client.render.orb;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * OrbConfigのバリデータ
 * 
 * 【堅牢性向上】
 * - 設定値の境界チェック
 * - エラーと警告の区別
 * - 詳細なエラーメッセージ
 */
public class OrbConfigValidator {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 定数定義
    private static final int MIN_SIZE = 16;
    private static final int MAX_SIZE = 256;
    private static final int MIN_POSITION = 0;
    private static final int MAX_POSITION = 4096;  // 4K対応
    
    private static final int MIN_COLOR = 0x00000000;
    private static final int MAX_COLOR = 0xFFFFFFFF;
    
    /**
     * 設定をバリデート
     * 
     * @param config 検証する設定
     * @return バリデーション結果
     */
    public ValidationResult validate(OrbConfig config) {
        if (config == null) {
            return ValidationResult.failure(List.of("Config is null"));
        }
        
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // IDの検証
        validateId(config.getId(), errors, warnings);
        
        // サイズの検証
        validateSize(config.getSize(), errors, warnings);
        
        // 位置の検証
        validatePosition(config.getX(), config.getY(), errors, warnings);
        
        // 色の検証
        validateColor(config.getColor(), "color", errors);
        validateColor(config.getOverlayColor(), "overlayColor", errors);
        
        // データプロバイダーの検証
        validateDataProvider(config, errors);
        
        // オーバーレイ設定の検証
        validateOverlay(config, errors, warnings);
        
        // 結果を構築
        if (!errors.isEmpty()) {
            LOGGER.error("OrbConfig validation failed for '{}': {}", 
                config.getId(), errors);
            return ValidationResult.failure(errors);
        }
        
        if (!warnings.isEmpty()) {
            LOGGER.warn("OrbConfig validation warnings for '{}': {}", 
                config.getId(), warnings);
            return ValidationResult.withWarnings(warnings);
        }
        
        return ValidationResult.success();
    }
    
    private void validateId(String id, List<String> errors, List<String> warnings) {
        if (id == null || id.trim().isEmpty()) {
            errors.add("ID cannot be null or empty");
        } else if (id.length() > 64) {
            errors.add("ID too long (max 64 chars): " + id.length());
        } else if (!id.matches("^[a-zA-Z0-9_]+$")) {
            warnings.add("ID contains non-alphanumeric characters: " + id);
        }
    }
    
    private void validateSize(int size, List<String> errors, List<String> warnings) {
        if (size < MIN_SIZE) {
            errors.add(String.format("Size too small: %d (min: %d)", size, MIN_SIZE));
        } else if (size > MAX_SIZE) {
            errors.add(String.format("Size too large: %d (max: %d)", size, MAX_SIZE));
        } else if (size % 2 != 0) {
            warnings.add("Size is odd, center calculation may be off: " + size);
        }
    }
    
    private void validatePosition(int x, int y, List<String> errors, List<String> warnings) {
        if (x < MIN_POSITION) {
            errors.add(String.format("X position negative: %d", x));
        } else if (x > MAX_POSITION) {
            warnings.add(String.format("X position very large: %d", x));
        }
        
        if (y < MIN_POSITION) {
            errors.add(String.format("Y position negative: %d", y));
        } else if (y > MAX_POSITION) {
            warnings.add(String.format("Y position very large: %d", y));
        }
    }
    
    private void validateColor(int color, String fieldName, List<String> errors) {
        // int型の色値は0x00000000～0xFFFFFFFF（-1）の範囲を自然にカバーする
        // 有効な色値の範囲は全てのint値を含むため、追加チェックは不要
        // このメソッドは将来の拡張用に保持
    }
    
    private void validateDataProvider(OrbConfig config, List<String> errors) {
        if (config.getDataProvider() == null) {
            errors.add("DataProvider is required");
        }
    }
    
    private void validateOverlay(OrbConfig config, List<String> errors, 
                                  List<String> warnings) {
        if (config.isOverlay()) {
            String targetId = config.getOverlayFor();
            if (targetId == null || targetId.trim().isEmpty()) {
                errors.add("Overlay target ID is empty");
            }
            
            if (config.getOverlayProvider() == null) {
                errors.add("Overlay provider is required when overlay is enabled");
            }
            
            if (config.getId().equals(targetId)) {
                errors.add("Overlay cannot target itself");
            }
        }
        
        if (config.hasOverlayColor() && !config.isOverlay()) {
            warnings.add("Overlay color set but orb is not configured as overlay");
        }
    }
}
