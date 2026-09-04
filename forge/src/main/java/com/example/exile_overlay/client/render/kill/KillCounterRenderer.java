package com.example.exile_overlay.client.render.kill;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * キルカウンター HUD レンダラー
 *
 * アイコン画像を使わず、スタイリッシュなテキスト表記（KILL 38）でキル数を描画。
 * キル発生時に数値部分がポップ（拡大縮小）するモーション付き。
 */
public class KillCounterRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "kill_counter";
    private static final int PRIORITY = 80;

    private static final String PREFIX_TEXT = "KILL ";
    private static final int TEXT_COLOR = 0xFFFFFF;

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
        return mc.font.width(PREFIX_TEXT) + mc.font.width("9999");
    }

    @Override
    public int getHeight() {
        Minecraft mc = Minecraft.getInstance();
        return mc.font.lineHeight;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DraggableHudConfigScreen) {
            return getPosition().isVisible();
        }
        return getPosition().isVisible() && KillCountManager.getInstance().getKillCount() > 0;
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

        int killCount = KillCountManager.getInstance().getKillCount();
        boolean isEditing = mc.screen instanceof DraggableHudConfigScreen;

        if (killCount <= 0 && !isEditing) {
            return;
        }

        if (isEditing && killCount == 0) {
            killCount = 38;
        }

        HudPosition position = getPosition();
        int[] pos = position.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());
        float baseScale = getScale();
        float popMultiplier = KillCountManager.getInstance().getScaleMultiplier();

        RenderSystem.enableBlend();
        graphics.pose().pushPose();
        graphics.pose().translate(pos[0], pos[1], 0);
        graphics.pose().scale(baseScale, baseScale, 1.0f);

        int lineHeight = mc.font.lineHeight;

        // プレフィックス "KILL " の描画
        graphics.drawString(mc.font, PREFIX_TEXT, 0, 0, TEXT_COLOR, true);

        int prefixWidth = mc.font.width(PREFIX_TEXT);
        String numText = String.valueOf(killCount);
        int numWidth = mc.font.width(numText);

        // 数値部分のみポップアニメーション (キル発生時に拡大縮小)
        graphics.pose().pushPose();
        if (popMultiplier > 1.001f) {
            float centerX = prefixWidth + numWidth / 2.0f;
            float centerY = lineHeight / 2.0f;
            graphics.pose().translate(centerX, centerY, 0);
            graphics.pose().scale(popMultiplier, popMultiplier, 1.0f);
            graphics.pose().translate(-centerX, -centerY, 0);
        }

        // 数値の描画
        graphics.drawString(mc.font, numText, prefixWidth, 0, TEXT_COLOR, true);

        graphics.pose().popPose();

        graphics.pose().popPose();
        RenderSystem.disableBlend();
    }
}
