package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.shadowsoffire.gateways.entity.EndlessGatewayEntity;
import dev.shadowsoffire.gateways.entity.GatewayEntity;
import dev.shadowsoffire.gateways.entity.NormalGatewayEntity;
import dev.shadowsoffire.gateways.gate.Gateway;
import dev.shadowsoffire.gateways.gate.normal.NormalGateway;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GatewayBossBarRenderer implements IRenderCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayBossBarRenderer.class);
    private static final String COMMAND_ID = "gateway_boss_bar";
    private static final int PRIORITY = 88;

    private static final ResourceLocation BARS = new ResourceLocation("textures/gui/bars.png");

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int TOTAL_HEIGHT = 50;

    private static final double GATEWAY_SCAN_RANGE = 64.0;
    private static volatile int cachedGatewayId = -1;
    private static long lastScanTime = 0;
    private static final long SCAN_INTERVAL_MS = 500;

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
    public boolean isVisible(RenderContext ctx) {
        if (!IRenderCommand.super.isVisible(ctx)) {
            return false;
        }
        return findActiveGateway() != null;
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        GatewayEntity gate = findActiveGateway();
        if (gate == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        HudPosition position = getPosition();
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = getScale();

        int scaledWidth = (int) (BAR_WIDTH * scale);

        int x = pos[0] - scaledWidth / 2;
        int y = pos[1] - (int) (TOTAL_HEIGHT * scale) / 2;

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            if (gate instanceof NormalGatewayEntity normalGate) {
                renderNormalGateway(graphics, mc, normalGate);
            } else if (gate instanceof EndlessGatewayEntity endlessGate) {
                renderEndlessGateway(graphics, mc, endlessGate);
            }

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            LOGGER.error("Failed to render gateway boss bar: {}", e.getMessage());
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            graphics.pose().popPose();
        }
    }

    private void renderNormalGateway(GuiGraphics gfx, Minecraft mc, NormalGatewayEntity gate) {
        NormalGateway ng = gate.getGateway();
        int color = ng.color().getValue();
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        RenderSystem.setShaderColor(r / 255F, g / 255F, b / 255F, 1.0F);

        Font font = mc.font;
        int lineHeight = font.lineHeight;

        int wave = gate.getWave() + 1;
        int maxWave = ng.getNumWaves();
        int enemies = gate.getActiveEnemies();
        int maxEnemies = gate.getCurrentWave().entities().stream()
                .mapToInt(we -> we.getCount()).sum();

        float maxTime = gate.getMaxWaveTime();
        String gatewayName = gate.getCustomName().getString();

        int yBar1 = lineHeight + 2;
        int yBar2 = yBar1 + BAR_HEIGHT + lineHeight + 2;

        gfx.blit(BARS, 0, yBar1, 0, 6 * 5 * 2, BAR_WIDTH, BAR_HEIGHT, 256, 256);
        gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2, BAR_WIDTH, BAR_HEIGHT, 256, 256);

        float waveProgress = 1F / maxWave;
        float progress = waveProgress * (maxWave - wave + 1);
        if (gate.isWaveActive()) {
            progress -= waveProgress * ((float) (maxEnemies - enemies) / maxEnemies);
        }

        int i = (int) (progress * BAR_WIDTH + 1);
        if (i > 0) {
            gfx.blit(BARS, 0, yBar1, 0, 6 * 5 * 2 + 5, i, BAR_HEIGHT, 256, 256);
        }

        if (gate.isWaveActive()) {
            i = (int) ((maxTime - gate.getTicksActive()) / maxTime * BAR_WIDTH + 1);
            if (i > 0) {
                gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2 + 5, i, BAR_HEIGHT, 256, 256);
            }
        } else {
            float setupTime = gate.getSetupTime();
            i = (int) (gate.getTicksActive() / setupTime * BAR_WIDTH + 1);
            if (i > 0) {
                gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2 + 5, i, BAR_HEIGHT, 256, 256);
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);

        Component nameComp = Component.literal(gatewayName).withStyle(ChatFormatting.GOLD);
        int nameWidth = font.width(nameComp);
        int nameX = BAR_WIDTH / 2 - nameWidth / 2;
        gfx.drawString(font, nameComp, nameX, 0, 0xFFFFFF, true);

        int time = (int) maxTime - gate.getTicksActive();
        String infoStr;
        if (!gate.isWaveActive()) {
            if (gate.isLastWave()) {
                infoStr = I18n.get("boss.gateways.done");
            } else {
                infoStr = I18n.get("boss.gateways.starting", wave, StringUtil.formatTickDuration(Math.abs(time)));
            }
        } else {
            infoStr = I18n.get("boss.gateways.wave", wave, maxWave, StringUtil.formatTickDuration(Math.abs(time)), enemies);
        }

        Component infoComp = Component.literal(infoStr).withStyle(ChatFormatting.GREEN);
        int infoWidth = font.width(infoComp);
        int infoX = BAR_WIDTH / 2 - infoWidth / 2;
        int infoY = yBar1 + BAR_HEIGHT + 1;
        gfx.drawString(font, infoComp, infoX, infoY, 0xFFFFFF, true);
    }

    private void renderEndlessGateway(GuiGraphics gfx, Minecraft mc, EndlessGatewayEntity gate) {
        Gateway gw = gate.getGateway();
        int color = gw.color().getValue();
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        RenderSystem.setShaderColor(r / 255F, g / 255F, b / 255F, 1.0F);

        Font font = mc.font;
        int lineHeight = font.lineHeight;

        int wave = gate.getWave() + 1;
        int enemies = gate.getActiveEnemies();
        int maxEnemies = gate.getMaxEnemies();
        int modifiers = gate.getModifiersApplied();

        int yBar1 = lineHeight + 2;
        int yBar2 = yBar1 + BAR_HEIGHT + lineHeight + 2;

        gfx.blit(BARS, 0, yBar1, 0, 6 * 5 * 2, BAR_WIDTH, BAR_HEIGHT, 256, 256);
        gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2, BAR_WIDTH, BAR_HEIGHT, 256, 256);

        float maxTime = gate.getMaxWaveTime();
        int barWidth;

        if (gate.isWaveActive()) {
            barWidth = (int) (BAR_WIDTH * (float) enemies / maxEnemies);
            if (barWidth > 0) {
                gfx.blit(BARS, 0, yBar1, 0, 6 * 5 * 2 + 5, barWidth, BAR_HEIGHT, 256, 256);
            }

            barWidth = (int) ((maxTime - gate.getTicksActive()) / maxTime * BAR_WIDTH);
            if (barWidth > 0) {
                gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2 + 5, barWidth, BAR_HEIGHT, 256, 256);
            }
        } else {
            float setupTime = gate.getSetupTime();
            barWidth = (int) (gate.getTicksActive() / setupTime * BAR_WIDTH);
            if (barWidth > 0) {
                gfx.blit(BARS, 0, yBar1, 0, 6 * 5 * 2 + 5, barWidth, BAR_HEIGHT, 256, 256);
                gfx.blit(BARS, 0, yBar2, 0, 6 * 5 * 2 + 5, barWidth, BAR_HEIGHT, 256, 256);
            }
        }

        RenderSystem.setShaderColor(1, 1, 1, 1);

        String gatewayName = gate.getCustomName().getString();
        Component nameComp = Component.literal(gatewayName).withStyle(ChatFormatting.GOLD, ChatFormatting.UNDERLINE);
        int nameWidth = font.width(nameComp);
        int nameX = BAR_WIDTH / 2 - nameWidth / 2;
        gfx.drawString(font, nameComp, nameX, 0, 0xFFFFFF, true);

        int time = (int) maxTime - gate.getTicksActive();
        String topStr;
        String botStr;

        if (gate.isWaveActive()) {
            topStr = I18n.get("boss.gateways.endless.top", wave, '\u221E', StringUtil.formatTickDuration(Math.abs(time)));
            botStr = I18n.get("boss.gateways.endless.bot", enemies, maxEnemies, modifiers);
        } else {
            topStr = I18n.get("boss.gateways.endless.top.wave", wave);
            botStr = I18n.get("boss.gateways.starting", wave, StringUtil.formatTickDuration(Math.abs(time)));
        }

        Component topComp = Component.literal(topStr).withStyle(ChatFormatting.GREEN);
        int topWidth = font.width(topComp);
        int topX = BAR_WIDTH / 2 - topWidth / 2;
        int topY = yBar1 + BAR_HEIGHT + 1;
        gfx.drawString(font, topComp, topX, topY, 0xFFFFFF, true);

        Component botComp = Component.literal(botStr).withStyle(ChatFormatting.YELLOW);
        int botWidth = font.width(botComp);
        int botX = BAR_WIDTH / 2 - botWidth / 2;
        int botY = yBar2 + BAR_HEIGHT + 1;
        gfx.drawString(font, botComp, botX, botY, 0xFFFFFF, true);
    }

    private static GatewayEntity findActiveGateway() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }

        if (cachedGatewayId != -1) {
            Entity entity = mc.level.getEntity(cachedGatewayId);
            if (entity instanceof GatewayEntity cached && cached.isAlive()) {
                double distSq = cached.distanceToSqr(mc.player);
                if (distSq <= GATEWAY_SCAN_RANGE * GATEWAY_SCAN_RANGE) {
                    return cached;
                }
            }
            cachedGatewayId = -1;
        }

        long now = System.currentTimeMillis();
        if (now - lastScanTime < SCAN_INTERVAL_MS) {
            if (cachedGatewayId != -1) {
                Entity entity = mc.level.getEntity(cachedGatewayId);
                if (entity instanceof GatewayEntity gate) {
                    return gate;
                }
            }
            return null;
        }
        lastScanTime = now;

        AABB searchBox = new AABB(
                mc.player.getX() - GATEWAY_SCAN_RANGE, mc.player.getY() - GATEWAY_SCAN_RANGE, mc.player.getZ() - GATEWAY_SCAN_RANGE,
                mc.player.getX() + GATEWAY_SCAN_RANGE, mc.player.getY() + GATEWAY_SCAN_RANGE, mc.player.getZ() + GATEWAY_SCAN_RANGE
        );

        GatewayEntity found = mc.level.getEntitiesOfClass(
                GatewayEntity.class,
                searchBox,
                e -> e != null && e.isAlive()
        ).stream().findFirst().orElse(null);

        cachedGatewayId = found != null ? found.getId() : -1;
        return found;
    }

    @Override
    public int getConfigWidth() {
        return BAR_WIDTH;
    }

    @Override
    public int getConfigHeight() {
        return TOTAL_HEIGHT;
    }

    @Override
    public String getConfigKey() {
        return COMMAND_ID;
    }

    @Override
    public int getWidth() {
        return BAR_WIDTH;
    }

    @Override
    public int getHeight() {
        return TOTAL_HEIGHT;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    @Override
    public HudRenderMetadata getRenderMetadata() {
        return new HudRenderMetadata(
                CoordinateSystem.CENTER_BASED,
                new Insets(0, 0, 0, 0),
                new Insets(0, 0, 0, 0)
        );
    }
}
