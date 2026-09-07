package com.example.exile_overlay.client.render.minion;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.api.data.MercenaryDisplayInfo;
import com.example.exile_overlay.api.data.MercenarySkillInfo;
import com.example.exile_overlay.api.data.MinionDisplayInfo;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
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
    private static final ResourceLocation DEFAULT_MINION_ICON = new ResourceLocation("mmorpg",
            "textures/gui/spells/icons/summon_zombie.png");
    private static final ResourceLocation DEFAULT_MERCENARY_ICON = new ResourceLocation("mmorpg",
            "textures/gui/mercenary/classes/fighter.png");
    private static final ResourceLocation DUMMY_SKILL_1_ICON = new ResourceLocation("mmorpg",
            "textures/gui/spells/icons/charge.png");
    private static final ResourceLocation DUMMY_SKILL_2_ICON = new ResourceLocation("mmorpg",
            "textures/gui/spells/icons/circle_of_healing.png");
    private static final ResourceLocation SKILL_SLOT_BASE = new ResourceLocation("exile_overlay",
            "textures/gui/skill_slot_base.png");
    private static final ResourceLocation SKILL_SLOT_BG = new ResourceLocation("exile_overlay",
            "textures/gui/skill_slot_background.png");
    private static final ResourceLocation MERCENARY_UI = new ResourceLocation("exile_overlay",
            "textures/gui/mercenary_ui.png");

    // フレームサイズ定数
    private static final int FRAME_WIDTH = 30;
    private static final int FRAME_HEIGHT = 39;
    private static final int ICON_SIZE = 22;

    // 配置間隔定数（縦並びは3px狭く配置）
    private static final int SPACING_HORIZONTAL = FRAME_WIDTH + 1;
    private static final int SPACING_VERTICAL = FRAME_HEIGHT - 2;

    // 傭兵フレーム定数 (mercenary_ui.png アトラス)
    private static final int MERC_BAR_OFFSET_X = 34;
    private static final float MERC_BAR_SCALE = 0.5f;
    private static final int MERC_UI_TEX_SIZE = 256;
    private static final int MERC_ICON_FRAME_U = 1;
    private static final int MERC_ICON_FRAME_V = 1;
    private static final int MERC_ICON_FRAME_SIZE = 32;

    private static final int MERC_BAR_FRAME_U = 1;
    private static final int MERC_BAR_FRAME_V = 36;
    private static final int MERC_BAR_FRAME_W = 195;
    private static final int MERC_BAR_FRAME_H = 13;
    private static final int MERC_BAR_INNER_X = 2;
    private static final int MERC_BAR_INNER_Y = 1;
    private static final int MERC_BAR_INNER_W = 191;
    private static final int MERC_BAR_INNER_H = 10;
    private static final int MERC_BAR_WIDTH = (int) Math.round(MERC_BAR_FRAME_W * MERC_BAR_SCALE);
    private static final int MERC_TOTAL_WIDTH = MERC_BAR_OFFSET_X + MERC_BAR_WIDTH;
    private static final int MERC_TOTAL_HEIGHT = 33;

    // 傭兵UI移動画面用ダミープレビュー
    private static final MercenaryDisplayInfo DUMMY_MERCENARY = new MercenaryDisplayInfo(
            "dummy",
            "Mercenary",
            DEFAULT_MERCENARY_ICON,
            1,
            100.0f,
            100.0f,
            20.0f,
            20.0f,
            List.of(
                    new MercenarySkillInfo("dummy_skill_1", DUMMY_SKILL_1_ICON, false, 0.0f, 0, 0),
                    new MercenarySkillInfo("dummy_skill_2", DUMMY_SKILL_2_ICON, true, 0.4f, 8, 20)
            )
    );

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

        public VisualState(float startX, float startY) {
            this.currentX = startX;
            this.currentY = startY;
            this.alpha = 0.0f;
            this.offsetX = SLIDE_DISTANCE;
        }
    }

    private static final Map<String, VisualState> displayStates = new HashMap<>();
    private static final Set<String> currentIdsCache = new HashSet<>(32);
    private static final List<MinionDisplayInfo> minionCache = new ArrayList<>(16);

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        if (!MethodHandlesUtil.isMercenarySupported()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MercenaryDisplayInfo merc = MethodHandlesUtil.getActiveMercenary(mc.player);
        if (merc == null && mc.screen instanceof DraggableHudConfigScreen) {
            merc = DUMMY_MERCENARY;
        }

        // 独立した召喚数表示（将来再利用する可能性があるためコメントアウトで無効化）
        /*
        List<MinionDisplayInfo> minions = MethodHandlesUtil.getActiveMinions(mc.player);
        minionCache.clear();
        if (minions != null) {
            minionCache.addAll(minions);
        }
        */

        if (merc != null /* || !minionCache.isEmpty() */) {
            int screenWidth = ctx.getScreenWidth();
            int screenHeight = ctx.getScreenHeight();
            HudPosition position = POSITION_MANAGER.getPosition(CONFIG_KEY);
            int[] pos = position.resolve(screenWidth, screenHeight);

            float userScale = getScale();
            renderCombinedOverlay(graphics, mc, merc, minionCache, pos[0], pos[1], position.isHorizontal(),
                    userScale, ctx.getPartialTick());
        }
    }

    private static void renderCombinedOverlay(GuiGraphics graphics, Minecraft mc,
                                             MercenaryDisplayInfo merc,
                                             List<MinionDisplayInfo> minions,
                                             int listX, int listY, boolean horizontal,
                                             double scale, float partialTick) {
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(listX, listY, 0);
            graphics.pose().scale((float) scale, (float) scale, 1.0f);

            /*
            int minionStartX = 0;
            int minionStartY = 0;
            */

            if (merc != null) {
                renderMercenaryFrame(graphics, mc, merc, 0, 0, horizontal);
                /*
                if (horizontal) {
                    minionStartX = MERC_BAR_OFFSET_X + MERC_BAR_WIDTH + 8;
                } else {
                    minionStartY = 37;
                }
                */
            }

            // 独立した召喚数表示（将来再利用する可能性があるためコメントアウトで無効化）
            /*
            if (!minions.isEmpty()) {
                renderMinionListInternal(graphics, mc, minions, minionStartX, minionStartY, horizontal);
            }
            */
        } finally {
            graphics.pose().popPose();
        }
    }

    private static void renderMercenaryFrame(GuiGraphics graphics, Minecraft mc,
                                            MercenaryDisplayInfo merc,
                                            int x, int y, boolean mirrored) {
        RenderSystem.enableBlend();

        int frameW = 32;
        int frameH = 32;
        int iconOffset = 2;
        int iconSize = 28;
        int drawY = y + 1;

        // 反転時は右側にアイコン、左側にバーを配置
        int iconFrameX = mirrored ? (x + MERC_BAR_WIDTH + 2) : x;
        int iconX = iconFrameX + iconOffset;
        int iconY = drawY + iconOffset;

        // 1. アイコン穴の背景
        graphics.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, 0xAA000000);

        // 2. 傭兵クラスアイコン
        ResourceLocation icon = merc.icon() != null ? merc.icon() : DEFAULT_MINION_ICON;
        int srcSize = icon.getPath().contains("summon_zombie") ? 16 : 36;

        RenderSystem.setShaderTexture(0, icon);
        graphics.blit(icon, iconX, iconY, iconSize, iconSize, 0, 0, srcSize, srcSize, srcSize, srcSize);

        // 3. アイコン枠 (mercenary_ui.png から描画)
        RenderSystem.setShaderTexture(0, MERCENARY_UI);
        graphics.blit(MERCENARY_UI, iconFrameX, drawY, frameW, frameH,
                (float) MERC_ICON_FRAME_U, (float) MERC_ICON_FRAME_V,
                MERC_ICON_FRAME_SIZE, MERC_ICON_FRAME_SIZE,
                MERC_UI_TEX_SIZE, MERC_UI_TEX_SIZE);

        // 4. HP / MS バー座標 & スキルアイコン座標設定
        boolean hasMS = merc.maxMagicShield() > 0;
        int barShift = hasMS ? 6 : 0;

        int barFrameX = mirrored ? x : (x + MERC_BAR_OFFSET_X);
        int hpBarFrameY = drawY + 22 - barShift;
        float msBarFrameY = hpBarFrameY + 7.5f;

        // 5. 装備スキルアイコン (HPバー枠の上側に配置、反転時はアイコンに近い右側から配置)
        if (merc.skills() != null && !merc.skills().isEmpty()) {
            int skillIconSize = 12;
            int skillY = hpBarFrameY - 2 - skillIconSize;

            long lastUpdateTime = MercenaryClientCache.getLastUpdatedTime();
            float elapsedSeconds = lastUpdateTime > 0 ? (System.currentTimeMillis() - lastUpdateTime) / 1000.0f : 0.0f;

            for (int i = 0; i < merc.skills().size(); i++) {
                MercenarySkillInfo skill = merc.skills().get(i);
                int skillX = mirrored
                        ? (barFrameX + MERC_BAR_WIDTH - 1 - skillIconSize - i * (skillIconSize + 2))
                        : (barFrameX + 1 + i * (skillIconSize + 2));

                // アイコン背景
                graphics.fill(skillX, skillY, skillX + skillIconSize, skillY + skillIconSize, 0xAA000000);

                // スキルアイコン
                ResourceLocation skillIcon = skill.icon() != null ? skill.icon() : DEFAULT_MINION_ICON;
                RenderSystem.setShaderTexture(0, skillIcon);
                graphics.blit(skillIcon, skillX, skillY, skillIconSize, skillIconSize, 0, 0, 16, 16, 16, 16);

                // クールダウンオーバーレイ（パケット受信からの経過時間を補間して滑らかに描画）
                if (skill.onCooldown()) {
                    float totalSeconds = skill.totalTicks() / 20.0f;
                    float remainingSeconds = (skill.remainingTicks() / 20.0f) - elapsedSeconds;
                    float cdPct;
                    if (totalSeconds > 0) {
                        cdPct = Math.max(0.0f, Math.min(1.0f, remainingSeconds / totalSeconds));
                    } else {
                        cdPct = Math.max(0.0f, Math.min(1.0f, skill.cooldownProgress()));
                    }
                    int cdH = (int) Math.ceil(skillIconSize * cdPct);
                    if (cdH > 0) {
                        graphics.fill(skillX, skillY, skillX + skillIconSize, skillY + cdH, 0xB0000000);
                    }
                }
            }
        }

        // 6. HP バー描画（上段）
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(barFrameX, hpBarFrameY, 0);
            graphics.pose().scale(MERC_BAR_SCALE, MERC_BAR_SCALE, 1.0f);

            // バー背景
            graphics.fill(MERC_BAR_INNER_X, MERC_BAR_INNER_Y,
                    MERC_BAR_INNER_X + MERC_BAR_INNER_W, MERC_BAR_INNER_Y + MERC_BAR_INNER_H,
                    0x80000000);

            // 現在HPバー（反転時はアイコンに近い右端起点で伸びる）
            float hpPct = merc.maxHealth() > 0 ? Math.max(0.0f, Math.min(1.0f, merc.health() / merc.maxHealth())) : 0.0f;
            int hpFillW = (int) (MERC_BAR_INNER_W * hpPct);
            if (hpFillW > 0) {
                int fillLeft = mirrored ? (MERC_BAR_INNER_X + MERC_BAR_INNER_W - hpFillW) : MERC_BAR_INNER_X;
                int fillRight = mirrored ? (MERC_BAR_INNER_X + MERC_BAR_INNER_W) : (MERC_BAR_INNER_X + hpFillW);
                graphics.fill(fillLeft, MERC_BAR_INNER_Y,
                        fillRight, MERC_BAR_INNER_Y + MERC_BAR_INNER_H,
                        0xFF43A047);
            }

            // HPBar枠テクスチャ描画
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            graphics.blit(MERCENARY_UI, 0, 0, MERC_BAR_FRAME_W, MERC_BAR_FRAME_H,
                    (float) MERC_BAR_FRAME_U, (float) MERC_BAR_FRAME_V,
                    MERC_BAR_FRAME_W, MERC_BAR_FRAME_H,
                    MERC_UI_TEX_SIZE, MERC_UI_TEX_SIZE);
        } finally {
            graphics.pose().popPose();
        }

        // 7. MS（Magic Shield）バー描画（下段、二段表示）
        if (hasMS) {
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(barFrameX, msBarFrameY, 0);
                graphics.pose().scale(MERC_BAR_SCALE, MERC_BAR_SCALE, 1.0f);

                // バー背景
                graphics.fill(MERC_BAR_INNER_X, MERC_BAR_INNER_Y,
                        MERC_BAR_INNER_X + MERC_BAR_INNER_W, MERC_BAR_INNER_Y + MERC_BAR_INNER_H,
                        0x80000000);

                // 現在MSバー（反転時はアイコンに近い右端起点で伸びる）
                float msPct = Math.max(0.0f, Math.min(1.0f, merc.magicShield() / merc.maxMagicShield()));
                int msFillW = (int) (MERC_BAR_INNER_W * msPct);
                if (msFillW > 0) {
                    int fillLeft = mirrored ? (MERC_BAR_INNER_X + MERC_BAR_INNER_W - msFillW) : MERC_BAR_INNER_X;
                    int fillRight = mirrored ? (MERC_BAR_INNER_X + MERC_BAR_INNER_W) : (MERC_BAR_INNER_X + msFillW);
                    graphics.fill(fillLeft, MERC_BAR_INNER_Y,
                            fillRight, MERC_BAR_INNER_Y + MERC_BAR_INNER_H,
                            0xFF00B0FF);
                }

                // MSBar枠テクスチャ描画
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                graphics.blit(MERCENARY_UI, 0, 0, MERC_BAR_FRAME_W, MERC_BAR_FRAME_H,
                    (float) MERC_BAR_FRAME_U, (float) MERC_BAR_FRAME_V,
                    MERC_BAR_FRAME_W, MERC_BAR_FRAME_H,
                    MERC_UI_TEX_SIZE, MERC_UI_TEX_SIZE);
            } finally {
                graphics.pose().popPose();
            }
        }

        // 8. HP数値テキスト（一旦無効化）
        /*
        if (merc.maxHealth() > 0) {
            int curHp = (int) Math.ceil(merc.health());
            int maxHp = (int) Math.ceil(merc.maxHealth());
            String hpText;
            if (hasMS && merc.magicShield() > 0) {
                int curMs = (int) Math.ceil(merc.magicShield());
                hpText = curHp + " (+" + curMs + ") / " + maxHp;
            } else {
                hpText = curHp + " / " + maxHp;
            }

            float hpScale = 0.7f;
            float textW = HudFontHelper.getTextWidth(mc.font, hpText) * hpScale;
            float textX = barFrameX + MERC_BAR_WIDTH - textW - 2;
            float textY = hpBarFrameY - mc.font.lineHeight * hpScale - 1.0f;

            graphics.pose().pushPose();
            graphics.pose().translate(textX, textY, 0);
            graphics.pose().scale(hpScale, hpScale, 1.0f);
            HudFontHelper.drawString(graphics, mc.font, hpText, 0, 0, 0xFFFFFFFF, true);
            graphics.pose().popPose();
        }
        */
    }

    private static void renderMinionListInternal(GuiGraphics graphics, Minecraft mc,
                                                 List<MinionDisplayInfo> minions,
                                                 int startX, int startY, boolean horizontal) {
        int spacing = horizontal ? SPACING_HORIZONTAL : SPACING_VERTICAL;
        updateVisualStates(minions);

        for (int i = 0; i < minions.size(); i++) {
            MinionDisplayInfo minion = minions.get(i);

            float targetX = horizontal ? startX + i * spacing : startX;
            float targetY = horizontal ? startY : startY + i * spacing;

            VisualState state = getVisualState(minion.spellId(), targetX, targetY);
            updateFadeIn(state);

            float currentPosX = updatePositionX(state, targetX);
            float currentPosY = updatePositionY(state, targetY);

            float renderX = horizontal ? currentPosX + state.offsetX : startX;
            float renderY = horizontal ? startY : currentPosY;

            if (state.alpha < 0.01f)
                continue;

            RenderSystem.setShaderColor(1f, 1f, 1f, state.alpha);
            try {
                renderSingleMinion(graphics, mc, minion, (int) renderX, (int) renderY, state);
            } finally {
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            }
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

        // 3. HPプログレスバー（最低HPミニオンの残量を表示）
        int barMaxWidth = 22;
        int barHeight = 3;
        int barX = x + 4;
        int barY = y + FRAME_HEIGHT - 6 - barHeight;
        int barColor = 0xFF4CAF50; // ミニオン用グリーン固定

        float hpRatio = Math.max(0.0f, Math.min(1.0f, minion.healthRatio()));
        int barWidth = (int) (barMaxWidth * hpRatio);

        graphics.fill(barX, barY, barX + barMaxWidth, barY + barHeight, 0x80000000);
        if (barWidth > 0) {
            graphics.fill(barX, barY, barX + barWidth, barY + barHeight, barColor);
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

        // 6. 残り時間テキスト（下部、空文字・∞の場合は非表示）
        String durationText = minion.durationText();
        if (durationText != null && !durationText.isEmpty()) {
            boolean colonFormat = EquipmentDisplayConfig.getInstance().isBuffDurationColonFormat();
            float textScale = colonFormat ? 0.6f : 0.5f;
            int textWidth = HudFontHelper.getTextWidth(mc.font, durationText);

            graphics.pose().pushPose();
            try {
                float textX = colonFormat
                        ? (x + (FRAME_WIDTH - textWidth * textScale) / 2 + 0.5f) / textScale
                        : (x + (FRAME_WIDTH - textWidth * textScale) / 2 + 1.0f) / textScale;
                float textY = colonFormat
                        ? (float) (y + 29) / textScale
                        : (float) ((y + 29) + 0.4) / textScale;

                graphics.pose().scale(textScale, textScale, 1.0f);
                HudFontHelper.drawString(graphics, mc.font, durationText, (int) textX, (int) textY, 0xFFFFFFFF, false);
            } finally {
                graphics.pose().popPose();
            }
        }
    }

    private static VisualState getVisualState(String id, float targetX, float targetY) {
        VisualState state = displayStates.get(id);
        if (state == null) {
            state = new VisualState(targetX, targetY);
            displayStates.put(id, state);
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
        return MERC_TOTAL_WIDTH;
    }

    @Override
    public int getHeight() {
        return MERC_TOTAL_HEIGHT;
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
        return MERC_TOTAL_WIDTH;
        /*
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
        */
    }

    @Override
    public int getConfigHeight() {
        return MERC_TOTAL_HEIGHT;
        /*
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
        */
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.TOP_LEFT_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(2, 2, 2, 2)
        );
    }
}
