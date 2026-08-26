package com.example.exile_overlay.client.render.effect;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudFontHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * バフ/デバフオーバーレイのレンダリングクラス
 */
public class BuffOverlayRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BuffOverlayRenderer.class);
    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();

    // テクスチャリソース
    private static final ResourceLocation EFFECT_FRAME = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame.png");
    private static final ResourceLocation EFFECT_FRAME_BACKGROUND = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame_background.png");
    private static final ResourceLocation EFFECT_STACK_BADGE = new ResourceLocation("exile_overlay",
            "textures/gui/effect_stack_badge.png");

    // フレームサイズ定数
    private static final int FRAME_WIDTH = 30;
    private static final int FRAME_HEIGHT = 39;
    private static final int ICON_SIZE = 22;

    // 配置設定
    private static final boolean HORIZONTAL = true;
    private static final double SCALE = 1.0;
    private static final String CONFIG_KEY = "buff_overlay";

    private static final String[] ROMAN_THOUSANDS = {"", "M", "MM", "MMM"};
    private static final String[] ROMAN_HUNDREDS = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
    private static final String[] ROMAN_TENS = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
    private static final String[] ROMAN_ONES = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

    // 位置変更リスナーを登録（設定画面での変更を検知）
    static {
        POSITION_MANAGER.addListener(CONFIG_KEY, (key, newPosition) -> {
            positionDirty = true;
        });
    }

    private static final List<EffectRenderHelper.DisplayableEffect> effectCache = new ArrayList<>(32);
    private static boolean positionDirty = true;

    private static long lastLogTime = 0;

    private static void renderUnifiedEffectList(GuiGraphics graphics, Minecraft mc,
            List<EffectRenderHelper.DisplayableEffect> effects,
            int listX, int listY, boolean horizontal,
            double scale, float partialTick) {
        int spacing = horizontal ? (FRAME_WIDTH + 1) : (FRAME_HEIGHT + 1);

        EffectRenderHelper.updateVisualStates(CONFIG_KEY, effects);

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(listX, listY, 0);
            graphics.pose().scale((float) scale, (float) scale, 1.0f);

            for (int i = 0; i < effects.size(); i++) {
                EffectRenderHelper.DisplayableEffect effect = effects.get(i);

                float targetX = horizontal ? i * spacing : 0;
                float targetY = horizontal ? 0 : i * spacing;

                EffectRenderHelper.VisualState state = EffectRenderHelper.getVisualState(CONFIG_KEY,
                        effect.getId(), horizontal ? targetX : targetY, effect.getMaxDuration());

                EffectRenderHelper.updateFadeIn(state);

                float currentPos = horizontal
                        ? EffectRenderHelper.updatePosition(state, targetX, partialTick)
                        : targetY;

                float renderX = horizontal ? currentPos + state.offsetX : 0;
                float renderY = horizontal ? 0 : currentPos;

                if (!horizontal) {
                    renderY = i * spacing;
                }

                if (state.alpha < 0.01f)
                    continue;

                RenderSystem.setShaderColor(1f, 1f, 1f, state.alpha);
                try {
                    renderSingleEffect(graphics, mc, effect, (int) renderX, (int) renderY, state);
                } finally {
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void renderSingleEffect(GuiGraphics graphics, Minecraft mc,
            EffectRenderHelper.DisplayableEffect effect,
            int x, int y, EffectRenderHelper.VisualState state) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, EFFECT_FRAME_BACKGROUND);
        graphics.blit(EFFECT_FRAME_BACKGROUND, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        int iconOffset = 3;
        int iconX = x + iconOffset;
        int iconY = y + iconOffset;
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(-1.0f, -1.0f, 0);
            effect.renderIcon(graphics, iconX, iconY, ICON_SIZE + 4);
        } finally {
            graphics.pose().popPose();
        }

        int barMaxWidth = 22;
        int barHeight = 3;
        int barX = x + 4;
        int barY = y + FRAME_HEIGHT - 6 - barHeight;
        int barColor = effect.isBeneficial() ? 0xFF4CAF50 : 0xFFF44336;

        if (!effect.isInfinite()) {
            int currentDuration = effect.getDuration();
            int maxDur = state.maxDuration;
            int effectMax = effect.getMaxDuration();

            if (maxDur <= 0 || effectMax > maxDur) {
                maxDur = effectMax;
                state.maxDuration = maxDur;
            }

            float progress = maxDur > 0 ? (float) currentDuration / maxDur : 1.0f;
            progress = Math.max(0.0f, Math.min(1.0f, progress));
            int barWidth = (int) (barMaxWidth * progress);

            graphics.fill(barX, barY, barX + barMaxWidth, barY + barHeight, 0x80000000);
            if (barWidth > 0) {
                graphics.fill(barX, barY, barX + barWidth, barY + barHeight, barColor);
            }
        } else {
            graphics.fill(barX, barY, barX + barMaxWidth, barY + barHeight, barColor);
        }

        RenderSystem.setShaderTexture(0, EFFECT_FRAME);
        graphics.blit(EFFECT_FRAME, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        int stacks = effect.getStacks();
        if (stacks > 1) {
            boolean isSimple = EquipmentDisplayConfig.getInstance().isSimpleBuffStackDisplay();
            String stackText = toRoman(stacks);
            float stackScale = isSimple ? 0.9f : 0.7f;
            int stackTextWidth = HudFontHelper.getTextWidth(mc.font, stackText);

            float badgeCenterX = x + FRAME_WIDTH - 5;
            float badgeCenterY = y + 7;

            if (isSimple) {
                float textX = badgeCenterX - (stackTextWidth * stackScale) / 2.0f;
                float textY = badgeCenterY - (mc.font.lineHeight * stackScale) / 2.0f;

                graphics.pose().pushPose();
                graphics.pose().translate(textX, textY, 0);
                graphics.pose().scale(stackScale, stackScale, 1.0f);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            HudFontHelper.drawString(graphics, mc.font, stackText, dx, dy, 0xFF000000, false);
                        }
                    }
                }
                HudFontHelper.drawString(graphics, mc.font, stackText, 0, 0, 0xFFFFFFFF, false);
                graphics.pose().popPose();
            } else {
                RenderSystem.setShaderTexture(0, EFFECT_STACK_BADGE);
                graphics.blit(EFFECT_STACK_BADGE, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

                float stackX = (badgeCenterX - stackTextWidth * stackScale / 2.0f) / stackScale;
                float stackY = (badgeCenterY - mc.font.lineHeight * stackScale / 2.0f) / stackScale;

                graphics.pose().pushPose();
                try {
                    graphics.pose().scale(stackScale, stackScale, 1.0f);
                    HudFontHelper.drawString(graphics, mc.font, stackText, (int) stackX, (int) stackY, 0xFFFFFFFF, true);
                } finally {
                    graphics.pose().popPose();
                }
            }
        }

        String durationText = effect.getDurationText();
        if (durationText != null && !durationText.isEmpty()) {
            float textScale = 0.5f;
            int textWidth = HudFontHelper.getTextWidth(mc.font, durationText);

            graphics.pose().pushPose();
            try {
                float textX = (x + (FRAME_WIDTH - textWidth * textScale) / 2) / textScale;
                float textY = (float) ((y + 29) + 0.4) / textScale;

                graphics.pose().scale(textScale, textScale, 1.0f);

                int textColor = effect.isInfinite() ? 0xFF88FF88 : 0xFFFFFFFF;
                HudFontHelper.drawString(graphics, mc.font, durationText, (int) textX, (int) textY, textColor, false);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<EffectRenderHelper.DisplayableEffect> effects =
                EffectRenderHelper.getFilteredEffects(mc.player, CONFIG_KEY);

        effectCache.clear();
        effectCache.addAll(effects);

        if (!effectCache.isEmpty()) {
            int screenWidth = ctx.getScreenWidth();
            int screenHeight = ctx.getScreenHeight();
            HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
            int[] pos = position.resolve(screenWidth, screenHeight);

            long now = System.currentTimeMillis();
            if (now - lastLogTime > 2000) {
                LOGGER.debug("[BUFF] pos: ({}, {}), ctx: {}x{}, effects: {}",
                        pos[0], pos[1], screenWidth, screenHeight, effectCache.size());
                lastLogTime = now;
            }
            float userScale = getScale();
            renderUnifiedEffectList(graphics, mc, effectCache, pos[0], pos[1], HORIZONTAL, SCALE * userScale,
                    ctx.getPartialTick());
        }
    }

    @Override
    public String getId() {
        return CONFIG_KEY;
    }

    @Override
    public int getWidth() {
        return FRAME_WIDTH;
    }

    @Override
    public int getHeight() {
        return FRAME_HEIGHT;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.FILL;
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    /**
     * 設定画面用の幅を取得
     * 現在のバフ数に応じた動的サイズを返す
     */
    @Override
    public int getConfigWidth() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return FRAME_WIDTH * 3 + 2;
        }
        List<EffectRenderHelper.DisplayableEffect> effects =
                EffectRenderHelper.getFilteredEffects(mc.player, CONFIG_KEY);
        int count = effects.size();

        if (count <= 0) {
            return FRAME_WIDTH * 3 + 2;
        }
        return FRAME_WIDTH * count + (count - 1);
    }

    /**
     * 設定画面用の高さを取得
     */
    @Override
    public int getConfigHeight() {
        return FRAME_HEIGHT;
    }

    private static String toRoman(int num) {
        if (num <= 0 || num >= 4000)
            return String.valueOf(num);
        return ROMAN_THOUSANDS[num / 1000] + ROMAN_HUNDREDS[(num % 1000) / 100] + ROMAN_TENS[(num % 100) / 10] + ROMAN_ONES[num % 10];
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED, // 左上基準
                new Insets(0, 0, 0, 0), // オフセットなし
                new Insets(0, 0, 0, 0) // 拡張なし
        );
    }
}
