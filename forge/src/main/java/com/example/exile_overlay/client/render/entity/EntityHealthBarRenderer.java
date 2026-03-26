package com.example.exile_overlay.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityHealthBarRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EntityHealthBarRenderer");

    private static final float BASE_SCALE = 0.0267F;
    private static final int COLOR_HEALTH = 0xFFFF4040;
    private static final int COLOR_BACKGROUND = 0x7F401010;

    private EntityHealthBarRenderer() {}

    public static void hookRender(Entity entity, PoseStack poseStack, MultiBufferSource buffers,
            Camera camera, EntityRenderer<? super Entity> entityRenderer,
            float partialTicks, double x, double y, double z) {

        EntityHealthBarConfig config = EntityHealthBarConfig.getInstance();
        if (!config.isEnabled()) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (!shouldShowBar(living, camera.getEntity(), config)) {
            return;
        }

        try {
            renderHealthBar(living, poseStack, buffers, camera, entityRenderer, partialTicks, x, y, z, config);
        } catch (Exception e) {
            LOGGER.error("Failed to render health bar for {}", entity.getName().getString(), e);
        }
    }

    private static boolean shouldShowBar(LivingEntity living, Entity cameraEntity, EntityHealthBarConfig config) {
        if (living == cameraEntity) {
            return false;
        }

        if (living instanceof Player) {
            return false;
        }

        if (living.getMaxHealth() <= 0) {
            return false;
        }

        float health = living.getHealth();
        float maxHealth = living.getMaxHealth();

        if (health >= maxHealth) {
            return false;
        }

        var id = BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());
        if (config.getBlacklist().contains(id.toString())) {
            return false;
        }

        float distance = living.distanceTo(cameraEntity);
        if (distance > config.getMaxDistance()) {
            return false;
        }

        if (living.isInvisible()) {
            return false;
        }

        Team livingTeam = living.getTeam();
        Team cameraTeam = cameraEntity.getTeam();
        if (livingTeam != null) {
            return switch (livingTeam.getNameTagVisibility()) {
                case ALWAYS -> true;
                case NEVER -> false;
                case HIDE_FOR_OTHER_TEAMS -> cameraTeam == null || livingTeam.isAlliedTo(cameraTeam);
                case HIDE_FOR_OWN_TEAM -> cameraTeam == null || !livingTeam.isAlliedTo(cameraTeam);
            };
        }

        return true;
    }

    private static void renderHealthBar(LivingEntity living, PoseStack poseStack,
            MultiBufferSource buffers, Camera camera,
            EntityRenderer<? super Entity> entityRenderer,
            float partialTicks, double x, double y, double z,
            EntityHealthBarConfig config) {

        final int light = 0xF000F0;

        var vec3 = entityRenderer.getRenderOffset(living, partialTicks);
        double d0 = x + vec3.x();
        double d1 = y + vec3.y();
        double d2 = z + vec3.z();

        int barWidth = config.getBarWidth();
        int barHeight = config.getBarHeight();
        float scale = BASE_SCALE * config.getScale();

        poseStack.pushPose();
        poseStack.translate(d0, d1, d2);
        poseStack.translate(0, living.getBbHeight() + config.getHeightAbove(), 0);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        float hpRatio = living.getHealth() / living.getMaxHealth();

        float halfWidth = barWidth / 2.0F;
        float filledWidth = barWidth * hpRatio;

        VertexConsumer builder = buffers.getBuffer(HealthBarRenderType.BAR_TYPE);

        var pose = poseStack.last().pose();

        builder.vertex(pose, -halfWidth, 0, -0.01F).color(COLOR_BACKGROUND).uv(0.0F, 0.0F).uv2(light).endVertex();
        builder.vertex(pose, -halfWidth, barHeight, -0.01F).color(COLOR_BACKGROUND).uv(0.0F, 1.0F).uv2(light).endVertex();
        builder.vertex(pose, halfWidth, barHeight, -0.01F).color(COLOR_BACKGROUND).uv(1.0F, 1.0F).uv2(light).endVertex();
        builder.vertex(pose, halfWidth, 0, -0.01F).color(COLOR_BACKGROUND).uv(1.0F, 0.0F).uv2(light).endVertex();

        if (filledWidth > 0) {
            builder.vertex(pose, -halfWidth, 0, -0.02F).color(COLOR_HEALTH).uv(0.0F, 0.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth, barHeight, -0.02F).color(COLOR_HEALTH).uv(0.0F, 1.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, barHeight, -0.02F).color(COLOR_HEALTH).uv(1.0F, 1.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, 0, -0.02F).color(COLOR_HEALTH).uv(1.0F, 0.0F).uv2(light).endVertex();
        }

        poseStack.popPose();
    }
}