package com.example.exile_overlay.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * MethodHandlesとリフレクションのパフォーマンス比較ベンチマーク
 * 
 * 【検証目的】
 * - MethodHandlesがリフレクションより高速であることを確認
 * - 目標: 10倍以上の高速化
 * 
 * 【実行方法】
 * 開発環境で直接実行: Run as Java Application
 * 
 * 【測定方法】
 * - 1,000,000回のメソッド呼び出しを計測
 * - ウォームアップ実行後、実際の計測を実施
 */
public class MethodHandlesBenchmark {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandlesBenchmark.class);
    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int BENCHMARK_ITERATIONS = 1_000_000;
    
    // テスト対象のメソッド
    private static MethodHandle methodHandle;
    private static Method reflectionMethod;
    
    /**
     * テスト用のシンプルなクラス
     */
    public static class TestTarget {
        private int value = 42;
        
        public int getValue() {
            return value;
        }
    }
    
    public static void main(String[] args) {
        try {
            initialize();
        } catch (Exception e) {
            LOGGER.error("Initialization failed: {}", e.getMessage(), e);
            return;
        }
        
        LOGGER.info("==============================================");
        LOGGER.info("MethodHandles vs Reflection Benchmark");
        LOGGER.info("==============================================");
        LOGGER.info("Warmup iterations: {}", WARMUP_ITERATIONS);
        LOGGER.info("Benchmark iterations: {}", BENCHMARK_ITERATIONS);
        
        TestTarget target = new TestTarget();
        
        // ウォームアップ
        LOGGER.info("\n[Warming up...]");
        try {
            warmup(target);
        } catch (Exception e) {
            LOGGER.error("Warmup failed: {}", e.getMessage(), e);
            return;
        }
        
        // ベンチマーク実行
        LOGGER.info("\n[Running benchmarks...]");
        
        long methodHandleTime;
        try {
            methodHandleTime = benchmarkMethodHandle(target);
        } catch (Throwable e) {
            LOGGER.error("MethodHandle benchmark failed: {}", e.getMessage(), e);
            return;
        }
        
        // 直接呼び出し（ベースライン）
        long directTime = benchmarkDirectCall(target);
        LOGGER.info("Direct call: {} ms (baseline)", directTime);
        
        // MethodHandle呼び出し
        double methodHandleVsDirect = (double) methodHandleTime / directTime;
        LOGGER.info("MethodHandle: {} ms ({:.2f}x slower than direct)", 
            methodHandleTime, methodHandleVsDirect);
        
        // リフレクション呼び出し
        long reflectionTime;
        try {
            reflectionTime = benchmarkReflection(target);
        } catch (Exception e) {
            LOGGER.error("Reflection benchmark failed: {}", e.getMessage(), e);
            return;
        }
        double reflectionVsDirect = (double) reflectionTime / directTime;
        double reflectionVsMethodHandle = (double) reflectionTime / methodHandleTime;
        LOGGER.info("Reflection: {} ms ({:.2f}x slower than direct, {:.2f}x slower than MethodHandle)", 
            reflectionTime, reflectionVsDirect, reflectionVsMethodHandle);
        
        // 結果サマリー
        LOGGER.info("\n==============================================");
        LOGGER.info("Results Summary");
        LOGGER.info("==============================================");
        LOGGER.info("MethodHandles speedup: {:.2f}x faster than Reflection", reflectionVsMethodHandle);
        
        if (reflectionVsMethodHandle >= 5.0) {
            LOGGER.info("✓ PASS: MethodHandles is at least 5x faster than Reflection");
        } else {
            LOGGER.warn("✗ FAIL: MethodHandles is only {:.2f}x faster (expected >= 5x)", 
                reflectionVsMethodHandle);
        }
        
        // StringBuilderベンチマーク
        LOGGER.info("\n[StringBuilder Benchmark]");
        benchmarkStringBuilderReuse();
    }
    
    private static void initialize() throws Exception {
        // MethodHandleの初期化
        methodHandle = MethodHandles.publicLookup()
            .findVirtual(TestTarget.class, "getValue", MethodType.methodType(int.class));
        
        // リフレクションの初期化
        reflectionMethod = TestTarget.class.getMethod("getValue");
    }
    
    private static void warmup(TestTarget target) throws Exception {
        // MethodHandleウォームアップ
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                methodHandle.invoke(target);
            } catch (Throwable e) {
                throw new Exception("MethodHandle warmup failed", e);
            }
        }
        
        // リフレクションウォームアップ
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            reflectionMethod.invoke(target);
        }
        
        // 直接呼び出しウォームアップ
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            target.getValue();
        }
        
        // GCを促進
        System.gc();
        Thread.sleep(100);
    }
    
    private static long benchmarkDirectCall(TestTarget target) {
        long start = System.currentTimeMillis();
        
        int sum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            sum += target.getValue();
        }
        
        long end = System.currentTimeMillis();
        // sumを使用して最適化を防ぐ
        if (sum <= 0) {
            throw new IllegalStateException("Sum should be positive: " + sum);
        }
        return end - start;
    }
    
    private static long benchmarkMethodHandle(TestTarget target) throws Throwable {
        long start = System.currentTimeMillis();
        
        int sum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            sum += (int) methodHandle.invoke(target);
        }
        
        long end = System.currentTimeMillis();
        if (sum <= 0) {
            throw new IllegalStateException("Sum should be positive: " + sum);
        }
        return end - start;
    }
    
    private static long benchmarkReflection(TestTarget target) throws Exception {
        long start = System.currentTimeMillis();
        
        int sum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            sum += (int) reflectionMethod.invoke(target);
        }
        
        long end = System.currentTimeMillis();
        if (sum <= 0) {
            throw new IllegalStateException("Sum should be positive: " + sum);
        }
        return end - start;
    }
    
    private static void benchmarkStringBuilderReuse() {
        final int iterations = 10_000_000;
        
        LOGGER.info("Iterations: {}", iterations);
        
        // 方法1: 毎回新規StringBuilder
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String s = new StringBuilder().append(i).append(" / ").append(i * 2).toString();
        }
        long time1 = System.currentTimeMillis() - start1;
        LOGGER.info("New StringBuilder each time: {} ms", time1);
        
        // 方法2: StringBuilder再利用
        StringBuilder sb = new StringBuilder(32);
        long start2 = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            sb.setLength(0);
            sb.append(i).append(" / ").append(i * 2);
            String s = sb.toString();
        }
        long time2 = System.currentTimeMillis() - start2;
        LOGGER.info("Reused StringBuilder: {} ms", time2);
        
        // 方法3: 文字列連結（最悪）
        long start3 = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            String s = i + " / " + (i * 2);
        }
        long time3 = System.currentTimeMillis() - start3;
        LOGGER.info("String concatenation: {} ms", time3);
        
        double improvement = (double) time1 / time2;
        LOGGER.info("StringBuilder reuse is {:.2f}x faster than creating new instances", improvement);
        
        if (time2 < time1) {
            LOGGER.info("✓ PASS: Reused StringBuilder is faster");
        } else {
            LOGGER.warn("✗ FAIL: Reused StringBuilder should be faster");
        }
    }
}
