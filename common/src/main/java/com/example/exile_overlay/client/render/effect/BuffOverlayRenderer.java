package com.example.exile_overlay.client.render.effect;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * バフ/デバフオーバーレイのレンダリングクラス
 */
public class BuffOverlayRenderer {

    // テクスチャリソース
    private static final ResourceLocation EFFECT_FRAME = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame.png");
    private static final ResourceLocation EFFECT_FRAME_BACKGROUND = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame_background.png");

    // フレームサイズ定数
    private static final int FRAME_WIDTH = 30;
    private static final int FRAME_HEIGHT = 39;
    private static final int ICON_SIZE = 20;

    // 配置設定
    private static final int BUFFS_X = 10;
    private static final int BUFFS_Y = 10;
    private static final boolean HORIZONTAL = true;
    private static final double SCALE = 1.0;

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // バフとデバフを統合して取得
        List<EffectRenderHelper.DisplayableEffect> buffs = EffectRenderHelper.getUnifiedEffects(mc.player, true);
        List<EffectRenderHelper.DisplayableEffect> debuffs = EffectRenderHelper.getUnifiedEffects(mc.player, false);

        List<EffectRenderHelper.DisplayableEffect> allEffects = new ArrayList<>();
        allEffects.addAll(buffs);
        allEffects.addAll(debuffs);

        // 統合リストをソート（無限→長期間→短期間）
        allEffects.sort((a, b) -> {
            boolean aInfinite = a.isInfinite();
            boolean bInfinite = b.isInfinite();

            if (aInfinite && !bInfinite) return -1;
            if (!aInfinite && bInfinite) return 1;
            if (aInfinite && bInfinite) return 0;

            return Integer.compare(b.getDuration(), a.getDuration());
        });

        // レンダリング
        if (!allEffects.isEmpty()) {
            renderUnifiedEffectList(graphics, mc, allEffects, BUFFS_X, BUFFS_Y, HORIZONTAL, SCALE, partialTick);
        }
    }

    private static void renderUnifiedEffectList(GuiGraphics graphics, Minecraft mc,
                                                 List<EffectRenderHelper.DisplayableEffect> effects,
                                                 int listX, int listY, boolean horizontal,
                                                 double scale, float partialTick) {
        int spacing = horizontal ? (FRAME_WIDTH + 3) : (FRAME_HEIGHT + 3);

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(listX, listY, 0);
            graphics.pose().scale((float) scale, (float) scale, 1.0f);

            for (int i = 0; i < effects.size(); i++) {
                EffectRenderHelper.DisplayableEffect effect = effects.get(i);

                // ターゲット位置を計算
                float targetX = horizontal ? i * spacing : 0;
                float targetY = horizontal ? 0 : i * spacing;

                // アニメーション状態を取得
                EffectRenderHelper.VisualState state = EffectRenderHelper.getVisualState(effect.getId(),
                        horizontal ? targetX : targetY);

                // 位置を更新（Lerp）
                float currentPos = horizontal
                        ? EffectRenderHelper.updatePosition(state, targetX, partialTick)
                        : targetY;

                float renderX = horizontal ? currentPos : 0;
                float renderY = horizontal ? 0 : currentPos;

                if (!horizontal) {
                    renderY = i * spacing;
                }

                // 効果を描画
                renderSingleEffect(graphics, mc, effect, (int) renderX, (int) renderY, state);
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void renderSingleEffect(GuiGraphics graphics, Minecraft mc,
                                           EffectRenderHelper.DisplayableEffect effect,
                                           int x, int y, EffectRenderHelper.VisualState state) {
        // 1. 背景を描画
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, EFFECT_FRAME_BACKGROUND);
        graphics.blit(EFFECT_FRAME_BACKGROUND, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        // 2. アイコンを描画（中央揃え）
        int iconOffset = (FRAME_WIDTH - ICON_SIZE) / 2;
        int iconX = x + iconOffset;
        int iconY = y + iconOffset;
        effect.renderIcon(graphics, iconX, iconY, ICON_SIZE);

        // 3. フレームを描画
        RenderSystem.setShaderTexture(0, EFFECT_FRAME);
        graphics.blit(EFFECT_FRAME, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        // 4. 残り時間テキストを描画
        String durationText = effect.getDurationText();
        if (durationText != null && !durationText.isEmpty()) {
            float textScale = 0.5f;
            int textWidth = mc.font.width(durationText);

            graphics.pose().pushPose();
            try {
                float textX = (x + (FRAME_WIDTH - textWidth * textScale) / 2) / textScale;
                float textY = (y + 31) / textScale;

                graphics.pose().scale(textScale, textScale, 1.0f);
                graphics.pose().translate(0, 0, 201.0f);

                int textColor = effect.isInfinite() ? 0xFF88FF88 : 0xFFFFFFFF;
                graphics.drawString(mc.font, durationText, (int) textX, (int) textY, textColor, false);
            } finally {
                graphics.pose().popPose();
            }
        }
    }
}
