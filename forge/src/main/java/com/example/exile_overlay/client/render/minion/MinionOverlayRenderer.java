package com.example.exile_overlay.client.render.minion;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.api.data.MinionDisplayInfo;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.HudFontHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 召喚ミニオンオーバーレイのレンダリングクラス
 * バフUIと統一されたフレーム形式で、全アクティブミニオンのアイコン・召喚数・残り時間を表示
 */
public class MinionOverlayRenderer implements IRenderCommand {

    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();

    // テクスチャリソース（バフUIと統一）
    private static final ResourceLocation EFFECT_FRAME = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame.png");
    private static final ResourceLocation EFFECT_FRAME_BACKGROUND = new ResourceLocation("exile_overlay",
            "textures/gui/effect_frame_background.png");
    private static final ResourceLocation EFFECT_STACK_BADGE = new ResourceLocation("exile_overlay",
            "textures/gui/effect_stack_badge.png");
    private static final ResourceLocation DEFAULT_MINION_ICON = new ResourceLocation("exile_overlay",
            "textures/gui/skill_slot_summon_badge.png");

    // フレームサイズ定数
    private static final int FRAME_WIDTH = 30;
    private static final int FRAME_HEIGHT = 39;
    private static final int ICON_SIZE = 22;

    // 配置間隔定数（縦並びは3px狭く配置）
    private static final int SPACING_HORIZONTAL = FRAME_WIDTH + 1;
    private static final int SPACING_VERTICAL = FRAME_HEIGHT - 2;

    // アニメーション設定
    private static final float ANIMATION_SPEED = 0.2f;
    private static final float FADE_IN_SPEED = 0.06f;
    private static final float SLIDE_DISTANCE = 30.0f;
    private static final float SLIDE_SPEED = 0.08f;

    private static final String CONFIG_KEY = "minion_overlay";

    public static class VisualState {
        public float currentX;
        public float currentY;
        public float alpha;
        public float offsetX;
        public int maxDuration;

        public VisualState(float startX, float startY) {
            this.currentX = startX;
            this.currentY = startY;
            this.alpha = 0.0f;
            this.offsetX = SLIDE_DISTANCE;
            this.maxDuration = -1;
        }
    }

    private static final Map<String, VisualState> displayStates = new HashMap<>();
    private static final Set<String> currentIdsCache = new HashSet<>(32);
    private static final List<MinionDisplayInfo> minionCache = new ArrayList<>(16);

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        List<MinionDisplayInfo> minions = MethodHandlesUtil.getActiveMinions(mc.player);
        minionCache.clear();
        minionCache.addAll(minions);

        if (!minionCache.isEmpty()) {
            int screenWidth = ctx.getScreenWidth();
            int screenHeight = ctx.getScreenHeight();
            HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
            int[] pos = position.resolve(screenWidth, screenHeight);

            float userScale = getScale();
            renderMinionList(graphics, mc, minionCache, pos[0], pos[1], position.isHorizontal(),
                    userScale, ctx.getPartialTick());
        }
    }

    private static void renderMinionList(GuiGraphics graphics, Minecraft mc,
                                         List<MinionDisplayInfo> minions,
                                         int listX, int listY, boolean horizontal,
                                         double scale, float partialTick) {
        int spacing = horizontal ? SPACING_HORIZONTAL : SPACING_VERTICAL;

        updateVisualStates(minions);

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(listX, listY, 0);
            graphics.pose().scale((float) scale, (float) scale, 1.0f);

            for (int i = 0; i < minions.size(); i++) {
                MinionDisplayInfo minion = minions.get(i);

                float targetX = horizontal ? i * spacing : 0;
                float targetY = horizontal ? 0 : i * spacing;

                VisualState state = getVisualState(minion.spellId(), targetX, targetY, minion.durationTicks());

                updateFadeIn(state);

                float currentPosX = updatePositionX(state, targetX);
                float currentPosY = updatePositionY(state, targetY);

                float renderX = horizontal ? currentPosX + state.offsetX : 0;
                float renderY = horizontal ? 0 : currentPosY;

                if (state.alpha < 0.01f)
                    continue;

                RenderSystem.setShaderColor(1f, 1f, 1f, state.alpha);
                try {
                    renderSingleMinion(graphics, mc, minion, (int) renderX, (int) renderY, state);
                } finally {
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                }
            }
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void renderSingleMinion(GuiGraphics graphics, Minecraft mc,
                                           MinionDisplayInfo minion,
                                           int x, int y, VisualState state) {
        RenderSystem.enableBlend();

        // 1. 背景描画
        RenderSystem.setShaderTexture(0, EFFECT_FRAME_BACKGROUND);
        graphics.blit(EFFECT_FRAME_BACKGROUND, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        // 2. ミニオン / 呪文アイコン描画
        int iconOffset = 3;
        int iconX = x + iconOffset;
        int iconY = y + iconOffset;
        ResourceLocation icon = minion.icon() != null ? minion.icon() : DEFAULT_MINION_ICON;

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(-1.0f, -1.0f, 0);
            RenderSystem.setShaderTexture(0, icon);
            graphics.blit(icon, iconX, iconY, ICON_SIZE + 4, ICON_SIZE + 4, 0, 0, 16, 16, 16, 16);
        } finally {
            graphics.pose().popPose();
        }

        // 3. 残り時間プログレスバー
        int barMaxWidth = 22;
        int barHeight = 3;
        int barX = x + 4;
        int barY = y + FRAME_HEIGHT - 6 - barHeight;
        int barColor = 0xFF4CAF50; // ミニオン用グリーン

        if (!minion.isInfinite()) {
            int currentDuration = minion.durationTicks();
            int maxDur = state.maxDuration;

            if (maxDur <= 0 || currentDuration > maxDur) {
                maxDur = currentDuration;
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

        // 4. 外枠フレーム
        RenderSystem.setShaderTexture(0, EFFECT_FRAME);
        graphics.blit(EFFECT_FRAME, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

        // 5. 右上スタックバッジ（召喚数）
        int count = minion.count();
        if (count > 0) {
            RenderSystem.setShaderTexture(0, EFFECT_STACK_BADGE);
            graphics.blit(EFFECT_STACK_BADGE, x, y, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

            String countText = String.valueOf(count);
            float stackScale = 0.7f;
            int stackTextWidth = HudFontHelper.getTextWidth(mc.font, countText);

            float badgeCenterX = x + FRAME_WIDTH - 5;
            float badgeCenterY = y + 7;
            float stackX = (badgeCenterX - stackTextWidth * stackScale / 2.0f) / stackScale;
            float stackY = (badgeCenterY - mc.font.lineHeight * stackScale / 2.0f) / stackScale;

            graphics.pose().pushPose();
            try {
                graphics.pose().scale(stackScale, stackScale, 1.0f);
                HudFontHelper.drawString(graphics, mc.font, countText, (int) stackX, (int) stackY, 0xFFFFFFFF, true);
            } finally {
                graphics.pose().popPose();
            }
        }

        // 6. 残り時間テキスト（下部）
        String durationText = minion.durationText();
        if (durationText != null && !durationText.isEmpty()) {
            float textScale = 0.5f;
            int textWidth = HudFontHelper.getTextWidth(mc.font, durationText);

            graphics.pose().pushPose();
            try {
                float textX = (x + (FRAME_WIDTH - textWidth * textScale) / 2) / textScale;
                float textY = (float) ((y + 29) + 0.4) / textScale;

                graphics.pose().scale(textScale, textScale, 1.0f);

                int textColor = minion.isInfinite() ? 0xFF88FF88 : 0xFFFFFFFF;
                HudFontHelper.drawString(graphics, mc.font, durationText, (int) textX, (int) textY, textColor, false);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private static VisualState getVisualState(String id, float targetX, float targetY, int duration) {
        VisualState state = displayStates.get(id);
        if (state == null) {
            state = new VisualState(targetX, targetY);
            state.maxDuration = duration;
            displayStates.put(id, state);
        } else if (duration > state.maxDuration) {
            state.maxDuration = duration;
        }
        return state;
    }

    private static void updateFadeIn(VisualState state) {
        if (state.alpha < 1.0f) {
            state.alpha = Math.min(1.0f, state.alpha + FADE_IN_SPEED);
        }
        if (state.offsetX > 0.5f) {
            state.offsetX += (0.0f - state.offsetX) * SLIDE_SPEED;
        } else {
            state.offsetX = 0.0f;
        }
    }

    private static void updateVisualStates(List<MinionDisplayInfo> currentMinions) {
        currentIdsCache.clear();
        for (MinionDisplayInfo minion : currentMinions) {
            currentIdsCache.add(minion.spellId());
        }
        displayStates.keySet().removeIf(id -> !currentIdsCache.contains(id));
    }

    private static float updatePositionX(VisualState state, float targetX) {
        float diff = targetX - state.currentX;
        if (Math.abs(diff) < 0.5f) {
            state.currentX = targetX;
        } else {
            state.currentX += diff * ANIMATION_SPEED;
        }
        return state.currentX;
    }

    private static float updatePositionY(VisualState state, float targetY) {
        float diff = targetY - state.currentY;
        if (Math.abs(diff) < 0.5f) {
            state.currentY = targetY;
        } else {
            state.currentY += diff * ANIMATION_SPEED;
        }
        return state.currentY;
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

    @Override
    public int getConfigWidth() {
        Minecraft mc = Minecraft.getInstance();
        int count = 2;
        if (mc.player != null) {
            List<MinionDisplayInfo> minions = MethodHandlesUtil.getActiveMinions(mc.player);
            if (!minions.isEmpty()) {
                count = minions.size();
            }
        }
        HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
        if (position.isHorizontal()) {
            return FRAME_WIDTH + (count - 1) * SPACING_HORIZONTAL;
        } else {
            return FRAME_WIDTH;
        }
    }

    @Override
    public int getConfigHeight() {
        Minecraft mc = Minecraft.getInstance();
        int count = 2;
        if (mc.player != null) {
            List<MinionDisplayInfo> minions = MethodHandlesUtil.getActiveMinions(mc.player);
            if (!minions.isEmpty()) {
                count = minions.size();
            }
        }
        HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
        if (position.isHorizontal()) {
            return FRAME_HEIGHT;
        } else {
            return FRAME_HEIGHT + (count - 1) * SPACING_VERTICAL;
        }
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }
}
