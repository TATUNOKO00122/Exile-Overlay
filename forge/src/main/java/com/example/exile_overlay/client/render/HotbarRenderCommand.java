package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.DataType;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.ModDataProviderRegistry;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.render.orb.OrbRegistry;
import com.example.exile_overlay.client.render.orb.OrbRenderer;
import com.example.exile_overlay.client.render.orb.OrbShaderRenderer;
import com.example.exile_overlay.client.render.orb.OrbType;
import com.example.exile_overlay.util.MineAndSlashHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.exile_overlay.client.render.orb.OrbType.ORB_3;

/**
 * ホットバー描画コマンド
 *
 * 【Layered Sandwich Rendering Architecture】
 * 描画順序（下から上）：
 * 1. Background Layer (背面): 経験値バーなど
 * 2. Fill Layer (中間): オーブ液面（シェーダーで描画）
 * 3. Frame Layer (前面): 背景フレーム（透明マスクで円形を形成）
 * 4. Overlay Layer (最前面): 反射、テキスト、スロット
 *
 * 【パフォーマンス最適化】
 * - StringBuilder再利用によるGCプレッシャー低減
 * - 可視性チェックで不要な描画をスキップ
 *
 * 【位置設定対応】
 * - IHudRendererを実装してドラッグ設定に対応
 * - HudPositionManagerから位置を取得
 */
public class HotbarRenderCommand implements IRenderCommand, IHudRenderer {
    
    private static final String COMMAND_ID = "hotbar";
    private static final int PRIORITY = 100;
    
    private static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background.png");
    private static final ResourceLocation BACKGROUND_NO_ORBS_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/hotbar_background_no_orbs.png");
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("exile_overlay", "textures/gui/slot.png");

    private static final int BG_WIDTH = 640;
    private static final int BG_HEIGHT = 256;
    private static final float RENDER_SCALE = 0.5f;

    // スロット定数
    private static final int SLOT_TEX_SIZE = 30;
    private static final int SLOT_DISPLAY_SIZE = 30;
    private static final int SLOT_GAP = 1;
    private static final int SLOT_START_X = 181;
    private static final int SLOT_START_Y = 218;
    private static final int SLOT_PITCH = SLOT_DISPLAY_SIZE + SLOT_GAP;

    // オフハンドスロット定数（アイテムスロットより4px小さい）
    private static final int OFFHAND_SLOT_DISPLAY_SIZE = 26;
    private static final int OFFHAND_SLOT_GAP = 2;

    // ポーションスロット定数（テクスチャサイズ16x26、アイテムは16x16原寸描画）
    private static final int POTION_SLOT_DISPLAY_SIZE_X = 16;
    private static final int POTION_SLOT_DISPLAY_SIZE_Y = 26;
    private static final int POTION_SLOT_GAP = 2;
    private static final int POTION_SLOT_INNER_GAP = 1;
    private static final int POTION_SLOT_COUNT = 2;
    private static final ResourceLocation POTION_SLOT_TEXTURE = 
        new ResourceLocation("exile_overlay", "textures/gui/potion_slot.png");

    // 経験値バー定数
    private static final int EXP_BAR_X = 65;
    private static final int EXP_BAR_WIDTH = 509;
    private static final int MOD_EXP_BAR_Y = 252;
    private static final int VANILLA_EXP_BAR_Y = 249;
    private static final int EXP_BAR_HEIGHT = 2;

    // テキスト定数（中心座標、HJUDと同じ値）
    private static final float LEVEL_CENTER_X = 320.0f;
    private static final float LEVEL_CENTER_Y = 204.0f;
    
    // レイアウト設定
    private final int screenOffsetY;
    
    // 文字列フォーマットキャッシュ
    private final StringFormatCache levelFormatCache = new StringFormatCache();
    
    /**
     * 文字列フォーマットキャッシュ
     * StringBuilderを再利用してGCプレッシャーを低減
     */
    private static final class StringFormatCache {
        private final StringBuilder sb = new StringBuilder(32);
        private int lastVanillaLevel = -1;
        private int lastModLevel = -1;
        private String cached = "";
        
        String formatLevel(int vanillaLevel, int modLevel) {
            if (vanillaLevel != lastVanillaLevel || modLevel != lastModLevel) {
                sb.setLength(0);
                sb.append(vanillaLevel).append(" / ").append(modLevel);
                cached = sb.toString();
                lastVanillaLevel = vanillaLevel;
                lastModLevel = modLevel;
            }
            return cached;
        }
    }
    
    public HotbarRenderCommand() {
        this(0);
    }
    
    public HotbarRenderCommand(int screenOffsetY) {
        this.screenOffsetY = screenOffsetY;
    }
    
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
        return RenderLayer.FILL;
    }
    
    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!IHudRenderer.super.isVisible(ctx)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        return mc.gameMode == null || 
               mc.gameMode.getPlayerMode() != net.minecraft.world.level.GameType.SPECTATOR;
    }
    
    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }
    
    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        // 画面サイズ取得
        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        // 表示中のオーブを取得
        List<OrbType> visibleOrbs = OrbRegistry.getVisibleOrbs(player);

        // 位置設定とスケールを取得
        HudPosition position = getPosition();
        int[] pos = position.resolve(screenWidth, screenHeight);
        float userScale = getScale();
        float totalScale = RENDER_SCALE * userScale;

        // ホットバーは底辺中心基準で描画するため、オフセットを計算
        int width = (int) (BG_WIDTH * totalScale);
        int height = (int) (BG_HEIGHT * totalScale);
        int bgX = pos[0] - width / 2;
        int bgY = pos[1] - height + screenOffsetY;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(bgX, bgY, 0);
            graphics.pose().scale(totalScale, totalScale, 1.0f);
            
            // Layer 1: Background Layer (背面)
            renderExpBars(graphics, mc);
            
            // Layer 2: Fill Layer (中間)
            visibleOrbs.forEach(orbType -> OrbRenderer.renderFillLayer(graphics, orbType.getConfig(), player));
            
            // Layer 3: Frame Layer (前面)
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            ResourceLocation bgTex = selectBackgroundTexture(visibleOrbs, player);
            graphics.blit(bgTex, 0, 0, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
            
            // Layer 4: Overlay Layer (最前面)
            visibleOrbs.forEach(orbType -> OrbRenderer.renderOverlayLayer(graphics, orbType.getConfig(), player, mc));
            renderLevelDisplay(graphics, mc);
            renderHotbarSlots(graphics, mc);
        } finally {
            graphics.pose().popPose();
        }
    }
    
    private void renderExpBars(GuiGraphics graphics, Minecraft mc) {
        // MOD経験値 (明るい黄土色)
        float currentModExp = ModDataProviderRegistry.getValue(mc.player, DataType.EXP);
        float maxModExp = ModDataProviderRegistry.getMaxValue(mc.player, DataType.EXP_REQUIRED);
        renderSingleExpBar(graphics, MOD_EXP_BAR_Y, currentModExp, maxModExp, 0xFFF0C040);
        
        // バニラ経験値 (濃ゆい明るいオリーブ)
        renderSingleExpBar(graphics, VANILLA_EXP_BAR_Y, mc.player.experienceProgress, 1.0f, 0xFF40B040);
    }
    
    private void renderSingleExpBar(GuiGraphics graphics, int y, float current, float max, int color) {
        float progress = max > 0 ? Math.min(current / max, 1.0f) : 0;
        
        // 背景
        graphics.fill(EXP_BAR_X, y, EXP_BAR_X + EXP_BAR_WIDTH, y + EXP_BAR_HEIGHT, 0xFF808080);
        
        // プログレス
        if (progress > 0) {
            int filledWidth = (int) (EXP_BAR_WIDTH * progress);
            graphics.fill(EXP_BAR_X, y, EXP_BAR_X + filledWidth, y + EXP_BAR_HEIGHT, color);
        }
    }

    private void renderLevelDisplay(GuiGraphics graphics, Minecraft mc) {
        int vanillaLevel = mc.player.experienceLevel;
        int modLevel = (int) ModDataProviderRegistry.getValue(mc.player, DataType.LEVEL);

        String vanillaStr = String.valueOf(vanillaLevel);
        String separator = "/";
        String modStr = String.valueOf(modLevel);

        // 各テキストの幅を計算
        int vanillaWidth = mc.font.width(vanillaStr);
        int sepWidth = mc.font.width(separator);
        int modWidth = mc.font.width(modStr);
        int totalWidth = vanillaWidth + sepWidth + modWidth;
        int textHeight = mc.font.lineHeight;

        // HJUDと同じ計算方法
        float maxWidth = 27.0f;
        float scale = Math.min(maxWidth / totalWidth, 1.0f);
        scale = Math.min(scale, 0.8f);

        graphics.pose().pushPose();
        graphics.pose().translate(LEVEL_CENTER_X, LEVEL_CENTER_Y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        int startX = -totalWidth / 2;
        int textY = -textHeight / 2 + 1;

        // バニラレベル（緑）
        graphics.drawString(mc.font, vanillaStr, startX, textY, 0x55FF55, false);

        // 区切り（白）
        graphics.drawString(mc.font, separator, startX + vanillaWidth, textY, 0xFFFFFF, false);

        // M&Sレベル（黄色）
        graphics.drawString(mc.font, modStr, startX + vanillaWidth + sepWidth, textY, 0xFFFF55, false);

        graphics.pose().popPose();
    }
    
    private ResourceLocation selectBackgroundTexture(List<OrbType> visibleOrbs, Player player) {
        if (!OrbRegistry.isOrbVisible(player, ORB_3)) {
            return BACKGROUND_NO_ORBS_TEXTURE;
        }
        return BACKGROUND_TEXTURE;
    }
    
    private void renderHotbarSlots(GuiGraphics graphics, Minecraft mc) {
        // オフハンドスロット（左側、下部合わせ）
        renderOffhandSlot(graphics, mc);

        // ポーションスロット（右側、下部合わせ）
        renderPotionSlot(graphics, mc);

        int selectedSlot = mc.player.getInventory().selected;
        
        for (int i = 0; i < 9; i++) {
            int slotX = SLOT_START_X + (i * SLOT_PITCH);
            
            if (i == selectedSlot) {
                graphics.fill(slotX + 2, SLOT_START_Y + 2, slotX + SLOT_DISPLAY_SIZE - 2,
                        SLOT_START_Y + SLOT_DISPLAY_SIZE - 2,
                        0x80FFFFFF);
            }
            
            graphics.blit(SLOT_TEXTURE, slotX, SLOT_START_Y, SLOT_DISPLAY_SIZE, SLOT_DISPLAY_SIZE, 0, 0, SLOT_TEX_SIZE,
                    SLOT_TEX_SIZE, SLOT_TEX_SIZE, SLOT_TEX_SIZE);
            
            ItemStack stack = mc.player.getInventory().items.get(i);
            if (!stack.isEmpty()) {
                renderHotbarItem(graphics, mc, stack, slotX, SLOT_START_Y);
            }
        }
    }

    private void renderOffhandSlot(GuiGraphics graphics, Minecraft mc) {
        int offhandX = SLOT_START_X - OFFHAND_SLOT_GAP - OFFHAND_SLOT_DISPLAY_SIZE;
        int offhandY = SLOT_START_Y + (SLOT_DISPLAY_SIZE - OFFHAND_SLOT_DISPLAY_SIZE);

        graphics.blit(SLOT_TEXTURE, offhandX, offhandY,
                OFFHAND_SLOT_DISPLAY_SIZE, OFFHAND_SLOT_DISPLAY_SIZE,
                0, 0, SLOT_TEX_SIZE, SLOT_TEX_SIZE,
                SLOT_TEX_SIZE, SLOT_TEX_SIZE);

        ItemStack offhandStack = mc.player.getOffhandItem();
        if (!offhandStack.isEmpty()) {
            graphics.pose().pushPose();
            graphics.pose().translate(offhandX + 2.0f, offhandY + 2.0f, 0);
            float itemScale = (OFFHAND_SLOT_DISPLAY_SIZE - 4) / 16.0f;
            graphics.pose().scale(itemScale, itemScale, 1.0f);
            graphics.renderItem(offhandStack, 0, 0);
            graphics.renderItemDecorations(mc.font, offhandStack, 0, 0);
            graphics.pose().popPose();
        }
    }

    private void renderPotionSlot(GuiGraphics graphics, Minecraft mc) {
        int lastSlotX = SLOT_START_X + (8 * SLOT_PITCH);
        int potionY = SLOT_START_Y + SLOT_DISPLAY_SIZE - POTION_SLOT_DISPLAY_SIZE_Y;

        ItemStack hpPotion = MineAndSlashHelper.findBestPotion(mc.player, true);
        ItemStack manaPotion = MineAndSlashHelper.findBestPotion(mc.player, false);
        boolean hpCooldown = MineAndSlashHelper.isPotionOnCooldown(mc.player, hpPotion);
        boolean manaCooldown = MineAndSlashHelper.isPotionOnCooldown(mc.player, manaPotion);

        for (int i = 0; i < POTION_SLOT_COUNT; i++) {
            int potionX = lastSlotX + SLOT_DISPLAY_SIZE + POTION_SLOT_GAP 
                + (i * (POTION_SLOT_DISPLAY_SIZE_X + POTION_SLOT_INNER_GAP));

            ItemStack stack = (i == 0) ? hpPotion : manaPotion;
            boolean cooldown = (i == 0) ? hpCooldown : manaCooldown;

            if (!stack.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(potionX, potionY + 5.0f, -10.0f);

                if (cooldown) {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.5f);
                }

                graphics.renderItem(stack, 0, 0);

                if (cooldown) {
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                }

                graphics.pose().popPose();
            }

            graphics.blit(POTION_SLOT_TEXTURE, potionX, potionY,
                    POTION_SLOT_DISPLAY_SIZE_X, POTION_SLOT_DISPLAY_SIZE_Y,
                    0, 0, POTION_SLOT_DISPLAY_SIZE_X, POTION_SLOT_DISPLAY_SIZE_Y,
                    POTION_SLOT_DISPLAY_SIZE_X, POTION_SLOT_DISPLAY_SIZE_Y);
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
    
    // IHudRenderer implementation
    
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
        // 底辺中心基準: Xは中心、Yは底辺
        // render()メソッド: bgY = pos[1] - height + screenOffsetY
        return new HudRenderMetadata(
            CoordinateSystem.BOTTOM_CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(0, 0, 0, 0)
        );
    }
}
