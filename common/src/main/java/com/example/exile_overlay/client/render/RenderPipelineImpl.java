package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.CircuitBreaker;
import com.example.exile_overlay.api.ErrorLogCache;
import com.example.exile_overlay.api.HudError;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IRenderPipeline;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * レンダリングパイプラインの実装クラス（改良版）
 * 
 * 【設計思想】
 * - レイヤー別にコマンドを管理
 * - 優先度順にソートして描画
 * - エラー分類による適切な対応（回復可能/致命的）
 * 
 * 【パフォーマンス最適化】
 * - ArrayList + synchronizedによるゼロアロケーションソート
 * - レイヤーごとのバッチ処理
 * - 無駄なOpenGL状態変更の削減
 * 
 * 【エラーハンドリング】
 * - RuntimeException: 回復可能、次のコマンドを続行
 * - Error: 致命的、再スローしてクラッシュさせる
 * - CircuitBreaker: 連続失敗時の保護
 * 
 * 【スレッド安全性】
 * - synchronizedによるコマンドリスト保護
 * - ConcurrentHashMapによるエラー追跡
 * - TTL付きエラーログによるメモリ保護
 */
public class RenderPipelineImpl implements IRenderPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderPipelineImpl.class);
    
    private static final int CIRCUIT_BREAKER_THRESHOLD = 10;
    private static final long CIRCUIT_BREAKER_TIMEOUT_MS = 5000;

    private final Map<RenderLayer, List<PrioritizedCommand>> commandsByLayer = new EnumMap<>(RenderLayer.class);
    private final Map<String, IRenderCommand> commandsById = new ConcurrentHashMap<>();
    private volatile boolean needsSorting = true;
    private final Object commandLock = new Object();
    private final ErrorLogCache errorLog = new ErrorLogCache();
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();
    private final Set<String> blockedCommandsLogged = ConcurrentHashMap.newKeySet();
    
    public RenderPipelineImpl() {
        for (RenderLayer layer : RenderLayer.values()) {
            commandsByLayer.put(layer, new ArrayList<>());
        }
    }
    
    @Override
    public void register(IRenderCommand command, int priority) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        String id = command.getId();
        
        synchronized (commandLock) {
            if (commandsById.containsKey(id)) {
                LOGGER.warn("Command with id '{}' is already registered, replacing", id);
                unregister(id);
            }
            
            RenderLayer layer = command.getLayer();
            List<PrioritizedCommand> layerCommands = commandsByLayer.get(layer);
            
            PrioritizedCommand pc = new PrioritizedCommand(command, priority);
            layerCommands.add(pc);
            commandsById.put(id, command);
            
            needsSorting = true;
        }
        
        // サーキットブレイカーを初期化
        circuitBreakers.computeIfAbsent(id, k -> 
            new CircuitBreaker(CIRCUIT_BREAKER_THRESHOLD, CIRCUIT_BREAKER_TIMEOUT_MS));
        
        LOGGER.debug("Registered render command: {} (layer: {}, priority: {})", 
            id, command.getLayer(), priority);
    }
    
    @Override
    public boolean unregister(IRenderCommand command) {
        if (command == null) return false;
        return unregister(command.getId());
    }
    
    @Override
    public boolean unregister(String commandId) {
        IRenderCommand command;
        
        synchronized (commandLock) {
            command = commandsById.remove(commandId);
            if (command == null) {
                return false;
            }
            
            RenderLayer layer = command.getLayer();
            List<PrioritizedCommand> layerCommands = commandsByLayer.get(layer);
            
            boolean removed = layerCommands.removeIf(pc -> pc.command.getId().equals(commandId));
            
            if (removed) {
                LOGGER.debug("Unregistered render command: {}", commandId);
            }
        }
        
        // 関連リソースをクリーンアップ
        circuitBreakers.remove(commandId);
        errorLog.clear(commandId);
        blockedCommandsLogged.remove(commandId);
        
        return true;
    }
    
    @Override
    public void clear() {
        synchronized (commandLock) {
            commandsByLayer.values().forEach(List::clear);
            commandsById.clear();
            needsSorting = true;
        }
        
        // 全リソースをクリーンアップ
        circuitBreakers.clear();
        errorLog.clearAll();
        blockedCommandsLogged.clear();
        
        LOGGER.debug("Cleared all render commands");
    }
    
    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        if (needsSorting) {
            sortCommands();
        }
        
        // レイヤー順に描画
        for (RenderLayer layer : RenderLayer.values()) {
            renderLayerInternal(layer, graphics, ctx);
        }
    }
    
    @Override
    public void renderLayer(RenderLayer layer, GuiGraphics graphics, RenderContext ctx) {
        if (needsSorting) {
            sortCommands();
        }
        
        renderLayerInternal(layer, graphics, ctx);
    }
    
    private void renderLayerInternal(RenderLayer layer, GuiGraphics graphics, RenderContext ctx) {
        // 【最適化】synchronizedブロック内でイテレーション準備のみ行い、
        // 実際のレンダリングはロック外で実行
        // コマンド登録/解除はメインスレッドで行われるため、レンダリング中の変更はない
        
        layer.setupRenderState();
        
        try {
            synchronized (commandLock) {
                List<PrioritizedCommand> commands = commandsByLayer.get(layer);
                if (commands.isEmpty()) {
                    return;
                }
                
                // ロック内でイテレーション（レンダリング中はリストが変更されない前提）
                for (PrioritizedCommand pc : commands) {
                    IRenderCommand command = pc.command;
                    String commandId = command.getId();
                    
                    CircuitBreaker cb = circuitBreakers.get(commandId);
                    if (cb != null && !cb.allowExecution()) {
                        if (blockedCommandsLogged.add(commandId)) {
                            LOGGER.warn("Command '{}' is blocked due to repeated failures (circuit breaker OPEN)", 
                                commandId);
                        }
                        continue;
                    }
                    
                    if (!command.isVisible(ctx)) {
                        continue;
                    }
                    
                    try {
                        command.execute(graphics, ctx);
                        
                        if (cb != null) {
                            cb.recordSuccess();
                            blockedCommandsLogged.remove(commandId);
                        }
                        
                    } catch (RuntimeException e) {
                        if (cb != null) {
                            cb.recordFailure();
                        }
                        
                        LOGGER.error("Recoverable error in render command '{}': {}", 
                            commandId, e.getMessage(), e);
                        
                        errorLog.add(commandId, new HudError(
                            commandId,
                            HudError.ErrorType.RECOVERABLE,
                            "Render execution failed: " + e.getMessage(),
                            e
                        ));
                        
                        continue;
                        
                    } catch (Error e) {
                        if (cb != null) {
                            cb.recordFailure();
                        }
                        
                        LOGGER.error("Critical error in render command '{}': {}", 
                            commandId, e.getMessage(), e);
                        
                        errorLog.add(commandId, new HudError(
                            commandId,
                            HudError.ErrorType.CRITICAL,
                            "Critical error: " + e.getMessage(),
                            e
                        ));
                        
                        throw e;
                    }
                }
            }
        } finally {
            layer.cleanupRenderState();
        }
    }
    
    @Override
    public List<IRenderCommand> getCommands() {
        synchronized (commandLock) {
            return List.copyOf(commandsById.values());
        }
    }
    
    @Override
    public List<IRenderCommand> getCommands(RenderLayer layer) {
        synchronized (commandLock) {
            return commandsByLayer.get(layer).stream()
                .map(pc -> pc.command)
                .toList();
        }
    }
    
    @Override
    public int getCommandCount() {
        return commandsById.size();
    }
    
    @Override
    public boolean hasCommand(String commandId) {
        return commandsById.containsKey(commandId);
    }
    
    @Override
    public IRenderCommand getCommand(String commandId) {
        return commandsById.get(commandId);
    }
    
    /**
     * コマンドを優先度順にソート（in-place、アロケーションなし）
     */
    private void sortCommands() {
        synchronized (commandLock) {
            if (!needsSorting) return;
            
            for (List<PrioritizedCommand> commands : commandsByLayer.values()) {
                commands.sort(Comparator.comparingInt(pc -> pc.priority));
            }
            needsSorting = false;
            LOGGER.debug("Sorted {} render commands", commandsById.size());
        }
    }
    
    /**
     * 優先度付きコマンドの内部クラス
     */
    private static class PrioritizedCommand {
        final IRenderCommand command;
        final int priority;
        
        PrioritizedCommand(IRenderCommand command, int priority) {
            this.command = command;
            this.priority = priority;
        }
    }
    
    // ========== エラー追跡メソッド ==========
    
    /**
     * コマンドのエラー履歴を取得
     */
    public List<HudError> getErrorHistory(String commandId) {
        return errorLog.get(commandId);
    }
    
    /**
     * 全エラー履歴をクリア
     */
    public void clearErrorHistory() {
        errorLog.clearAll();
    }
    
    /**
     * コマンドのエラー履歴をクリア
     */
    public void clearErrorHistory(String commandId) {
        errorLog.clear(commandId);
    }
    
    /**
     * エラー統計を取得
     */
    public ErrorStats getErrorStats() {
        // TTL付きキャッシュはサイズが動的に変わるため、
        // 現在のスナップショットを取得
        int totalErrors = 0;
        int criticalErrors = 0;
        int affectedCommands = errorLog.size();
        
        for (String commandId : commandsById.keySet()) {
            List<HudError> errors = errorLog.get(commandId);
            totalErrors += errors.size();
            criticalErrors += errors.stream()
                .filter(e -> e.type() == HudError.ErrorType.CRITICAL)
                .count();
        }
        
        return new ErrorStats(totalErrors, criticalErrors, affectedCommands);
    }
    
    /**
     * サーキットブレイカーの状態を取得
     */
    public CircuitBreaker.State getCircuitBreakerState(String commandId) {
        CircuitBreaker cb = circuitBreakers.get(commandId);
        return cb != null ? cb.getState() : null;
    }
    
    /**
     * サーキットブレイカーを手動でリセット
     */
    public void resetCircuitBreaker(String commandId) {
        CircuitBreaker cb = circuitBreakers.get(commandId);
        if (cb != null) {
            cb.reset();
            blockedCommandsLogged.remove(commandId);
            LOGGER.info("Circuit breaker reset for command: {}", commandId);
        }
    }
    
    /**
     * 全サーキットブレイカーをリセット
     */
    public void resetAllCircuitBreakers() {
        circuitBreakers.values().forEach(CircuitBreaker::reset);
        blockedCommandsLogged.clear();
        LOGGER.info("All circuit breakers reset");
    }
    
    /**
     * リソース解放（シャットダウン時に呼び出し）
     */
    public void shutdown() {
        errorLog.shutdown();
    }
    
    /**
     * エラー統計レコード
     */
    public record ErrorStats(int totalErrors, int criticalErrors, int affectedCommands) {
        @Override
        public String toString() {
            return String.format("ErrorStats{total=%d, critical=%d, commands=%d}",
                totalErrors, criticalErrors, affectedCommands);
        }
    }
}
