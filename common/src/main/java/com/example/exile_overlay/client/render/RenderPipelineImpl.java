package com.example.exile_overlay.client.render;

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
import java.util.stream.Collectors;

/**
 * レンダリングパイプラインの実装クラス
 * 
 * 【設計思想】
 * - レイヤー別にコマンドを管理
 * - 優先度順にソートして描画
 * - エラー分類による適切な対応（回復可能/致命的）
 * 
 * 【パフォーマンス最適化】
 * - コマンドリストのキャッシュ
 * - レイヤーごとのバッチ処理
 * - 無駄なOpenGL状態変更の削減
 * 
 * 【エラーハンドリング】
 * - RuntimeException: 回復可能、次のコマンドを続行
 * - Error: 致命的、再スローしてクラッシュさせる
 */
public class RenderPipelineImpl implements IRenderPipeline {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderPipelineImpl.class);

    // レイヤー → 優先度付きコマンドリスト
    private final Map<RenderLayer, List<PrioritizedCommand>> commandsByLayer = new EnumMap<>(RenderLayer.class);

    // ID → コマンドのマップ（高速検索用）
    private final Map<String, IRenderCommand> commandsById = new HashMap<>();

    // ソート済みフラグ
    private boolean needsSorting = true;

    // エラー追跡
    private final Map<String, List<HudError>> errorLog = new ConcurrentHashMap<>();
    private static final int MAX_ERROR_HISTORY = 10;
    
    public RenderPipelineImpl() {
        // 各レイヤーを初期化
        for (RenderLayer layer : RenderLayer.values()) {
            commandsByLayer.put(layer, new ArrayList<>());
        }
    }
    
    @Override
    public void register(IRenderCommand command, int priority) {
        Objects.requireNonNull(command, "Command cannot be null");
        
        String id = command.getId();
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
        LOGGER.debug("Registered render command: {} (layer: {}, priority: {})", 
            id, layer, priority);
    }
    
    @Override
    public boolean unregister(IRenderCommand command) {
        if (command == null) return false;
        return unregister(command.getId());
    }
    
    @Override
    public boolean unregister(String commandId) {
        IRenderCommand command = commandsById.remove(commandId);
        if (command == null) {
            return false;
        }
        
        RenderLayer layer = command.getLayer();
        List<PrioritizedCommand> layerCommands = commandsByLayer.get(layer);
        
        boolean removed = layerCommands.removeIf(pc -> pc.command.getId().equals(commandId));
        
        if (removed) {
            LOGGER.debug("Unregistered render command: {}", commandId);
        }
        
        return removed;
    }
    
    @Override
    public void clear() {
        commandsByLayer.values().forEach(List::clear);
        commandsById.clear();
        needsSorting = true;
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
        List<PrioritizedCommand> commands = commandsByLayer.get(layer);
        if (commands.isEmpty()) {
            return;
        }
        
        // レイヤーの描画設定を適用
        layer.setupRenderState();
        
        try {
            for (PrioritizedCommand pc : commands) {
                IRenderCommand command = pc.command;
                String commandId = command.getId();
                
                // 可視性チェック
                if (!command.isVisible(ctx)) {
                    continue;
                }
                
                try {
                    command.execute(graphics, ctx);
                } catch (RuntimeException e) {
                    // 回復可能なエラー - ログに記録して続行
                    LOGGER.error("Recoverable error in render command '{}': {}", 
                        commandId, e.getMessage(), e);
                    
                    // エラー追跡（将来の失敗監視用）
                    trackError(commandId, new HudError(
                        commandId,
                        HudError.ErrorType.RECOVERABLE,
                        "Render execution failed: " + e.getMessage(),
                        e
                    ));
                    
                    // 次のコマンドを続行
                    continue;
                } catch (Error e) {
                    // 致命的エラー - 再スロー
                    LOGGER.error("Critical error in render command '{}': {}", 
                        commandId, e.getMessage(), e);
                    
                    trackError(commandId, new HudError(
                        commandId,
                        HudError.ErrorType.CRITICAL,
                        "Critical error: " + e.getMessage(),
                        e
                    ));
                    
                    throw e;
                }
            }
        } finally {
            // 描画設定を復元
            layer.cleanupRenderState();
        }
    }
    
    @Override
    public List<IRenderCommand> getCommands() {
        return commandsById.values().stream()
            .collect(Collectors.toUnmodifiableList());
    }
    
    @Override
    public List<IRenderCommand> getCommands(RenderLayer layer) {
        return commandsByLayer.get(layer).stream()
            .map(pc -> pc.command)
            .collect(Collectors.toUnmodifiableList());
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
     * コマンドを優先度順にソート
     */
    private void sortCommands() {
        for (List<PrioritizedCommand> commands : commandsByLayer.values()) {
            commands.sort(Comparator.comparingInt(pc -> pc.priority));
        }
        needsSorting = false;
        LOGGER.debug("Sorted {} render commands", commandsById.size());
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
     * エラーを追跡
     */
    private void trackError(String commandId, HudError error) {
        errorLog.computeIfAbsent(commandId, k -> new ArrayList<>()).add(error);
        
        // 古いエラーを削除
        List<HudError> errors = errorLog.get(commandId);
        if (errors.size() > MAX_ERROR_HISTORY) {
            errors.remove(0);
        }
    }
    
    /**
     * コマンドのエラー履歴を取得
     */
    public List<HudError> getErrorHistory(String commandId) {
        return List.copyOf(errorLog.getOrDefault(commandId, List.of()));
    }
    
    /**
     * 全エラー履歴をクリア
     */
    public void clearErrorHistory() {
        errorLog.clear();
    }
    
    /**
     * コマンドのエラー履歴をクリア
     */
    public void clearErrorHistory(String commandId) {
        errorLog.remove(commandId);
    }
    
    /**
     * エラー統計を取得
     */
    public ErrorStats getErrorStats() {
        int totalErrors = errorLog.values().stream().mapToInt(List::size).sum();
        int criticalErrors = errorLog.values().stream()
            .flatMap(List::stream)
            .filter(e -> e.type() == HudError.ErrorType.CRITICAL)
            .mapToInt(e -> 1)
            .sum();
        
        return new ErrorStats(totalErrors, criticalErrors, errorLog.size());
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
