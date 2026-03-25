package com.example.exile_overlay.api;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 操作結果を表現するコンテナ型
 * 
 * 【堅牢性向上】
 * - 成功/失敗を型安全に表現
 * - 例外を伝播させず、失敗情報を含めて返す
 * 
 * @param <T> 成功時の値の型
 */
public class DataResult<T> {
    
    private final T value;
    private final String error;
    private final boolean success;
    private final Throwable cause;
    
    private DataResult(T value, String error, boolean success, Throwable cause) {
        this.value = value;
        this.error = error;
        this.success = success;
        this.cause = cause;
    }
    
    /**
     * 成功結果を作成
     */
    public static <T> DataResult<T> success(T value) {
        return new DataResult<>(value, null, true, null);
    }
    
    /**
     * 失敗結果を作成
     */
    public static <T> DataResult<T> failure(String error) {
        return new DataResult<>(null, error, false, null);
    }
    
    /**
     * 例外付き失敗結果を作成
     */
    public static <T> DataResult<T> failure(String error, Throwable cause) {
        return new DataResult<>(null, error, false, cause);
    }
    
    /**
     * デフォルト値付き失敗結果を作成
     */
    public static <T> DataResult<T> failure(String error, T defaultValue) {
        return new DataResult<>(defaultValue, error, false, null);
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isFailure() {
        return !success;
    }
    
    /**
     * 値を取得（失敗時はnull）
     */
    public T getValue() {
        return value;
    }
    
    /**
     * エラーメッセージを取得（成功時はnull）
     */
    public String getError() {
        return error;
    }
    
    /**
     * 原因例外を取得
     */
    public Throwable getCause() {
        return cause;
    }
    
    /**
     * 値を取得、失敗時はデフォルト値を返す
     */
    public T orElse(T defaultValue) {
        return success ? value : defaultValue;
    }
    
    /**
     * 値を取得、失敗時は例外を投げる
     */
    public T orElseThrow() {
        if (success) {
            return value;
        }
        throw new IllegalStateException(error, cause);
    }
    
    /**
     * 値を変換
     */
    public <R> DataResult<R> map(Function<T, R> mapper) {
        if (success) {
            return success(mapper.apply(value));
        }
        return failure(error, cause);
    }
    
    /**
     * 成功時のみ処理を実行
     */
    public void ifSuccess(Consumer<T> action) {
        if (success) {
            action.accept(value);
        }
    }
    
    /**
     * 失敗時のみ処理を実行
     */
    public void ifFailure(Consumer<String> action) {
        if (!success) {
            action.accept(error);
        }
    }
    
    @Override
    public String toString() {
        return success ? "Success[" + value + "]" : "Failure[" + error + "]";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataResult)) return false;
        DataResult<?> that = (DataResult<?>) o;
        return success == that.success && 
               Objects.equals(value, that.value) && 
               Objects.equals(error, that.error);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value, error, success);
    }
}
