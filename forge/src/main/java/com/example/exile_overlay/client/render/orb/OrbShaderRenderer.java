package com.example.exile_overlay.client.render.orb;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GPUシェーダー方式オーブレンダラー
 * 
 * 【カスタムシェーダーによる円形マスクと波アニメーション】
 * `orb_liquid` シェーダーを使用して、描画とマスクを同時に行います。
 * 
 * 【パフォーマンス】
 * - CPU負荷: ほぼゼロ（GPU処理）
 * - 画質: 最高（滑らかな縁、波アニメーション対応）
 */
public class OrbShaderRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbShaderRenderer.class);

    // シェーダーインスタンス
    private static ShaderInstance orbLiquidShader;

    // アニメーション時間
    private static float animationTime = 0.0f;
    
    // シェーダー未初期化警告の抑制用
    private static boolean shaderUnavailableWarned = false;

    /**
     * シェーダーを登録（プラットフォーム固有のイベントから呼び出される）
     */
    public static void setOrbLiquidShader(ShaderInstance shader) {
        orbLiquidShader = shader;
    }

    /**
     * シェーダーインスタンスを取得
     */
    public static ShaderInstance getOrbLiquidShader() {
        return orbLiquidShader;
    }

    /**
     * レンダラーを初期化
     */
    public static void initializeShader() {
        // プラットフォームごとの登録待ち
    }

    /**
     * レンダラーが初期化されているか
     */
    public static boolean isInitialized() {
        return orbLiquidShader != null;
    }

    /**
     * シェーダーが利用可能か
     */
    public static boolean isShaderAvailable() {
        return orbLiquidShader != null;
    }

    /**
     * アニメーション時間を更新
     */
    public static void updateAnimationTime(float deltaTime) {
        animationTime += deltaTime;
    }

    // 描画調整定数
    private static final float PADDING = 2.0f;
    private static final float OFFSET_X = -1.0f;
    private static final float OFFSET_Y = -1.0f;

    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        if (fillPercent <= 0) {
            return;
        }
        
        if (orbLiquidShader == null) {
            if (!shaderUnavailableWarned) {
                LOGGER.warn("Orb liquid shader not available. Orb rendering will be skipped. " +
                    "Check if orb_liquid.json shader is properly registered.");
                shaderUnavailableWarned = true;
            }
            return;
        }

        // ユニフォーム変数の設定
        if (orbLiquidShader.getUniform("FillLevel") != null) {
            orbLiquidShader.getUniform("FillLevel").set(fillPercent);
        }
        if (orbLiquidShader.getUniform("Time") != null) {
            orbLiquidShader.getUniform("Time").set(animationTime);
        }

        RenderSystem.setShader(() -> orbLiquidShader);

        // 色成分の抽出 (ARGB)
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        // 座標調整
        float adX = x - PADDING + OFFSET_X;
        float adY = y - PADDING + OFFSET_Y;
        float adSize = size + (PADDING * 2.0f);

        buffer.vertex(matrix, adX, adY + adSize, 0).uv(0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY + adSize, 0).uv(1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY, 0).uv(1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX, adY, 0).uv(0, 0).color(r, g, b, a).endVertex();

        tesselator.end();

        // 描画状態をリセット
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
