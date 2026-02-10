package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.RenderContext;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全なHUDレンダラーのラッパー
 * 
 * 【障害隔離】
 * - 個別のレンダラー失敗を捕捉
 * - 失敗したレンダラーを自動的に無効化
 * - HUD全体のクラッシュを防止
 */
public class SafeHudRenderer implements IHudRenderer {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_FAILURES = 3;  // 無効化までの失敗回数
    private static final long COOLDOWN_MS = 5000;  // 再試行までのクールダウン
    
    private final IHudRenderer delegate;
    private final Map<String, FailureState> failureStates = new ConcurrentHashMap<>();
    
    private static class FailureState {
        int count = 0;
        long lastFailureTime = 0;
        String lastError = "";
        
        boolean isDisabled() {
            return count >= MAX_FAILURES;
        }
        
        boolean canRetry() {
            return System.currentTimeMillis() - lastFailureTime > COOLDOWN_MS;
        }
        
        void recordFailure(String error) {
            count++;
            lastFailureTime = System.currentTimeMillis();
            lastError = error;
        }
        
        void reset() {
            count = 0;
            lastError = "";
        }
    }
    
    public SafeHudRenderer(IHudRenderer delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        String elementId = delegate.getId();
        FailureState state = failureStates.computeIfAbsent(elementId, k -> new FailureState());
        
        // 無効化されているかチェック
        if (state.isDisabled()) {
            if (state.canRetry()) {
                LOGGER.info("Retrying disabled HUD renderer: {}", elementId);
                state.reset();
            } else {
                return;  // まだ無効化中
            }
        }
        
        try {
            delegate.render(graphics, ctx);
            
            // 成功したら失敗カウントをリセット
            if (state.count > 0) {
                state.reset();
            }
            
        } catch (Exception e) {
            state.recordFailure(e.getMessage());
            
            if (state.isDisabled()) {
                LOGGER.error(
                    "HUD renderer '{}' failed {} times, disabling. Last error: {}",
                    elementId, MAX_FAILURES, state.lastError, e
                );
            } else {
                LOGGER.warn(
                    "HUD renderer '{}' failed ({}/{}): {}",
                    elementId, state.count, MAX_FAILURES, e.getMessage()
                );
            }
        }
    }
    
    @Override
    public boolean isVisible(RenderContext ctx) {
        try {
            return delegate.isVisible(ctx);
        } catch (Exception e) {
            LOGGER.debug("Error checking visibility for '{}': {}", delegate.getId(), e.getMessage());
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return delegate.getPriority();
    }
    
    @Override
    public String getId() {
        return delegate.getId();
    }
    
    @Override
    public String getConfigKey() {
        return delegate.getConfigKey();
    }
    
    @Override
    public int getWidth() {
        return delegate.getWidth();
    }
    
    @Override
    public int getHeight() {
        return delegate.getHeight();
    }
    
    @Override
    public boolean isDraggable() {
        return delegate.isDraggable();
    }
    
    /**
     * 全ての無効化状態をリセット
     */
    public void resetAll() {
        failureStates.clear();
        LOGGER.info("Reset all failure states for SafeHudRenderer");
    }
    
    /**
     * 特定のレンダラーの無効化を解除
     */
    public void reset(String elementId) {
        FailureState state = failureStates.get(elementId);
        if (state != null) {
            state.reset();
        }
    }
    
    /**
     * 現在の失敗状態を取得
     */
    public Map<String, Integer> getFailureCounts() {
        Map<String, Integer> counts = new ConcurrentHashMap<>();
        failureStates.forEach((id, state) -> counts.put(id, state.count));
        return counts;
    }
}
