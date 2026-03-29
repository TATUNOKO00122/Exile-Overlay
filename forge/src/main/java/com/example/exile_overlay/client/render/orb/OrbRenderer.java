package com.example.exile_overlay.client.render.orb;

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
 */
public class OrbRenderer {

    private static final ResourceLocation REFLECTION_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/orb_reflection.png");

    // ES（エナジーシールド）の色設定（HJUD Mod方式）
    // ARGB形式: 0x66 = 0.4 * 255 (40% alpha) - PoE等ハクスラ標準の半透明度
    private static final int ES_COLOR = 0x6600E6FF;

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

        // ORB_1（HPオーブ）の場合、ESオーバーレイを描画（HJUD Mod方式）
        if ("orb_1".equals(config.getId())) {
            renderEsOverlay(graphics, orbX, orbY, orbSize, player);
        }

        if (config.hasOverlayColor() && config.getOverlayProvider() != null) {
            renderOverlayFillLayer(graphics, config, player);
        }

        if (config.shouldShowReflection()) {
            RenderSystem.enableBlend();
            // 反射テクスチャを2px拡大、左に2pxずらす、上に2px上げる
            graphics.blit(REFLECTION_TEXTURE, orbX - 2, orbY - 2, 0, 0, 0, orbSize + 2, orbSize + 2, orbSize + 2, orbSize + 2);
        }
    }

    public static void renderOverlayLayer(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc) {
        if (shouldSkipRender(config, player))
            return;

        int orbX = config.getCenterX();
        int orbY = config.getCenterY();
        int orbSize = config.getSize();
        float centerX = orbX + orbSize / 2f;

        // ORB_1（HPオーブ）の場合、HPとESの両方を表示
        if ("orb_1".equals(config.getId())) {
            renderHpEsValues(graphics, config, player, mc, orbX, orbY, orbSize, centerX);
        } else if (config.getDataProvider().shouldShowValue()) {
            // 他のオーブは今まで通り
            float current = config.getDataProvider().getCurrentValue(player);
            float max = config.getDataProvider().getMaxValue(player);
            String text = (int) current + " / " + (int) max;
            float textScale = config.getDataProvider().getTextScale();
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, textScale, 0xFFFFFFFF);
        }
    }

    /**
     * HPとESの数値を表示（多い方を上、少ない方を下に小さく）
     * - ラベルなし（HP/ES文字なし）
     * - 白文字統一
     * - 上下隙間最小限
     */
    private static void renderHpEsValues(GuiGraphics graphics, OrbConfig config, Player player, Minecraft mc,
            int orbX, int orbY, int orbSize, float centerX) {
        // HP値はデータプロバイダーから取得（液面と同じ値）
        float hpCurrent = config.getDataProvider().getCurrentValue(player);
        float hpMax = config.getDataProvider().getMaxValue(player);
        // ES値はMineAndSlashHelperから取得
        float esCurrent = MineAndSlashHelper.getCurrentMagicShield(player);
        float esMax = MineAndSlashHelper.getMaxMagicShield(player);

        int whiteColor = 0xFFFFFFFF; // 白文字統一

        // ESがない場合は従来通り中央にHPのみ表示
        if (esMax <= 0) {
            String text = (int) hpCurrent + "/" + (int) hpMax;
            renderCenteredScaledText(graphics, mc, text, centerX, orbY + orbSize / 2f, 0.75f, whiteColor);
            return;
        }

        // ESがある場合：多い方を上、少ない方を下に表示
        boolean hpIsLarger = hpMax >= esMax;
        float largerCurrent = hpIsLarger ? hpCurrent : esCurrent;
        float largerMax = hpIsLarger ? hpMax : esMax;
        float smallerCurrent = hpIsLarger ? esCurrent : hpCurrent;
        float smallerMax = hpIsLarger ? esMax : hpMax;

        // 上：多い方（通常サイズ）- 中心より少し上
        String largerText = (int) largerCurrent + "/" + (int) largerMax;
        float mainScale = 0.75f;
        renderCenteredScaledText(graphics, mc, largerText, centerX, orbY + orbSize / 2f - 4, mainScale, whiteColor);

        // 下：少ない方（少し小さいサイズ）- すぐ下に（隙間最小）
        String smallerText = (int) smallerCurrent + "/" + (int) smallerMax;
        float subScale = 0.55f;
        renderCenteredScaledText(graphics, mc, smallerText, centerX, orbY + orbSize / 2f + 5, subScale, whiteColor);
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
     * HPオーブ上にシアン色の半透明レイヤーを下から上に描画
     * OrbShaderRendererを使用して位置ズレを防止
     *
     * @param graphics GUIグラフィックス
     * @param x        オーブ左上X座標
     * @param y        オーブ左上Y座標
     * @param size     オーブサイズ
     * @param player   プレイヤー
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
