package com.example.exile_overlay.client.dungeon;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class DungeonTimerRenderer implements IRenderCommand {

    private static final String COMMAND_ID = "dungeon_timer";
    private static final int DEFAULT_COLOR = 0xFFF0F0F0;

    @Override
    public String getId() {
        return COMMAND_ID;
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.OVERLAY;
    }

    @Override
    public int getPriority() {
        return 88;
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
            new Insets(0, 0, 0, 0)
        );
    }

    @Override
    public int getWidth() {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int digitWidth = Math.max(font.width("0"), font.width("8"));
        int colonWidth = font.width(":");
        return digitWidth * 4 + colonWidth;
    }

    @Override
    public int getHeight() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DraggableHudConfigScreen) {
            return getPosition().isVisible();
        }
        if (mc.screen != null) {
            return false;
        }
        return getPosition().isVisible() && DungeonTimerManager.getInstance().isInDungeon();
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        DungeonTimerManager timer = DungeonTimerManager.getInstance();
        Font font = mc.font;
        boolean showHours = timer.getElapsedSeconds() >= 3600L;

        int digitWidth = Math.max(font.width("0"), font.width("8"));
        int colonWidth = font.width(":");

        HudPosition position = getPosition();
        int[] pos = position.resolve(ctx.getScreenWidth(), ctx.getScreenHeight());
        float scale = getScale();

        RenderSystem.enableBlend();
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(pos[0], pos[1], 0);
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, 1.0f);
        }

        int curX = 0;

        if (showHours) {
            timer.getHourTens().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);
            curX += digitWidth;
            timer.getHourOnes().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);
            curX += digitWidth;

            graphics.drawString(font, ":", curX + (colonWidth - font.width(":")) / 2, 0, DEFAULT_COLOR, true);
            curX += colonWidth;
        }

        timer.getMinTens().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);
        curX += digitWidth;
        timer.getMinOnes().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);
        curX += digitWidth;

        graphics.drawString(font, ":", curX + (colonWidth - font.width(":")) / 2, 0, DEFAULT_COLOR, true);
        curX += colonWidth;

        timer.getSecTens().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);
        curX += digitWidth;
        timer.getSecOnes().render(graphics, font, curX, 0, digitWidth, DEFAULT_COLOR);

        poseStack.popPose();
        RenderSystem.disableBlend();
    }
}
