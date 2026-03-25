package com.example.exile_overlay.api;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * レンダリングパイプラインのインターフェース
 * 
 * 【設計思想】
 * - コマンドの登録・優先度付け・実行を管理
 * - レイヤーごとに最適化された描画順序
 * - 柔軟なコマンド追加・削除
 * 
 * 【使用例】
 * IRenderPipeline pipeline = new RenderPipelineImpl();
 * 
 * // コマンド登録
 * pipeline.register(new HotbarRenderCommand(), 100);
 * pipeline.register(new OrbRenderCommand(), 200);
 * 
 * // 描画実行（毎フレーム）
 * pipeline.render(graphics, ctx);
 */
public interface IRenderPipeline {
    
    /**
     * レンダリングコマンドを登録
     * 
     * @param command 登録するコマンド
     * @param priority 優先度（値が大きいほど手前）
     */
    void register(IRenderCommand command, int priority);
    
    /**
     * レンダリングコマンドを登録（デフォルト優先度）
     * 
     * @param command 登録するコマンド
     */
    default void register(IRenderCommand command) {
        register(command, command.getPriority());
    }
    
    /**
     * レンダリングコマンドの登録を解除
     * 
     * @param command 解除するコマンド
     * @return 解除に成功した場合true
     */
    boolean unregister(IRenderCommand command);
    
    /**
     * IDでレンダリングコマンドの登録を解除
     * 
     * @param commandId コマンドID
     * @return 解除に成功した場合true
     */
    boolean unregister(String commandId);
    
    /**
     * 全てのコマンドをクリア
     */
    void clear();
    
    /**
     * パイプラインを実行
     * 登録されたコマンドをレイヤー・優先度順に実行
     * 
     * @param graphics GUIグラフィックスコンテキスト
     * @param ctx レンダリングコンテキスト
     */
    void render(GuiGraphics graphics, RenderContext ctx);
    
    /**
     * 特定レイヤーのコマンドのみ実行
     * 
     * @param layer 実行するレイヤー
     * @param graphics GUIグラフィックスコンテキスト
     * @param ctx レンダリングコンテキスト
     */
    void renderLayer(RenderLayer layer, GuiGraphics graphics, RenderContext ctx);
    
    /**
     * 登録されているコマンド一覧を取得
     * 
     * @return コマンドリスト（変更不可）
     */
    List<IRenderCommand> getCommands();
    
    /**
     * 特定レイヤーのコマンド一覧を取得
     * 
     * @param layer 対象レイヤー
     * @return コマンドリスト（変更不可）
     */
    List<IRenderCommand> getCommands(RenderLayer layer);
    
    /**
     * 登録されているコマンド数を取得
     */
    int getCommandCount();
    
    /**
     * 特定IDのコマンドが登録されているか
     * 
     * @param commandId コマンドID
     * @return 登録されている場合true
     */
    boolean hasCommand(String commandId);
    
    /**
     * 特定IDのコマンドを取得
     * 
     * @param commandId コマンドID
     * @return コマンド、存在しない場合はnull
     */
    IRenderCommand getCommand(String commandId);
}
