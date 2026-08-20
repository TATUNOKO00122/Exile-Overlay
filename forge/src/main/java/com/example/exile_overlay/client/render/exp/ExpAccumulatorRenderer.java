package com.example.exile_overlay.client.render.exp;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * 累積獲得EXPポップアップ HUD レンダラー
 *
 * - 戦闘EXP獲得時: "+XXX EXP (YY.Y%)" を緑色太字でポップアップ
 * - 生活職EXP獲得時: "+XXX [職業名] EXP (YY.Y%)" を黄色太字でポップアップ
 * - 同時発生時: 上段に戦闘EXP、下段に生活職EXPの2段スタック表示
 * - 各行ごとに独立したポップ拡大演出とフェードアウト
 * - HUD編集画面でのプレビュー表示＆ドラッグ移動・スケール調整対応
 */
public class ExpAccumulatorRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "exp_accumulator";
    private static final int PRIORITY = 85;
    private static final int LINE_SPACING = 2;

    private static final int COMBAT_COLOR_RGB = 0x55FF55;
    private static final int PROF_COLOR_RGB = 0xFFFF55;

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.OVERLAY;
    }

    @Override
    public String getConfigKey() {
        return COMMAND_ID;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        return mc.font.width("+999 Gear Crafting Exp (75.5%)");
    }

    @Override
    public int getHeight() {
        Minecraft mc = Minecraft.getInstance();
        return (mc.font.lineHeight * 2) + LINE_SPACING;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DraggableHudConfigScreen) {
            return getPosition().isVisible();
        }
        ExpAccumulatorManager manager = ExpAccumulatorManager.getInstance();
        boolean hasCombat = manager.isCombatDisplaying() && manager.getCombatAccumulatedExp() > 0;
        boolean hasProf = manager.isProfDisplaying() && manager.getProfAccumulatedExp() > 0;
        return getPosition().isVisible() && (hasCombat || hasProf);
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        boolean isEditing = mc.screen instanceof DraggableHudConfigScreen;
        ExpAccumulatorManager manager = ExpAccumulatorManager.getInstance();

        boolean showCombat = isEditing || (manager.isCombatDisplaying() && manager.getCombatAccumulatedExp() > 0);
        boolean showProf = isEditing || (manager.isProfDisplaying() && manager.getProfAccumulatedExp() > 0);

        if (!showCombat && !showProf) {
            return;
        }

        HudPosition position = getPosition();
        int[] pos = position.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());
        float baseScale = position.getScale();
        int lineHeight = mc.font.lineHeight;

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        try {
            // 基準位置へ移動
            poseStack.translate(pos[0], pos[1], 0);
            poseStack.scale(baseScale, baseScale, 1.0f);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            int currentY = 0;

            // 1. 戦闘EXPの描画 (緑色)
            if (showCombat) {
                int expVal = isEditing ? 999 : manager.getCombatAccumulatedExp();
                float percentage = isEditing ? 75.5f : manager.getCombatProgressPercentage();
                float alpha = isEditing ? 1.0f : manager.getCombatAlpha();
                float animScale = isEditing ? 1.0f : manager.getCombatScaleMultiplier();

                if (expVal > 0 && alpha > 0.001f) {
                    String combatStr = String.format(Locale.ROOT, "+%d Exp (%.1f%%)", expVal, percentage);
                    Component combatText = Component.literal(combatStr)
                            .withStyle(ChatFormatting.GREEN)
                            .withStyle(ChatFormatting.BOLD);

                    renderExpLine(graphics, poseStack, mc, combatText, COMBAT_COLOR_RGB, alpha, animScale, currentY, lineHeight);
                    currentY += lineHeight + LINE_SPACING;
                }
            }

            // 2. 生活職EXPの描画 (黄色)
            if (showProf) {
                int expVal = isEditing ? 50 : manager.getProfAccumulatedExp();
                float percentage = isEditing ? 12.4f : manager.getProfProgressPercentage();
                float alpha = isEditing ? 1.0f : manager.getProfAlpha();
                float animScale = isEditing ? 1.0f : manager.getProfScaleMultiplier();

                if (expVal > 0 && alpha > 0.001f) {
                    Component profNameComp = isEditing ? Component.literal("Salvaging") : manager.getActiveProfDisplayName();
                    String profName = profNameComp.getString();

                    String profStr = String.format(Locale.ROOT, "+%d %s Exp (%.1f%%)", expVal, profName, percentage);
                    Component profText = Component.literal(profStr)
                            .withStyle(ChatFormatting.YELLOW)
                            .withStyle(ChatFormatting.BOLD);

                    renderExpLine(graphics, poseStack, mc, profText, PROF_COLOR_RGB, alpha, animScale, currentY, lineHeight);
                }
            }

            RenderSystem.disableBlend();
        } finally {
            poseStack.popPose();
        }
    }

    private void renderExpLine(GuiGraphics graphics, PoseStack poseStack, Minecraft mc, Component text,
                               int rgbColor, float alpha, float animScale, int y, int lineHeight) {
        int textWidth = mc.font.width(text);
        int textAlpha = (int) (alpha * 255);
        int textColor = (textAlpha << 24) | rgbColor;

        poseStack.pushPose();
        if (animScale != 1.0f) {
            poseStack.translate(textWidth / 2.0f, y + (lineHeight / 2.0f), 0);
            poseStack.scale(animScale, animScale, 1.0f);
            poseStack.translate(-textWidth / 2.0f, -(y + (lineHeight / 2.0f)), 0);
        }
        graphics.drawString(mc.font, text, 0, y, textColor, true);
        poseStack.popPose();
    }
}
