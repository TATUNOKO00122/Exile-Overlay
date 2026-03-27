package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.ThrottledDataSource;
import com.example.exile_overlay.client.render.orb.OrbRenderer;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.example.exile_overlay.client.render.orb.OrbType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.List;

import static com.example.exile_overlay.client.render.orb.OrbType.ORB_3;

/**
 * リファクタリング済みホットバーレンダラー
 * 
 * 【改善点】
 * - IHudRendererインターフェース実装による依存性注入対応
 * - RenderStateによるリスト再利用（GCプレッシャー軽減）
 * - ThrottledDataSourceによる間引き（更新頻度の低いデータを最適化）
 * - インデックスベースのforループ（ラムダ生成コスト回避）
 * - 厳格なNullチェックと境界防御
 */
public class RefactoredHotbarRenderer implements IHudRenderer {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background.png");
    private static final ResourceLocation BACKGROUND_NO_ORBS_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background_no_orbs.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("exile_overlay", "textures/gui/slot.png");

    private static final int BG_WIDTH = 640;
    private static final int BG_HEIGHT = 256;
    private static final float RENDER_SCALE = 0.5f;

    // スロット定数
    private static final int SLOT_TEX_SIZE = 29;
    private static final int SLOT_DISPLAY_SIZE = 29;
    private static final int SLOT_GAP = 1;
    private static final int SLOT_START_X = 185;
    private static final int SLOT_START_Y = 219;
    private static final int SLOT_PITCH = SLOT_DISPLAY_SIZE + SLOT_GAP;

    // 経験値バー定数
    private static final int EXP_BAR_X = 95;
    private static final int EXP_BAR_WIDTH = 449;
    private static final int MOD_EXP_BAR_Y = 252;
    private static final int VANILLA_EXP_BAR_Y = 249;
    private static final int EXP_BAR_HEIGHT = 2;

    // テキスト定数
    private static final int LEVEL_TEXT_X = 319;
    private static final int LEVEL_TEXT_Y = 210;
    
    // 更新間隔（ミリ秒）- 経験値は頻繁に変化しないため2秒間隔
    private static final long EXP_UPDATE_INTERVAL_MS = 2000;
    
    // 状態管理
    private final RenderState renderState;
    private final ThrottledDataSource expDataSource;
    private final ThrottledDataSource expRequiredDataSource;
    private final ThrottledDataSource levelDataSource;
    
    public RefactoredHotbarRenderer() {
        this.renderState = new RenderState();
        
        // データソースをThrottledでラップ（頻度の低い更新）
        this.expDataSource = new ThrottledDataSource(
            new DataSourceAdapter(DataType.EXP), 
            EXP_UPDATE_INTERVAL_MS
        );
        this.expRequiredDataSource = new ThrottledDataSource(
            new DataSourceAdapter(DataType.EXP_REQUIRED),
            EXP_UPDATE_INTERVAL_MS * 5  // 必要経験値はさらに変化が少ない
        );
        this.levelDataSource = new ThrottledDataSource(
            new DataSourceAdapter(DataType.LEVEL),
            EXP_UPDATE_INTERVAL_MS * 2
        );
    }

    @Override
    public String getId() {
        return "refactored_hotbar";
    }
    
    @Override
    public String getConfigKey() {
        return "hotbar";
    }
    
    @Override
    public int getWidth() {
        return (int) (BG_WIDTH * RENDER_SCALE);
    }
    
    @Override
    public int getHeight() {
        return (int) (BG_HEIGHT * RENDER_SCALE);
    }
    
    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.BOTTOM_CENTER_BASED, // 底辺中心基準
                new Insets(0, 0, 0, 0), // オフセットなし
                new Insets(0, 0, 0, 0) // 拡張なし
        );
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        final Minecraft mc = ctx.getMinecraft();
        final Player player = ctx.getPlayer();
        
        // 厳格なNullチェック
        if (mc == null || player == null) {
            return;
        }
        
        // スペクテーターモードでは描画しない
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        
        // 画面サイズ取得（Contextから）
        final int screenWidth = ctx.getScreenWidth();
        final int screenHeight = ctx.getScreenHeight();
        final long gameTick = ctx.getGameTick();
        
        // ユーザー設定のスケールを取得
        final float userScale = getScale();
        final float totalScale = RENDER_SCALE * userScale;
        
        // 可視オーブを取得（キャッシュ付き）
        final List<OrbType> visibleOrbs = renderState.getVisibleOrbs(player, gameTick);
        
        // ユーザー設定の位置を取得（底辺中心基準）
        int[] pos = resolvePosition(screenWidth, screenHeight);
        final int bgX = pos[0] - (int) ((BG_WIDTH * totalScale) / 2);
        final int bgY = pos[1] - (int) (BG_HEIGHT * totalScale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        graphics.pose().translate(bgX, bgY, 0);
        graphics.pose().scale(totalScale, totalScale, 1.0f);

        try {
            // Layer 1: Background Layer (背面)
            renderExpBars(graphics, mc, player);

            // Layer 2: Fill Layer (中間) - インデックスベースでループ
            final int orbCount = visibleOrbs.size();
            for (int i = 0; i < orbCount; i++) {
                OrbType orbType = visibleOrbs.get(i);
                if (orbType != null) {
                    OrbRenderer.renderFillLayer(graphics, orbType.getConfig(), player);
                }
            }

            // Layer 3: Frame Layer (前面)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            ResourceLocation bgTex = selectBackgroundTexture(visibleOrbs, player);
            graphics.blit(bgTex, 0, 0, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);

            // Layer 4: Overlay Layer (最前面) - インデックスベースでループ
            for (int i = 0; i < orbCount; i++) {
                OrbType orbType = visibleOrbs.get(i);
                if (orbType != null) {
                    OrbRenderer.renderOverlayLayer(graphics, orbType.getConfig(), player, mc);
                }
            }
            
            renderLevelDisplay(graphics, mc, player);
            renderHotbarSlots(graphics, mc, player);
            
        } finally {
            // 必ずポーズを復元
            graphics.pose().popPose();
        }
    }
    
    private void renderExpBars(GuiGraphics graphics, Minecraft mc, Player player) {
        // MOD経験値 (黄色) - ThrottledDataSourceから取得
        float currentModExp = expDataSource.getValue(player);
        float maxModExp = expRequiredDataSource.getValue(player);
        
        // 境界防御: 最大値が0以下の場合は描画しない
        if (maxModExp > 0) {
            renderSingleExpBar(graphics, MOD_EXP_BAR_Y, currentModExp, maxModExp, 0xFFFFFF99);
        }

        // バニラ経験値 (緑) - これは頻繁に変化するので直接取得
        renderSingleExpBar(graphics, VANILLA_EXP_BAR_Y, mc.player.experienceProgress, 1.0f, 0xFF00CC00);
    }

    private void renderSingleExpBar(GuiGraphics graphics, int y, float current, float max, int color) {
        // 境界防御
        if (max <= 0) {
            return;
        }
        
        float progress = Math.min(current / max, 1.0f);

        // 背景
        graphics.fill(EXP_BAR_X, y, EXP_BAR_X + EXP_BAR_WIDTH, y + EXP_BAR_HEIGHT, 0xFF808080);

        // プログレス
        if (progress > 0) {
            int filledWidth = (int) (EXP_BAR_WIDTH * progress);
            graphics.fill(EXP_BAR_X, y, EXP_BAR_X + filledWidth, y + EXP_BAR_HEIGHT, color);
        }
    }

    // レベル表示の色定数
    private static final int VANILLA_LEVEL_COLOR = 0xFFFFFF00; // 黄色
    private static final int MOD_LEVEL_COLOR = 0xFF00FF00;     // 緑
    private static final int SEPARATOR_COLOR = 0xFFFFFFFF;     // 白
    
    private void renderLevelDisplay(GuiGraphics graphics, Minecraft mc, Player player) {
        int vanillaLevel = mc.player.experienceLevel;
        int modLevel = (int) levelDataSource.getValue(player);
        
        String vanillaStr = String.valueOf(vanillaLevel);
        String separator = "/";
        String modStr = String.valueOf(modLevel);
        
        // 各テキストの幅を計算
        int vanillaWidth = mc.font.width(vanillaStr);
        int sepWidth = mc.font.width(separator);
        int modWidth = mc.font.width(modStr);
        int totalWidth = vanillaWidth + sepWidth + modWidth;
        
        float scale = 0.63f;
        float startX = LEVEL_TEXT_X - (totalWidth * scale) / 2;
        float y = LEVEL_TEXT_Y - (mc.font.lineHeight * scale) / 2;
        
        graphics.pose().pushPose();
        graphics.pose().translate(startX, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        
        // バニラレベル（黄色）
        graphics.drawString(mc.font, vanillaStr, 0, 0, VANILLA_LEVEL_COLOR, true);
        
        // 区切り（白）
        graphics.drawString(mc.font, separator, vanillaWidth, 0, SEPARATOR_COLOR, true);
        
        // M&Sレベル（緑）
        graphics.drawString(mc.font, modStr, vanillaWidth + sepWidth, 0, MOD_LEVEL_COLOR, true);
        
        graphics.pose().popPose();
    }

    private ResourceLocation selectBackgroundTexture(List<OrbType> visibleOrbs, Player player) {
        // ORB_3が可視リストに含まれているかチェック
        boolean hasOrb3 = false;
        for (int i = 0; i < visibleOrbs.size(); i++) {
            if (visibleOrbs.get(i) == ORB_3) {
                hasOrb3 = true;
                break;
            }
        }
        
        return hasOrb3 ? BACKGROUND_TEXTURE : BACKGROUND_NO_ORBS_TEXTURE;
    }

    private void renderHotbarSlots(GuiGraphics graphics, Minecraft mc, Player player) {
        int selectedSlot = player.getInventory().selected;

        for (int i = 0; i < 9; i++) {
            int slotX = SLOT_START_X + (i * SLOT_PITCH);

            if (i == selectedSlot) {
                graphics.fill(slotX + 2, SLOT_START_Y + 2, slotX + SLOT_DISPLAY_SIZE - 2,
                        SLOT_START_Y + SLOT_DISPLAY_SIZE - 2,
                        0x80FFFFFF);
            }

            graphics.blit(SLOT_TEXTURE, slotX, SLOT_START_Y, SLOT_DISPLAY_SIZE, SLOT_DISPLAY_SIZE, 0, 0, SLOT_TEX_SIZE,
                    SLOT_TEX_SIZE, SLOT_TEX_SIZE, SLOT_TEX_SIZE);

            ItemStack stack = player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                renderHotbarItem(graphics, mc, stack, slotX, SLOT_START_Y);
            }
        }
    }

    private void renderHotbarItem(GuiGraphics graphics, Minecraft mc, ItemStack stack, int x, int y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x + 3.5f, y + 3.5f, 0);
        graphics.pose().scale(1.375f, 1.375f, 1.0f);
        graphics.renderItem(stack, 0, 0);
        graphics.renderItemDecorations(mc.font, stack, 0, 0);
        graphics.pose().popPose();
    }
    
    /**
     * DataTypeをIDataSourceにアダプトする内部クラス
     */
    private static class DataSourceAdapter implements com.example.exile_overlay.api.IDataSource {
        private final DataType dataType;
        
        DataSourceAdapter(DataType dataType) {
            this.dataType = dataType;
        }
        
        @Override
        public float getValue(Player player) {
            return ModDataProviderRegistry.getValue(player, dataType);
        }
        
        @Override
        public float getMaxValue(Player player) {
            return ModDataProviderRegistry.getMaxValue(player, dataType);
        }
        
        @Override
        public String getId() {
            return "adapter_" + dataType.getKey();
        }
    }
}
