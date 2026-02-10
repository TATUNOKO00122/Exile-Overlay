package com.example.exile_overlay.client.render.effect;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * バフ/デバフオーバーレイのレンダリングクラス
 * IHudRendererを実装して設定画面に対応
 */
public class BuffOverlayRenderer implements IHudRenderer {

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
    private static final boolean HORIZONTAL = true;
    private static final double SCALE = 1.0;
    private static final String CONFIG_KEY = "buff_overlay";

    // キャッシュ
    private static final List<EffectRenderHelper.DisplayableEffect> effectCache = new ArrayList<>(32);
    private static int cachedScreenWidth = -1;
    private static int cachedScreenHeight = -1;
    private static int cachedX = 0;
    private static int cachedY = 0;
    private static long lastEffectChangeTime = 0;

    public static void render(GuiGraphics graphics, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // バフとデバフを統合して取得
        List<EffectRenderHelper.DisplayableEffect> buffs = EffectRenderHelper.getUnifiedEffects(mc.player, true);
        List<EffectRenderHelper.DisplayableEffect> debuffs = EffectRenderHelper.getUnifiedEffects(mc.player, false);

        // リストをクリアして再利用（アロケーション削減）
        effectCache.clear();
        effectCache.addAll(buffs);
        effectCache.addAll(debuffs);

        // 変更検出：リストの内容が変わったかチェック
        boolean hasChanged = hasEffectListChanged(effectCache);
        if (hasChanged) {
            lastEffectChangeTime = System.currentTimeMillis();
            // 変更があった場合のみソート
            effectCache.sort(EFFECT_COMPARATOR);
        }

        // レンダリング
        if (!effectCache.isEmpty()) {
            int[] pos = getCachedPosition(mc);
            renderUnifiedEffectList(graphics, mc, effectCache, pos[0], pos[1], HORIZONTAL, SCALE, partialTick);
        }
    }

    /**
     * エフェクトリストの変更検出
     */
    private static boolean hasEffectListChanged(List<EffectRenderHelper.DisplayableEffect> current) {
        // サイズが異なる場合は変更あり
        if (current.size() != effectCache.size()) {
            return true;
        }
        // 内容を比較（IDと持続時間で比較）
        for (int i = 0; i < current.size(); i++) {
            EffectRenderHelper.DisplayableEffect currentEffect = current.get(i);
            // 前回のキャッシュと比較
            if (i >= effectCache.size()) return true;
            EffectRenderHelper.DisplayableEffect cachedEffect = effectCache.get(i);
            if (!currentEffect.getId().equals(cachedEffect.getId()) ||
                currentEffect.getDuration() != cachedEffect.getDuration()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 位置をキャッシュして取得
     */
    private static int[] getCachedPosition(Minecraft mc) {
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 画面サイズまたは設定が変わった場合のみ再計算
        if (screenWidth != cachedScreenWidth ||
            screenHeight != cachedScreenHeight ||
            isPositionDirty()) {

            HudPosition position = HudPositionManager.getInstance().getPosition(CONFIG_KEY);
            int[] pos = position.resolve(screenWidth, screenHeight);
            cachedX = pos[0];
            cachedY = pos[1];
            cachedScreenWidth = screenWidth;
            cachedScreenHeight = screenHeight;
        }

        return new int[]{cachedX, cachedY};
    }

    /**
     * 位置が変更されたかチェック
     */
    private static boolean isPositionDirty() {
        // 最後の変更から一定時間経過したら再計算
        // またはHudPositionManagerに変更通知メカニズムを実装
        return false; // TODO: 実装
    }

    private static final java.util.Comparator<EffectRenderHelper.DisplayableEffect> EFFECT_COMPARATOR = (a, b) -> {
        boolean aInfinite = a.isInfinite();
        boolean bInfinite = b.isInfinite();

        if (aInfinite && !bInfinite) return -1;
        if (!aInfinite && bInfinite) return 1;
        if (aInfinite && bInfinite) return 0;

        return Integer.compare(b.getDuration(), a.getDuration());
    };

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

    // ========================================
    // IHudRenderer インターフェース実装
    // ========================================

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx.getPartialTick());
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
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
            CoordinateSystem.TOP_LEFT_BASED,  // 左上基準
            new Insets(0, 0, 0, 0),           // オフセットなし
            new Insets(0, 0, 0, 0)            // 拡張なし
        );
    }
}
