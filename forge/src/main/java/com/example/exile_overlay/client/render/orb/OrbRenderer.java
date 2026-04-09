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
 * 【POE2スタイルテキスト表示】
 * OrbTextConfig.poe2StyleText=true時:
 * - 左Orb上: ES → Life → Energy を縦に並べて表示
 * - 右Orb上: Mana単体を表示
 * - Orb内部にはテキストを描画しない
 */
public class OrbRenderer {

    private static final ResourceLocation REFLECTION_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/orb_reflection.png");

    // ES（エナジーシールド）の色設定（HJUD Mod方式）
    private static final int ES_COLOR = 0x6600E6FF;

    // POE2スタイル表示色
    private static final int ES_TEXT_COLOR = 0xFF00E6FF;
    private static final int HP_TEXT_COLOR = 0xFFFFFFFF;
    private static final int ENERGY_TEXT_COLOR = 0xFFFFFF00;
    private static final int MANA_TEXT_COLOR = 0xFF5555FF;

    // POE2スタイル表示スケール
    private static final float POE2_TEXT_SCALE = 0.75f;
    private static final int POE2_LINE_HEIGHT = 12;
    private static final int POE2_TEXT_OFFSET_Y = -20;

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
        if (textConfig.isPoe2StyleText()) {
            return;
        }

        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        float centerX = orbX + orbSize / 2f;

        // ORB_1（HPオーブ）の場合、HPとESの両方を表示
        if ("orb_1".equals(config.getId())) {
            renderHpEsValues(graphics, config, player, mc, orbX, orbY, orbSize, centerX);
        } else if (config.getDataProvider().shouldShowValue()) {
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);
            String text = (int) current + " / " + (int) max;
            float textScale = config.getDataProvider().getTextScale();
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, textScale, 0xFFFFFFFF);
        }
    }

    /**
     * POE2スタイル: 左Orb（ORB_1）上にES/Life/Energyを縦に並べて描画
     * HotbarRenderCommandのLayer 4から呼び出される
     *
     * データ取得はResourceSlotManager経由で統一（既存パイプラインと同一）
     */
    public static void renderPoe2LeftOrbText(GuiGraphics graphics, Minecraft mc, Player player,
            int orb1CenterX, int orb1TopY) {
        ResourceSlotManager rsm = ResourceSlotManager.getInstance();

        float esCurrent = rsm.getCurrentValue("orb1_overlay", player);
        float esMax = rsm.getMaxValue("orb1_overlay", player);
        float hpCurrent = rsm.getCurrentValue("orb1", player);
        float hpMax = rsm.getMaxValue("orb1", player);
        float energyCurrent = rsm.getCurrentValue("orb3", player);
        float energyMax = rsm.getMaxValue("orb3", player);

        float centerX = orb1CenterX;
        float startY = orb1TopY + POE2_TEXT_OFFSET_Y;

        int lineCount = 0;
        if (esMax > 0) lineCount++;
        lineCount++;
        if (energyMax > 0) lineCount++;

        float totalHeight = (lineCount - 1) * POE2_LINE_HEIGHT;
        float currentY = startY - totalHeight;

        if (esMax > 0) {
            String esText = (int) esCurrent + "/" + (int) esMax;
            renderCenteredScaledText(graphics, mc, esText, centerX, currentY, POE2_TEXT_SCALE, ES_TEXT_COLOR);
            currentY += POE2_LINE_HEIGHT;
        }

        String hpText = (int) hpCurrent + "/" + (int) hpMax;
        renderCenteredScaledText(graphics, mc, hpText, centerX, currentY, POE2_TEXT_SCALE, HP_TEXT_COLOR);
        currentY += POE2_LINE_HEIGHT;

        if (energyMax > 0) {
            String energyText = (int) energyCurrent + "/" + (int) energyMax;
            renderCenteredScaledText(graphics, mc, energyText, centerX, currentY, POE2_TEXT_SCALE, ENERGY_TEXT_COLOR);
        }
    }

    /**
     * POE2スタイル: 右Orb（ORB_2）上にManaを表示
     */
    public static void renderPoe2RightOrbText(GuiGraphics graphics, Minecraft mc, Player player,
            int orb2CenterX, int orb2TopY) {
        ResourceSlotManager rsm = ResourceSlotManager.getInstance();
        float manaCurrent = rsm.getCurrentValue("orb2", player);
        float manaMax = rsm.getMaxValue("orb2", player);

        if (manaMax <= 0) return;

        String text = (int) manaCurrent + "/" + (int) manaMax;
        renderCenteredScaledText(graphics, mc, text, orb2CenterX, orb2TopY + POE2_TEXT_OFFSET_Y,
                POE2_TEXT_SCALE, MANA_TEXT_COLOR);
    }

    /**
     * HPとESの数値を表示（多い方を上、少ない方を下に小さく）
     * - ラベルなし（HP/ES文字なし）
     * - 白文字統一
     * - 上下隙間最小限
     */
    private static void renderHpEsValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, float centerX) {
        float hpCurrent = config.getDataProvider().getCurrentValue(player);
        float hpMax = config.getDataProvider().getMaxValue(player);
        float esCurrent = MineAndSlashHelper.getCurrentMagicShield(player);
        float esMax = MineAndSlashHelper.getMaxMagicShield(player);

        int whiteColor = 0xFFFFFFFF;

        if (esMax <= 0) {
            String text = (int) hpCurrent + "/" + (int) hpMax;
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, 0.75f, whiteColor);
            return;
        }

        boolean hpIsLarger = hpMax >= esMax;
        float largerCurrent = hpIsLarger ? hpCurrent : esCurrent;
        float largerMax = hpIsLarger ? hpMax : esMax;
        float smallerCurrent = hpIsLarger ? esCurrent : hpCurrent;
        float smallerMax = hpIsLarger ? esMax : hpMax;

        String largerText = (int) largerCurrent + "/" + (int) largerMax;
        renderCenteredScaledText(graphics, mc, largerText, centerX, orbY + orbSize / 2f - 4, 0.75f, whiteColor);

        String smallerText = (int) smallerCurrent + "/" + (int) smallerMax;
        renderCenteredScaledText(graphics, mc, smallerText, centerX, orbY + orbSize / 2f + 5, 0.55f, whiteColor);
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
