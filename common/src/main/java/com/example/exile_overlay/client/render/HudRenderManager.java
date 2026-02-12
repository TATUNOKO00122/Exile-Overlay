package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IRenderPipeline;
import com.example.exile_overlay.api.PooledRenderContext;
import com.example.exile_overlay.api.RenderContextPool;
import com.example.exile_overlay.api.UnifiedCache;
import com.example.exile_overlay.client.render.effect.BuffOverlayRenderer;
import com.example.exile_overlay.client.render.orb.OrbRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HUDレンダリングマネージャー
 * 
 * 【責任】
 * - RenderPipelineの管理
 * - フレームカウンターの更新
 * - グローバルなレンダリング設定
 * 
 * 【シングルトンパターン】
 * このクラスはシングルトンとして実装
 * 全HUD要素の統合管理点
 */
public class HudRenderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(HudRenderManager.class);
    private static final HudRenderManager INSTANCE = new HudRenderManager();

    private final IRenderPipeline pipeline;
    private boolean initialized = false;
    
    private final Map<String, IHudRenderer> rendererIndex = new ConcurrentHashMap<>();

    private HudRenderManager() {
        this.pipeline = new RenderPipelineImpl();
    }

    /**
     * インスタンスを取得
     */
    public static HudRenderManager getInstance() {
        return INSTANCE;
    }

    /**
     * 初期化
     * デフォルトのHUD要素を登録
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("HudRenderManager is already initialized");
            return;
        }

        LOGGER.info("Initializing HudRenderManager...");

        // OrbRegistryを初期化
        OrbRegistry.initialize();

        // デフォルトコマンドを登録
        registerDefaultCommands();

        initialized = true;
        LOGGER.info("HudRenderManager initialized with {} commands", pipeline.getCommandCount());
    }

    /**
     * デフォルトのレンダリングコマンドを登録
     */
    private void registerDefaultCommands() {
        // ホットバー（this.registerCommand経由でrendererIndexにも追加）
        this.registerCommand(new HotbarRenderCommand(), 100);

        // バフオーバーレイ
        this.registerCommand(new BuffOverlayRenderer(), 50);
    }

    /**
     * 毎フレーム呼び出されるレンダリングメソッド
     * 
     * @param graphics     GUIグラフィックスコンテキスト
     * @param screenWidth  画面幅
     * @param screenHeight 画面高さ
     */
    public void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!initialized) {
            LOGGER.warn("HudRenderManager is not initialized");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // フレームカウンターを更新
        UnifiedCache.getInstance().incrementFrame();

        // レンダリングコンテキストをプールから取得（オブジェクト再利用）
        PooledRenderContext ctx = RenderContextPool.getInstance().acquire(
                mc,
                mc.player,
                screenWidth,
                screenHeight,
                mc.getFrameTime(),
                mc.level != null ? mc.level.getGameTime() : 0,
                "hud");

        try {
            // パイプラインを実行
            pipeline.render(graphics, ctx);
        } finally {
            // コンテキストをプールに戻す
            RenderContextPool.getInstance().release(ctx);
        }
    }

    /**
     * カスタムコマンドを登録
     * 
     * @param command  登録するコマンド
     * @param priority 優先度
     */
    public void registerCommand(IRenderCommand command, int priority) {
        pipeline.register(command, priority);
        if (command instanceof IHudRenderer renderer) {
            rendererIndex.put(renderer.getConfigKey(), renderer);
        }
        LOGGER.debug("Registered custom command: {} (priority: {})", command.getId(), priority);
    }

    /**
     * カスタムコマンドを登録（デフォルト優先度）
     * 
     * @param command 登録するコマンド
     */
    public void registerCommand(IRenderCommand command) {
        pipeline.register(command);
    }

    /**
     * コマンドを解除
     * 
     * @param commandId コマンドID
     * @return 解除に成功した場合true
     */
    public boolean unregisterCommand(String commandId) {
        IRenderCommand command = pipeline.getCommand(commandId);
        if (command instanceof IHudRenderer renderer) {
            rendererIndex.remove(renderer.getConfigKey());
        }
        return pipeline.unregister(commandId);
    }

    /**
     * パイプラインを取得
     * 高度な操作が必要な場合に使用
     */
    public IRenderPipeline getPipeline() {
        return pipeline;
    }

    /**
     * 登録されているコマンド数を取得
     */
    public int getCommandCount() {
        return pipeline.getCommandCount();
    }

    /**
     * 特定のコマンドが登録されているか
     */
    public boolean hasCommand(String commandId) {
        return pipeline.hasCommand(commandId);
    }

    /**
     * HUDレンダラーを取得
     * IHudRendererを実装しているコマンドを取得する
     *
     * @param configKey 設定キー（getConfigKey()の戻り値）
     * @return IHudRenderer、見つからない場合はnull
     */
    public IHudRenderer getHudRenderer(String configKey) {
        if (configKey == null) {
            return null;
        }
        return rendererIndex.get(configKey);
    }
}
