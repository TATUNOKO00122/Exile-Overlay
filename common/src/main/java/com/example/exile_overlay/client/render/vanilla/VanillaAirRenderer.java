package com.example.exile_overlay.client.render.vanilla;

import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanillaAirRenderer implements IHudRenderer, IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaAirRenderer.class);
    public static final String CONFIG_KEY = "vanilla_air";

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");
    private static final int ICON_SIZE = 9;
    private static final int MAX_BUBBLES = 10;
    private static final int SPACING = 8;

    private static boolean positionDirty = true;

    static {
        HudPositionManager.getInstance().addListener(CONFIG_KEY, (key, newPosition) -> {
            positionDirty = true;
        });
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (!isVisible(ctx)) {
            return;
        }

        int airSupply = mc.player.getAirSupply();
        int maxAirSupply = mc.player.getMaxAirSupply();

        if (airSupply >= maxAirSupply) {
            return;
        }

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();
        HudPosition position = HudPositionManager.getInstance().getPosition(CONFIG_KEY);
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = position.getScale();

        renderAirBubbles(graphics, mc.player, pos[0], pos[1], scale);
    }

    private void renderAirBubbles(GuiGraphics graphics, Player player, int x, int y, float scale) {
        int airSupply = player.getAirSupply();
        int maxAirSupply = player.getMaxAirSupply();

        if (airSupply >= maxAirSupply) {
            return;
        }

        double percent = (double) airSupply / (double) maxAirSupply;
        int fullBubbles = (int) Math.ceil(percent * MAX_BUBBLES);

        RenderSystem.enableBlend();

        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(scale, scale, 1.0f);

        for (int i = 0; i < MAX_BUBBLES; i++) {
            int bubbleX = i * SPACING;
            int bubbleY = 0;

            if (i < fullBubbles) {
                // 満タンの泡 - U=16
                graphics.blit(GUI_ICONS, bubbleX, bubbleY, 16, 18, ICON_SIZE, ICON_SIZE);
            } else {
                // 破れた泡 - U=25
                graphics.blit(GUI_ICONS, bubbleX, bubbleY, 25, 18, ICON_SIZE, ICON_SIZE);
            }
        }

        graphics.pose().popPose();
    }

    @Override
    public String getId() {
        return CONFIG_KEY;
    }

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        return IHudRenderer.super.isVisible(ctx);
    }

    @Override
    public int getWidth() {
        return MAX_BUBBLES * SPACING;
    }

    @Override
    public int getHeight() {
        return ICON_SIZE;
    }

    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    @Override
    public RenderLayer getLayer() {
        return RenderLayer.FILL;
    }

    @Override
    public int getPriority() {
        return IHudRenderer.super.getPriority();
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
