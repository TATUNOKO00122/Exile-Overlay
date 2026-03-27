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
 * `orb_fill` シェーダーを使用して円形マスク描画を行います。
 * 
 * 【パフォーマンス】
 * - CPU負荷: ほぼゼロ（GPU処理）
 * - 画質: 滑らかな縁
 */
public class OrbShaderRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbShaderRenderer.class);

    private static ShaderInstance orbFillShader;
    
    private static boolean shaderUnavailableWarned = false;

    public static void setOrbFillShader(ShaderInstance shader) {
        orbFillShader = shader;
    }

    public static ShaderInstance getOrbFillShader() {
        return orbFillShader;
    }

    public static boolean isInitialized() {
        return orbFillShader != null;
    }

    public static boolean isShaderAvailable() {
        return orbFillShader != null;
    }

    private static final float PADDING = 2.0f;
    private static final float OFFSET_X = -1.0f;
    private static final float OFFSET_Y = -1.0f;

    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        if (fillPercent <= 0) {
            return;
        }
        
        if (orbFillShader == null) {
            if (!shaderUnavailableWarned) {
                LOGGER.warn("Orb fill shader not available. Orb rendering will be skipped. " +
                    "Check if orb_fill.json shader is properly registered.");
                shaderUnavailableWarned = true;
            }
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        if (orbFillShader.getUniform("FillPercent") != null) {
            orbFillShader.getUniform("FillPercent").set(fillPercent);
        }

        RenderSystem.setShader(() -> orbFillShader);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float adX = x - PADDING + OFFSET_X;
        float adY = y - PADDING + OFFSET_Y;
        float adSize = size + (PADDING * 2.0f);

        buffer.vertex(matrix, adX, adY + adSize, 0).uv(0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY + adSize, 0).uv(1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY, 0).uv(1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX, adY, 0).uv(0, 0).color(r, g, b, a).endVertex();

        tesselator.end();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

    public static void drawCircularFillAdditive(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        if (fillPercent <= 0) {
            return;
        }
        
        if (orbFillShader == null) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);

        if (orbFillShader.getUniform("FillPercent") != null) {
            orbFillShader.getUniform("FillPercent").set(fillPercent);
        }

        RenderSystem.setShader(() -> orbFillShader);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        RenderSystem.setShaderColor(r, g, b, a);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float adX = x - PADDING + OFFSET_X;
        float adY = y - PADDING + OFFSET_Y;
        float adSize = size + (PADDING * 2.0f);

        buffer.vertex(matrix, adX, adY + adSize, 0).uv(0, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY + adSize, 0).uv(1, 1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX + adSize, adY, 0).uv(1, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, adX, adY, 0).uv(0, 0).color(r, g, b, a).endVertex();

        tesselator.end();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }
}