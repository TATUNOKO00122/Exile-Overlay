package com.example.exile_overlay.client.render.currency;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.compat.LightmansCurrencyCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Lightman's Currency のコイン・財布HUD表示。
 * DraggableHudConfigScreenと連携し、未所持時はダミープレビューを描画して直感的な配置移動を可能にする。
 */
public class LightmansCurrencyCoinRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "lightmans_currency_coins";
    private static final String CONFIG_KEY = "lightmans_currency_coins";

    private static final int ITEM_SPACING = 17;
    private static final int BASE_HEIGHT = 18;
    private static final int DEFAULT_PREVIEW_WIDTH = 16 + (ITEM_SPACING * 4); // 財布 + コイン4枚

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.OVERLAY;
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public int getWidth() {
        return DEFAULT_PREVIEW_WIDTH;
    }

    @Override
    public int getHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public int getConfigWidth() {
        return DEFAULT_PREVIEW_WIDTH;
    }

    @Override
    public int getConfigHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
            CoordinateSystem.TOP_LEFT_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(2, 2, 2, 2)
        );
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!LightmansCurrencyCompat.isLightmansCurrencyLoaded()) {
            return false;
        }
        return getPosition().isVisible();
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Screen currentScreen = mc.screen;
        boolean isConfigScreen = currentScreen instanceof DraggableHudConfigScreen;

        ItemStack wallet = LightmansCurrencyCompat.getEquippedWallet(player);
        List<ItemStack> coins = LightmansCurrencyCompat.getStoredCoins(player);

        // 設定画面中でなく、財布もコインも所持していない場合は描画しない
        if (!isConfigScreen && wallet.isEmpty() && coins.isEmpty()) {
            return;
        }

        // 設定画面中で所持品が空の場合はプレビュー用ダミーデータを使用
        if (isConfigScreen && wallet.isEmpty() && coins.isEmpty()) {
            wallet = LightmansCurrencyCompat.getPreviewWallet();
            coins = LightmansCurrencyCompat.getPreviewCoins();
        }

        HudPosition position = getPosition();
        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = position.getScale();

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(pos[0], pos[1], 0);
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, 1.0f);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int currentX = 0;
        int currentY = 0;

        // 1. 財布アイテムを描画（存在する場合）
        if (!wallet.isEmpty()) {
            graphics.renderItem(wallet, currentX, currentY);
            graphics.renderItemDecorations(mc.font, wallet, currentX, currentY);
            currentX += ITEM_SPACING;
        }

        // 2. コインアイテム列を描画
        for (ItemStack coin : coins) {
            if (coin != null && !coin.isEmpty()) {
                graphics.renderItem(coin, currentX, currentY);
                graphics.renderItemDecorations(mc.font, coin, currentX, currentY);
                currentX += ITEM_SPACING;
            }
        }

        RenderSystem.disableBlend();
        poseStack.popPose();
    }
}
