package com.example.exile_overlay.client.render.orb;

import com.example.exile_overlay.client.config.OrbColorConfig;
import com.example.exile_overlay.client.config.OrbTextConfig;
import com.example.exile_overlay.client.render.HudFontHelper;
import com.example.exile_overlay.client.render.resource.ResourceSlotManager;
import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * オーブ描画を担当するクラス。
 * レイヤー構造: Fill(液面メインフレーム背面) → Overlay(反射・テキスト：メインフレーム前面)。
 * 背景画像の透明部分を担当型として利用し、円形に見せる仕組み。
 * GPUシェーダー方式（OrbShaderRenderer）で円形マスクと液面アニメーションを実現。
 * ESオーバーレイ: HPオーブ上にシアン層を下から上に描画（HJUD Mod方式）。
 */
public class OrbRenderer {

    private static final OrbTextConfig TEXT_CONFIG = OrbTextConfig.getInstance();

    /**
     * 二段組・分割表示時のスケール比（メインテキストに対する比率）
     * 0.55/0.75 ≈ 0.733 を踏襲
     */
    private static final float COMPACT_SCALE_RATIO = 0.55f / 0.75f;

    /**
     * ABOVEモードの整基準X座標（HotbarRenderCommandの経験値バーと同じ値）
     * 経験値バー: EXP_BAR_X=65, EXP_BAR_WIDTH=509 → 右端=574
     */
    private static final float ABOVE_LEFT_ALIGN_X = 65.0f;
    private static final float ABOVE_RIGHT_ALIGN_X = 65.0f + 509.0f;
    private static final float ABOVE_LINE_GAP = 1.0f;

    private static String formatValue(int value, boolean compact) {
        if (!compact || value < 1000) {
            return String.valueOf(value);
        }
        if (value < 1_000_000) {
            return String.format("%.1fk", value / 1000.0);
        }
        return String.format("%.1fm", value / 1_000_000.0);
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
        String slotId = mapOrbIdToSlotId(orbId, player);

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
    private static String mapOrbIdToSlotId(String orbId, Player player) {
        boolean swapped = OrbDataProviders.isSwapped(player);
        return switch (orbId) {
            case "orb_1" -> "orb1";
            case "orb_1_overlay" -> "orb1_overlay";
            case "orb_2", "orb_2_blood" -> swapped ? "orb3" : "orb2";
            case "orb_3" -> swapped ? "orb2" : "orb3";
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

        boolean hasOffset = config.getRenderOffsetX() != 0 || config.getRenderOffsetY() != 0;
        if (hasOffset) {
            graphics.pose().pushPose();
            graphics.pose().translate(config.getRenderOffsetX(), config.getRenderOffsetY(), 0);
        }

        float current = config.getDataProvider().getCurrentValue(player);
        float max = config.getDataProvider().getMaxValue(player);
        if (OrbDummyPreviewManager.getInstance().isDummyPreviewActive()) {
            if (max <= 0) max = 100.0f;
            if (current <= 0) current = 75.0f;
        }
        float targetPercent = max > 0 ? Math.min(current / max, 1.0f) : 0;
        float percent = OrbSmoothedValue.getSmoothedPercent(config.getId(), targetPercent);

        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();

        // バックプレート（黒い背景）の描画。テクスチャから完全移行
        // 色: 1e1c1f, アルファ値: 214 (0xD6) -> 0xD61E1C1F
        int bgPlateColor = 0xD61E1C1F;
        OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, 1.0f, bgPlateColor);

        int color = getDynamicColor(config, player);

        boolean isOrb1 = "orb_1".equals(config.getId());
        boolean splitMode = isOrb1 && TEXT_CONFIG.isSplitOrb1();

        if (splitMode) {
            float esMax = getEsMaxValue(player);

            if (esMax <= 0) {
                OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, percent, color);
            } else {
                float hpCurrent = config.getDataProvider().getCurrentValue(player);
                float hpMax = config.getDataProvider().getMaxValue(player);
                float hpPercent = hpMax > 0 ? Math.min(hpCurrent / hpMax, 1.0f) : 0;
                float hpSmoothed = OrbSmoothedValue.getSmoothedPercent(config.getId() + "_split_hp", hpPercent);
                OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, hpSmoothed, OrbColorConfig.getInstance().getHealthColor(), 1);

                float esCurrent = getEsCurrentValue(player);
                if (esCurrent > 0) {
                    float esPercent = Math.min(esCurrent / esMax, 1.0f);
                    float esSmoothed = OrbDummyPreviewManager.getInstance().isDummyPreviewActive()
                            ? esPercent
                            : OrbSmoothedValue.getSmoothedPercent(config.getId() + "_split_ms", esPercent);
                    OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, esSmoothed, OrbColorConfig.getInstance().getShieldColor(), 2);
                }
            }
        } else {
            boolean hideLower = isOrb1 && TEXT_CONFIG.isHideLowerHpEsGaugeOrb1();
            float hpMax = max;
            float esMax = isOrb1 ? getEsMaxValue(player) : 0;

            boolean hpIsLower = hideLower && esMax > 0 && hpMax < esMax;
            boolean esIsLower = hideLower && esMax > 0 && hpMax >= esMax;

            if (!hpIsLower) {
                OrbShaderRenderer.drawCircularFill(graphics, orbX, orbY, orbSize, percent, color);
            }

            if (isOrb1 && !esIsLower) {
                renderEsOverlay(graphics, orbX, orbY, orbSize, player, hpMax);
            }
        }

        if (config.hasOverlayColor() && config.getOverlayProvider() != null) {
            renderOverlayFillLayer(graphics, config, player);
        }

        if (config.shouldShowReflection()) {
            RenderSystem.enableBlend();
            graphics.pose().pushPose();
            graphics.pose().translate(config.getReflectionX(), config.getReflectionY(), 0.0f);
            int width = Math.round(config.getReflectionWidth());
            int height = Math.round(config.getReflectionHeight());
            graphics.blit(config.getReflectionTexture(), 0, 0, 0, 0, 0, width, height, width, height);
            graphics.pose().popPose();
        }

        if (hasOffset) {
            graphics.pose().popPose();
        }
    }

    public static void renderOverlayLayer(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (shouldSkipRender(config, player))
            return;

        OrbTextConfig textConfig = TEXT_CONFIG;
        if (!textConfig.isShowOrbText()) {
            return;
        }

        boolean hasOffset = config.getRenderOffsetX() != 0 || config.getRenderOffsetY() != 0;
        if (hasOffset) {
            graphics.pose().pushPose();
            graphics.pose().translate(config.getRenderOffsetX(), config.getRenderOffsetY(), 0);
        }

        boolean compact = textConfig.isCompactNumbers();
        boolean isEnergyOrb = "orb_3".equals(config.getId());
        boolean aboveMode = textConfig.getTextPosition() != OrbTextConfig.OrbTextPosition.CENTER;
        boolean isIntegrated = textConfig.getTextPosition() == OrbTextConfig.OrbTextPosition.ABOVE_INTEGRATED;
        float scaleFactor = aboveMode ? textConfig.getAboveTextScale() : textConfig.getTextScale();
        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        float centerX = orbX + orbSize / 2f;
        boolean isMainOrb = "orb_1".equals(config.getId()) || "orb_2".equals(config.getId());

        if (aboveMode) {
            if (isMainOrb) {
                renderAboveOrbValues(graphics, config, player, mc, orbY, compact, scaleFactor, isIntegrated);
                return;
            }
            if (isEnergyOrb) {
                if (!isIntegrated) {
                    renderAboveEnergyValue(graphics, config, player, mc, orbX, orbY, orbSize, compact);
                }
                return;
            }
            return;
        }

        if ("orb_1".equals(config.getId())) {
            if (TEXT_CONFIG.isSplitOrb1()
                    && getEsMaxValue(player) > 0) {
                renderSplitHpMsValues(graphics, config, player, mc, orbX, orbY, orbSize, compact, scaleFactor);
            } else {
                renderHpEsValues(graphics, config, player, mc, orbX, orbY, orbSize, centerX, compact, scaleFactor);
            }
        } else if (config.getDataProvider().shouldShowValue()) {
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);
            boolean energyCompact = isEnergyOrb && textConfig.isEnergyCompact();
            String text = formatValuePair((int) current, (int) max, "/", compact || energyCompact);
            float effectiveScale = isEnergyOrb ? textConfig.getEnergyTextScale() : scaleFactor;
            float textScale = config.getDataProvider().getTextScale() * effectiveScale;
            float textCenterX = isEnergyOrb ? centerX - 0.5f : centerX;
            renderCenteredScaledText(graphics, mc, text, textCenterX, orbY + orbSize / 2f, textScale, 0xFFFFFFFF, TEXT_CONFIG.isOrbTextShadow());
        }

        if (hasOffset) {
            graphics.pose().popPose();
        }
    }

    private static void renderHpEsValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, float centerX, boolean compact, float scaleFactor) {
        float hpCurrent = config.getDataProvider().getCurrentValue(player);
        float hpMax = config.getDataProvider().getMaxValue(player);
        float esCurrent = getEsCurrentValue(player);
        float esMax = getEsMaxValue(player);

        int whiteColor = 0xFFFFFFFF;

        if (esMax <= 0) {
            String text = formatValuePair((int) hpCurrent, (int) hpMax, "/", compact);
            float textScale = config.getDataProvider().getTextScale() * scaleFactor;
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, textScale, whiteColor, TEXT_CONFIG.isOrbTextShadow());
            return;
        }

        float baseProviderScale = config.getDataProvider().getTextScale();
        float hpScale = baseProviderScale * TEXT_CONFIG.getTextScale();
        float esScale = baseProviderScale * TEXT_CONFIG.getEsTextScale();

        boolean hpIsLarger = hpMax >= esMax;
        float largerScale = hpIsLarger ? hpScale : esScale;
        float smallerBaseScale = hpIsLarger ? esScale : hpScale;
        float largerCurrent = hpIsLarger ? hpCurrent : esCurrent;
        float largerMax = hpIsLarger ? hpMax : esMax;
        float smallerCurrent = hpIsLarger ? esCurrent : hpCurrent;
        float smallerMax = hpIsLarger ? esMax : hpMax;

        float centerY = orbY + orbSize / 2f;
        boolean hideSmaller = TEXT_CONFIG.isHideOrb1SmallerValue();

        if (hideSmaller) {
            String largerText = formatValuePair((int) largerCurrent, (int) largerMax, "/", compact);
            renderCenteredScaledText(graphics, mc, largerText, centerX, centerY, largerScale, whiteColor, TEXT_CONFIG.isOrbTextShadow());
            return;
        }

        float compactSmallerScale = smallerBaseScale * COMPACT_SCALE_RATIO;
        float largerHalfH = mc.font.lineHeight * largerScale / 2f;
        float smallerHalfH = mc.font.lineHeight * compactSmallerScale / 2f;
        float gap = 1.0f;
        float totalHeight = 2 * largerHalfH + gap + 2 * smallerHalfH;

        String largerText = formatValuePair((int) largerCurrent, (int) largerMax, "/", compact);
        renderCenteredScaledText(graphics, mc, largerText, centerX, centerY - totalHeight / 2f + largerHalfH, largerScale, whiteColor, TEXT_CONFIG.isOrbTextShadow());

        String smallerText = formatValuePair((int) smallerCurrent, (int) smallerMax, "/", compact);
        renderCenteredScaledText(graphics, mc, smallerText, centerX, centerY + totalHeight / 2f - smallerHalfH, compactSmallerScale, whiteColor, TEXT_CONFIG.isOrbTextShadow());
    }

    private static void renderSplitHpMsValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, boolean compact, float scaleFactor) {
        float hpCurrent = config.getDataProvider().getCurrentValue(player);
        float hpMax = config.getDataProvider().getMaxValue(player);
        float esCurrent = getEsCurrentValue(player);
        float esMax = getEsMaxValue(player);

        float halfSize = orbSize / 2f;
        float baseScale = config.getDataProvider().getTextScale() * COMPACT_SCALE_RATIO;
        float hpTextScale = baseScale * TEXT_CONFIG.getTextScale();
        float esTextScale = baseScale * TEXT_CONFIG.getEsTextScale();

        String hpText = formatValuePair((int) hpCurrent, (int) hpMax, "/", compact);
        float hpCenterX = orbX + halfSize / 2f;
        float centerY = orbY + orbSize / 2f;
        renderCenteredScaledText(graphics, mc, hpText, hpCenterX, centerY, hpTextScale, 0xFFFFFFFF, TEXT_CONFIG.isOrbTextShadow());

        if (esMax > 0 && !TEXT_CONFIG.isHideOrb1SmallerValue()) {
            String msText = formatValuePair((int) esCurrent, (int) esMax, "/", compact);
            float msCenterX = orbX + halfSize + halfSize / 2f;
            renderCenteredScaledText(graphics, mc, msText, msCenterX, centerY, esTextScale, 0xFFFFFFFF, TEXT_CONFIG.isOrbTextShadow());
        }
    }

    /**
     * オーブ上部に数値を表示するモード。
     * 左グループ(HP/ES/Energy): 経験値バー左端に左揃え、縦3行。
     * 右グループ(マナ): 経験値バー右端に右揃えの1行。
     * 全ての数値は同じスケール（textScale × dataProvider.getTextScale）。
     */
    private static void renderAboveOrbValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbY, boolean compact, float scaleFactor, boolean isIntegrated) {
        final int whiteColor = 0xFFFFFFFF;
        boolean shadow = TEXT_CONFIG.isOrbTextShadow();
        float lineH = mc.font.lineHeight;
        float offsetY = TEXT_CONFIG.getAboveOrbOffsetY();
        float offsetX = TEXT_CONFIG.getAboveOrbOffsetX();
        float leftX = ABOVE_LEFT_ALIGN_X - offsetX;
        float rightX = ABOVE_RIGHT_ALIGN_X + offsetX;
        float scale = 0.8f * scaleFactor;
        float textH = lineH * scale;
        float firstLineCenterY = orbY - offsetY - textH / 2f;
        float linePitch = textH + ABOVE_LINE_GAP;

        if ("orb_1".equals(config.getId())) {
            float hpCurrent = config.getDataProvider().getCurrentValue(player);
            float hpMax = config.getDataProvider().getMaxValue(player);
            float esCurrent = getEsCurrentValue(player);
            float esMax = getEsMaxValue(player);
            boolean showEs = esMax > 0 && !TEXT_CONFIG.isHideOrb1SmallerValue();

            if (isIntegrated) {
                float subCurrent = OrbDataProviders.ORB_3.getCurrentValue(player);
                float subMax = OrbDataProviders.ORB_3.getMaxValue(player);
                boolean showSub = OrbRegistry.isOrbVisible(player, OrbType.ORB_3);

                int totalLines = 1;
                if (showEs) totalLines++;
                if (showSub && subMax > 0) totalLines++;

                String hpText = formatValuePair((int) hpCurrent, (int) hpMax, "/", compact);
                renderLeftAlignedScaledText(graphics, mc, hpText, leftX,
                        firstLineCenterY - (totalLines - 1) * linePitch, scale, whiteColor, shadow);

                int currentLine = totalLines - 2;
                if (showSub && subMax > 0) {
                    boolean energyCompact = compact || TEXT_CONFIG.isEnergyCompact();
                    String subText = formatValuePair((int) subCurrent, (int) subMax, "/", energyCompact);
                    renderLeftAlignedScaledText(graphics, mc, subText, leftX,
                            firstLineCenterY - currentLine * linePitch, scale, whiteColor, shadow);
                    currentLine--;
                }

                if (showEs) {
                    String esText = formatValuePair((int) esCurrent, (int) esMax, "/", compact);
                    renderLeftAlignedScaledText(graphics, mc, esText, leftX,
                            firstLineCenterY - currentLine * linePitch, scale, whiteColor, shadow);
                }
            } else {
                int totalLines = 1;
                if (showEs) totalLines++;

                String hpText = formatValuePair((int) hpCurrent, (int) hpMax, "/", compact);
                renderLeftAlignedScaledText(graphics, mc, hpText, leftX,
                        firstLineCenterY - (totalLines - 1) * linePitch, scale, whiteColor, shadow);

                int currentLine = totalLines - 2;
                if (showEs) {
                    String esText = formatValuePair((int) esCurrent, (int) esMax, "/", compact);
                    renderLeftAlignedScaledText(graphics, mc, esText, leftX,
                            firstLineCenterY - currentLine * linePitch, scale, whiteColor, shadow);
                }
            }
        } else if (config.getDataProvider().shouldShowValue()) {
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);

            String text = formatValuePair((int) current, (int) max, "/", compact);
            renderRightAlignedScaledText(graphics, mc, text, rightX, firstLineCenterY, scale, whiteColor, shadow);
        }
    }

    /**
     * ABOVEモード時にエネルギー（orb_3）の真上に数値を描画する
     */
    private static void renderAboveEnergyValue(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, boolean compact) {
        float current = config.getDataProvider().getCurrentValue(player);
        float max = config.getDataProvider().getMaxValue(player);
        if (max <= 0) return;

        boolean shadow = TEXT_CONFIG.isOrbTextShadow();
        float offsetY = TEXT_CONFIG.getAboveOrbOffsetY();
        float scale = 0.8f * TEXT_CONFIG.getEnergyTextScale();
        float textH = mc.font.lineHeight * scale;

        // 中心X座標
        float centerX = orbX + orbSize / 2f;
        // Yオフセットを適用したY座標（上辺から offsetY 離れた位置の中心Y）
        float centerY = orbY - offsetY - textH / 2f;

        boolean energyCompact = compact || TEXT_CONFIG.isEnergyCompact();
        String text = formatValuePair((int) current, (int) max, "/", energyCompact);
        renderCenteredScaledText(graphics, mc, text, centerX, centerY, scale, 0xFFFFFFFF, shadow);
    }

    private static void renderOverlayFillLayer(GuiGraphics graphics, OrbConfig config, Player player) {
        OrbDataProvider overlayProvider = config.getOverlayProvider();
        if (overlayProvider == null)
            return;

        float currentOverlay = overlayProvider.getCurrentValue(player);
        float maxOverlay = overlayProvider.getMaxValue(player);
        float targetOverlayPercent = maxOverlay > 0 ? Math.min(currentOverlay / maxOverlay, 1.0f) : 0;
        float overlayPercent = OrbSmoothedValue.getSmoothedPercent(config.getId() + "_overlay", targetOverlayPercent);

        if (overlayPercent <= 0)
            return;

        OrbShaderRenderer.drawCircularFill(graphics, config.getCenterX(), config.getCenterY(), config.getSize(),
                overlayPercent, config.getOverlayColor());
    }

    private static float getEsCurrentValue(Player player) {
        if (OrbDummyPreviewManager.getInstance().isDummyPreviewActive()) {
            return 100.0f;
        }
        return ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT);
    }

    private static float getEsMaxValue(Player player) {
        if (OrbDummyPreviewManager.getInstance().isDummyPreviewActive()) {
            return 100.0f;
        }
        return ModDataProviderRegistry.getMaxValue(player, DataType.ORB_1_OVERLAY_MAX);
    }

    /**
     * エナジーシールド（ES）オーバーレイを描画（HJUD Mod方式または重ね合わせ方式）
     */
    private static void renderEsOverlay(GuiGraphics graphics, int x, int y, int size, Player player, float hpMax) {
        float currentEs = getEsCurrentValue(player);
        float maxEs = getEsMaxValue(player);

        if (maxEs <= 0 || currentEs <= 0) {
            return;
        }

        float esPercent = Math.min(currentEs / maxEs, 1.0f);
        if (!OrbDummyPreviewManager.getInstance().isDummyPreviewActive()) {
            esPercent = OrbSmoothedValue.getSmoothedPercent("orb_1_es", esPercent);
        }
        if (esPercent <= 0) {
            return;
        }

        if (TEXT_CONFIG.isOverlapHpEsOrb1()) {
            float calculatedHpMax = hpMax > 0 ? hpMax : 100.0f;
            float ratio = maxEs / (calculatedHpMax + maxEs);
            float overlapWidth = Math.min(0.75f, Math.max(1.0f / 6.0f, ratio));
            OrbShaderRenderer.drawCircularFill(graphics, x, y, size, esPercent, OrbColorConfig.getInstance().getShieldColor(), 3, overlapWidth);
        } else {
            OrbShaderRenderer.drawCircularFill(graphics, x, y, size, esPercent, OrbColorConfig.getInstance().getShieldColor());
        }
    }

    /**
     * 中央揃えのテキストを描画（デフォルトでシャドウあり）
     */
    public static void renderCenteredScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float centerX, float centerY, float scale, int color) {
        renderCenteredScaledText(graphics, mc, text, centerX, centerY, scale, color, true);
    }

    /**
     * 中央揃えのテキストを描画（シャドウ指定可能）
     */
    public static void renderCenteredScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float centerX, float centerY, float scale, int color, boolean shadow) {
        int textWidth = HudFontHelper.getTextWidth(mc.font, text);

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, text, -textWidth / 2, -mc.font.lineHeight / 2, color, shadow);
        graphics.pose().popPose();
    }

    /**
     * 左揃えのテキストを描画（leftXがテキスト左端）
     */
    public static void renderLeftAlignedScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float leftX, float centerY, float scale, int color, boolean shadow) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, text, 0, -mc.font.lineHeight / 2, color, shadow);
        graphics.pose().popPose();
    }

    /**
     * 右揃えのテキストを描画（rightXがテキスト右端）
     */
    public static void renderRightAlignedScaledText(GuiGraphics graphics, Minecraft mc, String text,
            float rightX, float centerY, float scale, int color, boolean shadow) {
        int textWidth = HudFontHelper.getTextWidth(mc.font, text);

        graphics.pose().pushPose();
        graphics.pose().translate(rightX, centerY, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        HudFontHelper.drawString(graphics, mc.font, text, -textWidth, -mc.font.lineHeight / 2, color, shadow);
        graphics.pose().popPose();
    }
}
