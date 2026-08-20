package com.example.exile_overlay.client.render.orb;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * GPUシェーダー方式オーブレンダラー。
 * `orb_fill` シェーダーで円形マスク描画。CPU負荷はほぼゼロ（GPU処理）、縁は滑らか。
 */
public class OrbShaderRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrbShaderRenderer.class);

    private static ShaderInstance orbFillShader;
    private static Uniform fillPercentUniform;
    private static Uniform halfModeUniform;
    private static Uniform overlapWidthUniform;
    private static Uniform useTextureUniform;
    private static Uniform enableNoiseUniform;
    private static Uniform enableLiquidShadowUniform;
    private static Uniform enableOrbInnerShadowUniform;
    
    private static boolean shaderUnavailableWarned = false;

    private static final Supplier<ShaderInstance> ORB_FILL_SHADER_SUPPLIER = () -> orbFillShader;

    public static void setOrbFillShader(ShaderInstance shader) {
        orbFillShader = shader;
        if (shader != null) {
            fillPercentUniform = shader.getUniform("FillPercent");
            halfModeUniform = shader.getUniform("HalfMode");
            overlapWidthUniform = shader.getUniform("OverlapWidth");
            useTextureUniform = shader.getUniform("UseTexture");
            enableNoiseUniform = shader.getUniform("EnableNoise");
            enableLiquidShadowUniform = shader.getUniform("EnableLiquidShadow");
            enableOrbInnerShadowUniform = shader.getUniform("EnableOrbInnerShadow");
        } else {
            fillPercentUniform = null;
            halfModeUniform = null;
            overlapWidthUniform = null;
            useTextureUniform = null;
            enableNoiseUniform = null;
            enableLiquidShadowUniform = null;
            enableOrbInnerShadowUniform = null;
        }
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
    private static final float OFFSET_X = 0.0f;
    private static final float OFFSET_Y = 0.0f;

    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        drawCircularFill(graphics, x, y, size, fillPercent, color, 0, 0.0f);
    }

    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color, int halfMode) {
        drawCircularFill(graphics, x, y, size, fillPercent, color, halfMode, 0.0f);
    }

    public static void drawCircularFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color, int halfMode, float overlapWidth) {
        if (fillPercent <= 0) {
            return;
        }
        
        if (orbFillShader == null) {
            if (!shaderUnavailableWarned) {
                LOGGER.warn("Orb fill shader not available. Using fallback rectangle rendering.");
                shaderUnavailableWarned = true;
            }
            drawFallbackFill(graphics, x, y, size, fillPercent, color);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        if (fillPercentUniform != null) {
            fillPercentUniform.set(fillPercent);
        }
        if (halfModeUniform != null) {
            halfModeUniform.set(halfMode);
        }
        if (overlapWidthUniform != null) {
            overlapWidthUniform.set(overlapWidth);
        }
        if (enableNoiseUniform != null) {
            enableNoiseUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbNoise() ? 1 : 0);
        }
        if (enableLiquidShadowUniform != null) {
            enableLiquidShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableLiquidShadow() ? 1 : 0);
        }
        if (enableOrbInnerShadowUniform != null) {
            enableOrbInnerShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbInnerShadow() ? 1 : 0);
        }

        RenderSystem.setShader(ORB_FILL_SHADER_SUPPLIER);

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
        if (useTextureUniform != null) {
            useTextureUniform.set(0);
        }
        RenderSystem.defaultBlendFunc();
    }

    /**
     * テクスチャ付き円形フィルを描画（OVERLAPモード用）
     *
     * @param texture 使用するResourceLocation
     * @param halfMode HalfMode値（3指定）
     * @param overlapWidth 弧形境界線の幅
     */
    public static void drawCircularFillWithTexture(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, ResourceLocation texture, int halfMode, float overlapWidth) {
        if (fillPercent <= 0) {
            return;
        }

        if (orbFillShader == null) {
            // シェーダー未登録時は完全不透明な単色でフォールバック
            drawFallbackFill(graphics, x, y, size, fillPercent, 0xFF00E6FF);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        if (fillPercentUniform != null) {
            fillPercentUniform.set(fillPercent);
        }
        if (halfModeUniform != null) {
            halfModeUniform.set(halfMode);
        }
        if (overlapWidthUniform != null) {
            overlapWidthUniform.set(overlapWidth);
        }
        if (useTextureUniform != null) {
            useTextureUniform.set(1);
        }
        if (enableNoiseUniform != null) {
            enableNoiseUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbNoise() ? 1 : 0);
        }
        if (enableLiquidShadowUniform != null) {
            enableLiquidShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableLiquidShadow() ? 1 : 0);
        }
        if (enableOrbInnerShadowUniform != null) {
            enableOrbInnerShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbInnerShadow() ? 1 : 0);
        }

        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ORB_FILL_SHADER_SUPPLIER);
        // テクスチャ自身の色をそのまま使用するため白色で設定
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        float adX = x - PADDING + OFFSET_X;
        float adY = y - PADDING + OFFSET_Y;
        float adSize = size + (PADDING * 2.0f);

        buffer.vertex(matrix, adX, adY + adSize, 0).uv(0, 1).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        buffer.vertex(matrix, adX + adSize, adY + adSize, 0).uv(1, 1).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        buffer.vertex(matrix, adX + adSize, adY, 0).uv(1, 0).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();
        buffer.vertex(matrix, adX, adY, 0).uv(0, 0).color(1.0f, 1.0f, 1.0f, 1.0f).endVertex();

        tesselator.end();

        if (useTextureUniform != null) {
            useTextureUniform.set(0);
        }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.defaultBlendFunc();
    }

    public static void drawCircularFillAdditive(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        if (fillPercent <= 0) {
            return;
        }
        
        if (orbFillShader == null) {
            drawFallbackFill(graphics, x, y, size, fillPercent, color);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);

        if (fillPercentUniform != null) {
            fillPercentUniform.set(fillPercent);
        }
        if (enableNoiseUniform != null) {
            enableNoiseUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbNoise() ? 1 : 0);
        }
        if (enableLiquidShadowUniform != null) {
            enableLiquidShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableLiquidShadow() ? 1 : 0);
        }
        if (enableOrbInnerShadowUniform != null) {
            enableOrbInnerShadowUniform.set(com.example.exile_overlay.client.config.EquipmentDisplayConfig.getInstance().isEnableOrbInnerShadow() ? 1 : 0);
        }

        RenderSystem.setShader(ORB_FILL_SHADER_SUPPLIER);

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

    /**
     * シェーダー未登録時のフォールバック描画
     * 下からfillPercent分だけ矩形で塗りつぶす
     */
    private static void drawFallbackFill(GuiGraphics graphics, int x, int y, int size,
            float fillPercent, int color) {
        if (fillPercent <= 0) {
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float fillHeight = (size + PADDING * 2) * fillPercent;
        float drawX = x - PADDING + OFFSET_X;
        float drawY = y - PADDING + OFFSET_Y + (size + PADDING * 2) - fillHeight;
        float drawW = size + PADDING * 2;

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, drawX, drawY + fillHeight, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, drawX + drawW, drawY + fillHeight, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, drawX + drawW, drawY, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, drawX, drawY, 0).color(r, g, b, a).endVertex();

        tesselator.end();
        RenderSystem.disableBlend();
    }
}