package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * オーブ描画を担当するクラス
 *
 * 【Layered Sandwich Rendering Architecture】
 * 描画を2つのレイヤーに分割：
 * 1. Fill Layer (中間): 背景フレームの「後ろ」に液面を描画
 * 2. Overlay Layer (最前面): 背景フレームの「前」に反射、テキストを描画
 * 
 * 背景画像の透明部分を「抜型」として利用し、円形に見せる仕組み
 *
 * 【レンダリング方式】
 * - GPUシェーダー方式（推奨）: OrbShaderRenderer
 * - カスタムシェーダーで円形マスクと液面アニメーション
 * - CPU負荷: ほぼゼロ
 * - 画質: 最高（滑らかな縁、波アニメーション対応）
 */
public class OrbRenderer {

    private static final ResourceLocation REFLECTION_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/orb_reflection.png");

    private static boolean shouldSkipRender(OrbConfig config, Player player) {
        return !config.isVisible(player) || config.isOverlay();
    }

    /**
     * オーブ設定から動的な色を取得
     * ResourceSlotManagerに対応するスロットがある場合はそこから色を取得
     */
    private static int getDynamicColor(OrbConfig config, Player player) {
        String orbId = config.getId();
        String slotId = mapOrbIdToSlotId(orbId);
        
        if (slotId != null) {
            int dynamicColor = ResourceSlotManager.getInstance().getActiveColor(slotId, player);
            // デフォルト色（グレー）でない場合は動的色を使用
            if (dynamicColor != 0x808080) {
                return dynamicColor;
            }
        }
        
        return config.getColor();
    }

    /**
     * オーブIDをResourceSlotManagerのスロットIDにマッピング
     */
    private static String mapOrbIdToSlotId(String orbId) {
        return switch (orbId) {
            case "orb_1" -> "orb1";
            case "orb_1_overlay" -> "orb1_overlay";
            case "orb_2", "orb_2_blood" -> "orb2";
            case "orb_3" -> "orb3";
            default -> null;
        };
    }

    public static void render(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (shouldSkipRender(config, player))
            return;

        renderFillLayer(graphics, config, player);
        renderOverlayLayer(graphics, config, player, mc);
    }

    public static void renderFillLayer(GuiGraphics graphics, OrbConfig config, Player player) {
        if (shouldSkipRender(config, player))
            return;

        float current = config.getDataProvider().getCurrentValue(player);
        float max = config.getDataProvider().getMaxValue(player);
        float percent = max > 0 ? Math.min(current / max, 1.0f) : 0;

        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        
        // 動的な色を取得（ResourceSlotManagerから）
        int color = getDynamicColor(config, player);

        OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, percent, color);

        if (config.hasOverlayColor() && config.getOverlayProvider() != null) {
            renderOverlayFillLayer(graphics, config, player);
        }

        if (config.shouldShowReflection()) {
            RenderSystem.enableBlend();
            // 反射テクスチャを1px拡大、左に2pxずらす
            graphics.blit(REFLECTION_TEXTURE, orbX - 2, orbY, 0, 0, 0, orbSize + 1, orbSize + 1, orbSize + 1, orbSize + 1);
        }
    }

    public static void renderOverlayLayer(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (shouldSkipRender(config, player))
            return;

        if (config.getDataProvider().shouldShowValue()) {
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);
            int orbX = config.getCenterX();
            int orbY = config.getCenterY();
            int orbSize = config.getSize();

            String text = (int) current + " / " + (int) max;
            float textScale = config.getDataProvider().getTextScale();
            renderCenteredScaledText(graphics, mc, text, orbX + orbSize / 2f, orbY + orbSize / 2f, textScale, 0xFFFFFFFF);
        }
    }

    private static void renderOverlayFillLayer(GuiGraphics graphics, OrbConfig config, Player player) {
        OrbDataProvider overlayProvider = config.getOverlayProvider();
        if (overlayProvider == null)
            return;

        float currentOverlay = overlayProvider.getCurrentValue(player);
        float maxOverlay = overlayProvider.getMaxValue(player);
        float overlayPercent = maxOverlay > 0 ? Math.min(currentOverlay / maxOverlay, 1.0f) : 0;

        if (overlayPercent <= 0)
            return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);

        OrbShaderRenderer.drawCircularFill(graphics, config.getCenterX(), config.getCenterY(), config.getSize(),
                overlayPercent, config.getOverlayColor());

        RenderSystem.defaultBlendFunc();
    }

    /**
     * 中央揃えのテキストを描画
     * 
     * @param graphics GuiGraphics
     * @param mc       Minecraftインスタンス
     * @param text     表示するテキスト
     * @param centerX  中心X座標
     * @param centerY  中心Y座標
     * @param scale    スケール
     * @param color    色
     */
    public static void renderCenteredScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float centerX, float centerY, float scale, int color) {
        int textWidth = mc.font.width(text);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.drawString(mc.font, text, -textWidth / 2, -mc.font.lineHeight / 2, color, true);
        graphics.pose().popPose();
    }
}
