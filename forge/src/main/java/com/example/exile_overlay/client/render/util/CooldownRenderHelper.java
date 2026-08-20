package com.example.exile_overlay.client.render.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/**
 * クールダウン表示用の描画ヘルパー。
 * 時計回りに回転して開く扇形（ラジアルスイープ）クールダウンオーバーレイ。
 * 外周交点ポリゴン計算で正方形・矩形アイコンに完全にフィットする。固定配列再利用でゼロアロケーション。
 */
public final class CooldownRenderHelper {

    private static final float[] TEMP_VX = new float[6];
    private static final float[] TEMP_VY = new float[6];

    private CooldownRenderHelper() {}

    /**
     * 円形（時計回りラジアルスイープ）クールダウンオーバーレイを描画
     *
     * @param graphics     描画用GuiGraphics
     * @param x            アイコン左上X座標
     * @param y            アイコン左上Y座標
     * @param width        アイコン幅
     * @param height       アイコン高さ
     * @param percent      残りクールダウン割合 (0.0 = 完了, 1.0 = 開始直後)
     * @param overlayColor 暗色オーバーレイのARGBカラー (例: 0xAA000000)
     */
    public static void drawRadialCooldown(GuiGraphics graphics, int x, int y, int width, int height,
                                          float percent, int overlayColor) {
        if (percent <= 0.0f) {
            return;
        }

        // 先行するGuiGraphicsの描画(アイコン等)をGPUにフラッシュして確実に下層に描画
        graphics.flush();

        float cx = x + width / 2.0f;
        float cy = y + height / 2.0f;
        float hw = width / 2.0f;
        float hh = height / 2.0f;

        float a = ((overlayColor >> 24) & 0xFF) / 255.0f;
        float r = ((overlayColor >> 16) & 0xFF) / 255.0f;
        float g = ((overlayColor >> 8) & 0xFF) / 255.0f;
        float b = (overlayColor & 0xFF) / 255.0f;

        Matrix4f matrix = graphics.pose().last().pose();
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        buffer.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);

        if (percent >= 1.0f) {
            // 全面塗りつぶし (2つの三角形)
            buffer.vertex(matrix, x, y, 0.0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x, y + height, 0.0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x + width, y + height, 0.0f).color(r, g, b, a).endVertex();

            buffer.vertex(matrix, x, y, 0.0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x + width, y + height, 0.0f).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, x + width, y, 0.0f).color(r, g, b, a).endVertex();
        } else {
            double twoPi = 2.0 * Math.PI;
            // 針の進行角度 (12時=0、時計回り)
            double theta = twoPi * (1.0 - percent);

            double alpha = Math.atan2(hw, hh);
            double theta1 = alpha;
            double theta2 = Math.PI - alpha;
            double theta3 = Math.PI + alpha;
            double theta4 = twoPi - alpha;

            // 針の先端と矩形外周との交点を計算
            double sin = Math.sin(theta);
            double cos = Math.cos(theta); // 画面座標系では上が -Y、下が +Y
            double dx = sin;
            double dy = -cos;

            double tX = (Math.abs(dx) > 1e-7) ? (dx > 0 ? (hw / dx) : (-hw / dx)) : Double.POSITIVE_INFINITY;
            double tY = (Math.abs(dy) > 1e-7) ? (dy > 0 ? (hh / dy) : (-hh / dy)) : Double.POSITIVE_INFINITY;
            double t = Math.min(tX, tY);

            float startX = (float) (cx + t * dx);
            float startY = (float) (cy + t * dy);

            // 外周頂点列を構築
            int count = 0;
            TEMP_VX[count] = startX;
            TEMP_VY[count] = startY;
            count++;

            if (theta < theta1) {
                TEMP_VX[count] = cx + hw;
                TEMP_VY[count] = cy - hh;
                count++;
            }
            if (theta < theta2) {
                TEMP_VX[count] = cx + hw;
                TEMP_VY[count] = cy + hh;
                count++;
            }
            if (theta < theta3) {
                TEMP_VX[count] = cx - hw;
                TEMP_VY[count] = cy + hh;
                count++;
            }
            if (theta < theta4) {
                TEMP_VX[count] = cx - hw;
                TEMP_VY[count] = cy - hh;
                count++;
            }

            // 12時 (真上) で閉じる
            TEMP_VX[count] = cx;
            TEMP_VY[count] = cy - hh;
            count++;

            for (int i = 0; i < count - 1; i++) {
                buffer.vertex(matrix, cx, cy, 0.0f).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, TEMP_VX[i], TEMP_VY[i], 0.0f).color(r, g, b, a).endVertex();
                buffer.vertex(matrix, TEMP_VX[i + 1], TEMP_VY[i + 1], 0.0f).color(r, g, b, a).endVertex();
            }
        }

        tesselator.end();

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
