package com.example.exile_overlay.client.render;

import com.example.exile_overlay.api.IRenderCommand;
import com.example.exile_overlay.api.IHudRenderer;
import com.example.exile_overlay.api.RenderContext;
import com.example.exile_overlay.api.RenderLayer;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.util.MineAndSlashHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;

public class TargetInfoRenderer implements IRenderCommand, IHudRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetInfoRenderer.class);
    private static final String COMMAND_ID = "target_info";
    private static final int PRIORITY = 80;

    private static final double MAX_DISTANCE = 64.0;
    private static final long RETENTION_MS = 1000;

    private WeakReference<LivingEntity> lastTargetRef = new WeakReference<>(null);
    private long lastTargetTime = 0;

    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation("exile_overlay",
            "textures/gui/target_hp_bar_frame.png");

    private static final int TEX_WIDTH = 224;
    private static final int TEX_HEIGHT = 32;

    private static final int BAR_X = 5;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 213;
    private static final int BAR_HEIGHT = 10;

    private static final int HP_BAR_COLOR = 0xFFCC2020;
    private static final int HP_BG_COLOR = 0x80000000;

    private static final int NAME_Y = 22;

    public TargetInfoRenderer() {
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
        return RenderLayer.OVERLAY;
    }

    @Override
    public boolean isVisible(RenderContext ctx) {
        if (!IHudRenderer.super.isVisible(ctx)) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    @Override
    public void execute(GuiGraphics graphics, RenderContext ctx) {
        render(graphics, ctx);
    }

    @Override
    public void render(GuiGraphics graphics, RenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        LivingEntity target = getTargetEntity(mc.player, MAX_DISTANCE);
        long now = System.currentTimeMillis();

        if (target != null) {
            lastTargetRef = new WeakReference<>(target);
            lastTargetTime = now;
        } else {
            target = lastTargetRef.get();
            if (target == null || !target.isAlive() || (now - lastTargetTime) > RETENTION_MS) {
                return;
            }
        }

        String name = target.getDisplayName().getString();
        if (name.isEmpty()) {
            return;
        }

        int mnsLevel = MineAndSlashHelper.getEntityLevel(target);
        String displayName;
        if (mnsLevel > 0) {
            displayName = "Lv." + mnsLevel + " " + name;
        } else {
            displayName = name;
        }

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float hpRatio = Mth.clamp(health / maxHealth, 0.0f, 1.0f);

        int screenWidth = ctx.getScreenWidth();
        int screenHeight = ctx.getScreenHeight();

        HudPosition position = getPosition();
        int[] pos = position.resolve(screenWidth, screenHeight);
        float scale = getScale();

        int scaledWidth = (int) (TEX_WIDTH * scale);
        int scaledHeight = (int) (TEX_HEIGHT * scale);

        int x = pos[0] - scaledWidth / 2;
        int y = pos[1] - scaledHeight / 2;

        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            RenderSystem.enableBlend();

            graphics.fill(BAR_X, BAR_Y, BAR_X + BAR_WIDTH, BAR_Y + BAR_HEIGHT, HP_BG_COLOR);

            int filledWidth = (int) (BAR_WIDTH * hpRatio);
            if (filledWidth > 0) {
                graphics.fill(BAR_X, BAR_Y, BAR_X + filledWidth, BAR_Y + BAR_HEIGHT, HP_BAR_COLOR);
            }

            graphics.blit(FRAME_TEXTURE, 0, 0, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);

            int nameWidth = mc.font.width(displayName);
            int textX = (TEX_WIDTH - nameWidth) / 2;

            graphics.drawString(mc.font, displayName, textX, NAME_Y, 0xFFFFFFFF, true);

        } finally {
            graphics.pose().popPose();
        }
    }

    private String formatHpText(float health, float maxHealth) {
        return formatNumber(health) + "/" + formatNumber(maxHealth);
    }

    private String formatNumber(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }

    private static LivingEntity getTargetEntity(Player player, double maxDistance) {
        try {
            Vec3 eyePos = player.getEyePosition(1.0f);
            Vec3 lookVec = player.getViewVector(1.0f);
            Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

            AABB searchBox = player.getBoundingBox()
                    .expandTowards(lookVec.scale(maxDistance))
                    .inflate(1.0);

            EntityHitResult result = ProjectileUtil.getEntityHitResult(
                    player,
                    eyePos,
                    endPos,
                    searchBox,
                    entity -> entity instanceof LivingEntity
                            && !entity.isSpectator()
                            && entity.isPickable()
                            && !(entity instanceof Player),
                    maxDistance * maxDistance);

            if (result != null && result.getEntity() instanceof LivingEntity living) {
                return living;
            }

        } catch (Exception e) {
            LOGGER.error("Failed to get target entity", e);
        }

        return null;
    }

    @Override
    public String getConfigKey() {
        return "target_mob_name";
    }

    @Override
    public int getWidth() {
        return TEX_WIDTH;
    }

    @Override
    public int getHeight() {
        return TEX_HEIGHT;
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
