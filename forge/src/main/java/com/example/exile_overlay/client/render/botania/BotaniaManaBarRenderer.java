package com.example.exile_overlay.client.render.botania;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.compat.BotaniaCompat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * Botania の全体マナゲージを exile_overlay の HUD パイプラインで描画するレンダラー。
 * Botania 純正の botania:textures/gui/mana_hud.png および HSV パルスグラデーション色計算を再現。
 */
public class BotaniaManaBarRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "botania_mana_bar";
    private static final String CONFIG_KEY = "botania_mana_bar";
    private static final ResourceLocation MANA_BAR_TEX = ResourceLocation.tryParse("botania:textures/gui/mana_hud.png");
    private static final HudPositionManager POSITION_MANAGER = HudPositionManager.getInstance();

    private static final int BASE_WIDTH = 182;
    private static final int BASE_HEIGHT = 5;

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
        return 50;
    }

    @Override
    public int getWidth() {
        return BASE_WIDTH;
    }

    @Override
    public int getHeight() {
        return BASE_HEIGHT;
    }

    @Override
    public int getConfigWidth() {
        return BASE_WIDTH;
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
            CoordinateSystem.BOTTOM_CENTER_BASED,
            new Insets(0, 0, 0, 0),
            new Insets(3, 3, 3, 3)
        );
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!BotaniaCompat.isBotaniaLoaded()) {
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

        Screen currentScreen = Minecraft.getInstance().screen;
        boolean isConfigScreen = currentScreen instanceof DraggableHudConfigScreen;

        BotaniaCompat.ManaInfo manaInfo = BotaniaCompat.getPlayerManaInfo(player);
        boolean hasManaUsingItem = BotaniaCompat.hasManaUsingItem(player);

        // 設定画面中でなく、マナ使用アイテムがない場合は描画しない
        if (!isConfigScreen && !hasManaUsingItem) {
            return;
        }

        int totalMana = manaInfo.totalMana();
        int totalMaxMana = manaInfo.totalMaxMana();

        int barWidth = BASE_WIDTH;
        if (totalMaxMana == 0) {
            barWidth = isConfigScreen ? BASE_WIDTH : 0;
        } else {
            barWidth = (int) Math.round((double) BASE_WIDTH * totalMana / totalMaxMana);
        }

        if (barWidth == 0 && totalMana > 0) {
            barWidth = 1;
        }

        if (barWidth <= 0 && !isConfigScreen) {
            return;
        }

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        HudPosition position = getPosition();
        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = position.getScale();

        poseStack.translate(pos[0], pos[1] - BASE_HEIGHT / 2.0f, 0);
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, 1.0f);
        }
        poseStack.translate(-BASE_WIDTH / 2.0f, -BASE_HEIGHT / 2.0f, 0);

        // Botania純正の HSV パルスカラー計算（サチュレーションを0.5~1.0の範囲でスムーズに明滅）
        float sat = (float) (Math.sin(Util.getMillis() / 200.0) * 0.25 + 0.75);
        int color = Mth.hsvToRgb(0.55F, sat, 1.0F);
        float r = (color >> 16 & 0xFF) / 255F;
        float g = (color >> 8 & 0xFF) / 255F;
        float b = (color & 0xFF) / 255F;

        RenderSystem.setShaderColor(r, g, b, 1.0F - r);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Botaniaのマナバーを描画 (182px, u=0, v=251)
        if (MANA_BAR_TEX != null) {
            graphics.blit(MANA_BAR_TEX, 0, 0, 0, 251, Math.min(BASE_WIDTH, barWidth), BASE_HEIGHT, 256, 256);
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
