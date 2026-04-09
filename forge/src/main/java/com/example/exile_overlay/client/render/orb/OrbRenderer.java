package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.client.config.OrbTextConfig;
import com.example.exile_overlay.client.render.HudFontHelper;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import com.example.exile_overlay.util.MineAndSlashHelper;
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
 *
 * 【エナジーシールド（ES）オーバーレイ】
 * HJUD Mod方式: HPオーブ上にシアン色の半透明レイヤーを下から上に描画
 * - 色: シアン/スカイブルー (0.0f, 0.9f, 1.0f, 0.7f)
 * - 描画順: 下から上へ溜まる方式
 * - Mine and SlashのMagic Shieldデータを使用
 *
 */
public class OrbRenderer {

    private static final ResourceLocation REFLECTION_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/orb_reflection.png");

    // ES（エナジーシールド）の色設定（HJUD Mod方式）
    private static final int ES_COLOR = 0x6600E6FF;

    private static String formatValue(int value, boolean compact) {
        if (!compact || value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return (value / 1000) + "k";
        }
        return (value / 1_000_000) + "m";
    }

    private static String formatValuePair(int current, int max, String separator, boolean compact) {
        return formatValue(current, compact) + separator + formatValue(max, compact);
    }

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

        int color = getDynamicColor(config, player);

        OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, percent, color);

        // ORB_1（HPオーブ）の場合、ESオーバーレイを描画（HJUD Mod方式）
        if ("orb_1".equals(config.getId())) {
            renderEsOverlay(graphics, orbX, orbY, orbSize, player);
        }

        if (config.hasOverlayColor() && config.getOverlayProvider() != null) {
            renderOverlayFillLayer(graphics, config, player);
        }

        if (config.shouldShowReflection()) {
            RenderSystem.enableBlend();
            graphics.blit(REFLECTION_TEXTURE, orbX - 2, orbY - 2, 0, 0, 0, orbSize + 2, orbSize + 2, orbSize + 2, orbSize + 2);
        }
    }

    public static void renderOverlayLayer(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (shouldSkipRender(config, player))
            return;

        OrbTextConfig textConfig = OrbTextConfig.getInstance();
        if (!textConfig.isShowOrbText()) {
            return;
        }

        boolean compact = textConfig.isCompactNumbers();
        boolean isEnergyOrb = "orb_3".equals(config.getId());
        float scaleFactor = textConfig.getTextScale();
        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        float centerX = orbX + orbSize / 2f;

        if ("orb_1".equals(config.getId())) {
            renderHpEsValues(graphics, config, player, mc, orbX, orbY, orbSize, centerX, compact, scaleFactor);
        } else if (config.getDataProvider().shouldShowValue()) {
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);
            String text = formatValuePair((int) current, (int) max, "/", compact || isEnergyOrb);
            float effectiveScale = isEnergyOrb ? textConfig.getEnergyTextScale() : scaleFactor;
            float textScale = config.getDataProvider().getTextScale() * effectiveScale;
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, textScale, 0xFFFFFFFF);
        }
    }

    private static void renderHpEsValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, float centerX, boolean compact, float scaleFactor) {
        float hpCurrent = config.getDataProvider().getCurrentValue(player);
        float hpMax = config.getDataProvider().getMaxValue(player);
        float esCurrent = MineAndSlashHelper.getCurrentMagicShield(player);
        float esMax = MineAndSlashHelper.getMaxMagicShield(player);

        int whiteColor = 0xFFFFFFFF;

        if (esMax <= 0) {
            String text = formatValuePair((int) hpCurrent, (int) hpMax, "/", compact);
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, 0.75f * scaleFactor, whiteColor);
            return;
        }

        boolean hpIsLarger = hpMax >= esMax;
        float largerCurrent = hpIsLarger ? hpCurrent : esCurrent;
        float largerMax = hpIsLarger ? hpMax : esMax;
        float smallerCurrent = hpIsLarger ? esCurrent : hpCurrent;
        float smallerMax = hpIsLarger ? esMax : hpMax;

        String largerText = formatValuePair((int) largerCurrent, (int) largerMax, "/", compact);
        renderCenteredScaledText(graphics, mc, largerText, centerX, orbY + orbSize / 2f - 4, 0.75f * scaleFactor, whiteColor);

        String smallerText = formatValuePair((int) smallerCurrent, (int) smallerMax, "/", compact);
        renderCenteredScaledText(graphics, mc, smallerText, centerX, orbY + orbSize / 2f + 5, 0.55f * scaleFactor, whiteColor);
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

        OrbShaderRenderer.drawCircularFill(graphics, config.getCenterX(), config.getCenterY(), config.getSize(),
                overlayPercent, config.getOverlayColor());
    }

    /**
     * エナジーシールド（ES）オーバーレイを描画（HJUD Mod方式）
     */
    private static void renderEsOverlay(GuiGraphics graphics, int x, int y, int size, Player player) {
        float currentEs = MineAndSlashHelper.getCurrentMagicShield(player);
        float maxEs = MineAndSlashHelper.getMaxMagicShield(player);

        if (maxEs <= 0 || currentEs <= 0) {
            return;
        }

        float esPercent = Math.min(currentEs / maxEs, 1.0f);
        if (esPercent <= 0) {
            return;
        }

        OrbShaderRenderer.drawCircularFill(graphics, x, y, size, esPercent, ES_COLOR);
    }

    /**
     * 中央揃えのテキストを描画
     */
    public static void renderCenteredScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float centerX, float centerY, float scale, int color) {
        int textWidth = HudFontHelper.getTextWidth(mc.font, text);

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, text, -textWidth / 2, -mc.font.lineHeight / 2, color, true);
        graphics.pose().popPose();
    }
}
