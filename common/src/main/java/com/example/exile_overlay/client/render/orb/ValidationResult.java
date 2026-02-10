package com.example.exile_overlay.client.render.orb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * OrbConfigのバリデーション結果
 */
public class ValidationResult {
    
    private final List<String> errors;
    private final List<String> warnings;
    
    private ValidationResult(List<String> errors, List<String> warnings) {
        this.errors = Collections.unmodifiableList(errors);
        this.warnings = Collections.unmodifiableList(warnings);
    }
    
    public static ValidationResult success() {
        return new ValidationResult(Collections.emptyList(), Collections.emptyList());
    }
    
    public static ValidationResult failure(List<String> errors) {
        return new ValidationResult(errors, Collections.emptyList());
    }
    
    public static ValidationResult withWarnings(List<String> warnings) {
        return new ValidationResult(Collections.emptyList(), warnings);
    }
    
    public boolean isValid() {
        return errors.isEmpty();
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    @Override
    public String toString() {
        if (isValid() && !hasWarnings()) {
            return "ValidationResult[Valid]";
        }
        StringBuilder sb = new StringBuilder("ValidationResult[");
        if (hasErrors()) {
            sb.append("Errors: ").append(errors.size());
        }
        if (hasWarnings()) {
            if (hasErrors()) sb.append(", ");
            sb.append("Warnings: ").append(warnings.size());
        }
        sb.append("]");
        return sb.toString();
    }
}
