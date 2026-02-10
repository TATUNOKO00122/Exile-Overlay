package com.example.exile_overlay.api;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 関数型エラーハンドリング用のResult型
 * 
 * 【設計思想】
 * - null安全なエラーハンドリング
 * - 成功/失敗の明示的な表現
 * - 関数型プログラミングスタイルのチェーン操作
 * 
 * 【使用例】
 * Result<Float, HudError> result = fetchPlayerHealth(player);
 * 
 * // パターンマッチング風の処理
 * result.match(
 *     success -> renderHealthBar(success),
 *     error -> showErrorMessage(error)
 * );
 * 
 * // デフォルト値提供
 * float health = result.getOrDefault(20.0f);
 * 
 * // マッピング
 * Result<String, HudError> healthStr = result.map(h -> h + " HP");
 * 
 * @param <T> 成功時の値の型
 * @param <E> 失敗時のエラーの型
 */
public abstract class Result<T, E> {
    
    /**
     * 成功結果
     */
    public static final class Success<T, E> extends Result<T, E> {
        private final T value;
        
        Success(T value) {
            this.value = Objects.requireNonNull(value, "Success value cannot be null");
        }
        
        public T value() {
            return value;
        }
        
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public T getOrDefault(T defaultValue) {
            return value;
        }
        
        @Override
        public T getOrElse(Supplier<T> supplier) {
            return value;
        }
        
        @Override
        public E getError() {
            throw new IllegalStateException("Success result has no error");
        }
        
        @Override
        public <R> Result<R, E> map(Function<T, R> mapper) {
            return Result.success(mapper.apply(value));
        }
        
        @Override
        public <R> Result<R, E> flatMap(Function<T, Result<R, E>> mapper) {
            return mapper.apply(value);
        }
        
        @Override
        public void match(Consumer<T> onSuccess, Consumer<E> onFailure) {
            onSuccess.accept(value);
        }
        
        @Override
        public String toString() {
            return "Success{" + value + '}';
        }
    }
    
    /**
     * 失敗結果
     */
    public static final class Failure<T, E> extends Result<T, E> {
        private final E error;
        
        Failure(E error) {
            this.error = Objects.requireNonNull(error, "Failure error cannot be null");
        }
        
        public E error() {
            return error;
        }
        
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public T getOrDefault(T defaultValue) {
            return defaultValue;
        }
        
        @Override
        public T getOrElse(Supplier<T> supplier) {
            return supplier.get();
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public E getError() {
            return error;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R, E> map(Function<T, R> mapper) {
            return (Result<R, E>) this;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <R> Result<R, E> flatMap(Function<T, Result<R, E>> mapper) {
            return (Result<R, E>) this;
        }
        
        @Override
        public void match(Consumer<T> onSuccess, Consumer<E> onFailure) {
            onFailure.accept(error);
        }
        
        @Override
        public String toString() {
            return "Failure{" + error + '}';
        }
    }
    
    // ========== ファクトリメソッド ==========
    
    /**
     * 成功結果を作成
     */
    public static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * 失敗結果を作成
     */
    public static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * 条件に基づいて成功/失敗を作成
     */
    public static <T, E> Result<T, E> of(boolean condition, Supplier<T> successValue, Supplier<E> errorValue) {
        return condition ? success(successValue.get()) : failure(errorValue.get());
    }
    
    /**
     * サプライヤー実行をResultでラップ
     */
    public static <T, E> Result<T, E> tryCatch(Supplier<T> supplier, Function<Exception, E> errorMapper) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(errorMapper.apply(e));
        }
    }
    
    // ========== 抽象メソッド ==========
    
    /**
     * 成功かどうか
     */
    public abstract boolean isSuccess();
    
    /**
     * 失敗かどうか
     */
    public abstract boolean isFailure();
    
    /**
     * 値を取得（失敗時はデフォルト値）
     */
    public abstract T getOrDefault(T defaultValue);
    
    /**
     * 値を取得（失敗時はサプライヤーから取得）
     */
    public abstract T getOrElse(Supplier<T> supplier);
    
    /**
     * エラーを取得（失敗時のみ）
     * @throws IllegalStateException 成功時に呼び出した場合
     */
    public abstract E getError();
    
    /**
     * 値を変換
     */
    public abstract <R> Result<R, E> map(Function<T, R> mapper);
    
    /**
     * 値を変換（Resultを返す関数用）
     */
    public abstract <R> Result<R, E> flatMap(Function<T, Result<R, E>> mapper);
    
    /**
     * パターンマッチング
     */
    public abstract void match(Consumer<T> onSuccess, Consumer<E> onFailure);
    
    // ========== デフォルトメソッド ==========
    
    /**
     * 成功時のみ処理を実行
     */
    public Result<T, E> ifSuccess(Consumer<T> action) {
        if (isSuccess()) {
            action.accept(((Success<T, E>) this).value());
        }
        return this;
    }
    
    /**
     * 失敗時のみ処理を実行
     */
    public Result<T, E> ifFailure(Consumer<E> action) {
        if (isFailure()) {
            action.accept(((Failure<T, E>) this).error());
        }
        return this;
    }
    
    /**
     * 値が存在するか（成功時のみtrue）
     */
    public boolean isPresent() {
        return isSuccess();
    }
    
    /**
     * 値が空か（失敗時のみtrue）
     */
    public boolean isEmpty() {
        return isFailure();
    }
}
