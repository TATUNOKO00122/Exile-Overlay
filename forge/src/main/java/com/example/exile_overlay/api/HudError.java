package com.example.exile_overlay.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * HUDレンダリングエラーを表すレコード
 * 
 * 【設計思想】
 * - 不変なエラー情報の保持
 * - エラーの分類による適切な対応
 * - 詳細なコンテキスト情報の提供
 * 
 * 【使用例】
 * HudError error = new HudError("hotbar", ErrorType.RECOVERABLE, "Failed to render texture", exception);
 * 
 * @param elementId エラーが発生したHUD要素のID
 * @param type エラーの種類
 * @param message エラーメッセージ
 * @param cause 原因となった例外（ない場合はnull）
 * @param timestamp エラー発生時刻
 * @param errorId エラーの一意なID（ログ追跡用）
 */
public record HudError(
    String elementId,
    ErrorType type,
    String message,
    Throwable cause,
    Instant timestamp,
    String errorId
) {
    
    /**
     * エラーの種類
     */
    public enum ErrorType {
        /**
         * 回復可能なエラー
         * - 個別のHUD要素を無効化して継続可能
         * - 例: テクスチャ読み込み失敗、無効な設定値
         */
        RECOVERABLE,
        
        /**
         * 致命的なエラー
         * - HUD全体に影響する可能性あり
         * - 例: メモリ不足、OpenGLコンテキスト破損
         */
        CRITICAL,
        
        /**
         * 設定エラー
         * - ユーザー設定の問題
         * - 例: 不正な座標、無効な色値
         */
        CONFIGURATION,
        
        /**
         * データ取得エラー
         * - 外部MODからのデータ取得失敗
         * - 例: Mine and Slashが未ロード、API互換性問題
         */
        DATA_FETCH,
        
        /**
         * タイムアウト
         * - 処理が時間内に完了しなかった
         * - 例: ネットワーク遅延、重い計算
         */
        TIMEOUT
    }
    
    /**
     * 主要なフィールドのみ指定して作成
     */
    public HudError(String elementId, ErrorType type, String message) {
        this(elementId, type, message, null, Instant.now(), UUID.randomUUID().toString());
    }
    
    /**
     * 例外付きで作成
     */
    public HudError(String elementId, ErrorType type, String message, Throwable cause) {
        this(elementId, type, message, cause, Instant.now(), UUID.randomUUID().toString());
    }
    
    /**
     * コンパクトなコンストラクタ
     */
    public HudError {
        Objects.requireNonNull(elementId, "elementId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(message, "message cannot be null");
        Objects.requireNonNull(timestamp, "timestamp cannot be null");
        Objects.requireNonNull(errorId, "errorId cannot be null");
    }
    
    /**
     * エラーが回復可能かどうか
     */
    public boolean isRecoverable() {
        return type == ErrorType.RECOVERABLE || type == ErrorType.CONFIGURATION || type == ErrorType.DATA_FETCH;
    }
    
    /**
     * エラーが致命的かどうか
     */
    public boolean isCritical() {
        return type == ErrorType.CRITICAL;
    }
    
    /**
     * 原因となる例外があるかどうか
     */
    public boolean hasCause() {
        return cause != null;
    }
    
    /**
     * ログ用のフォーマットされたメッセージを取得
     */
    public String toLogMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(errorId).append("] ");
        sb.append(type).append(" error in '").append(elementId).append("': ");
        sb.append(message);
        
        if (cause != null) {
            sb.append(" (caused by: ").append(cause.getClass().getSimpleName());
            if (cause.getMessage() != null) {
                sb.append(": ").append(cause.getMessage());
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * ユーザー向けの簡潔なメッセージを取得
     */
    public String toUserMessage() {
        return switch (type) {
            case RECOVERABLE -> "HUD element '" + elementId + "' is temporarily unavailable";
            case CRITICAL -> "Critical HUD error occurred. Please check the logs.";
            case CONFIGURATION -> "Invalid configuration for '" + elementId + "'";
            case DATA_FETCH -> "Failed to load data for '" + elementId + "'";
            case TIMEOUT -> "HUD element '" + elementId + "' timed out";
        };
    }
    
    @Override
    public String toString() {
        return "HudError{" +
            "elementId='" + elementId + '\'' +
            ", type=" + type +
            ", message='" + message + '\'' +
            ", errorId='" + errorId + '\'' +
            '}';
    }
}
