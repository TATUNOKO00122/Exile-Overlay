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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityHealthBarRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EntityHealthBarRenderer");

    private static final int BAR_WIDTH = 30;
    private static final int BAR_HEIGHT = 2;
    private static final float GLOBAL_SCALE = 0.0267F;

    private static final int COLOR_HOSTILE = 0xFFFF4040;
    private static final int COLOR_FRIENDLY = 0xFF40FF40;
    private static final int COLOR_BACKGROUND = 0x7F401010;

    private EntityHealthBarRenderer() {}

    public static void hookRender(Entity entity, PoseStack poseStack, MultiBufferSource buffers,
            Camera camera, EntityRenderer<? super Entity> entityRenderer,
            float partialTicks, double x, double y, double z) {

        if (!EntityHealthBarConfig.enabled) {
            return;
        }

        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        if (!shouldShowBar(living, camera.getEntity())) {
            return;
        }

        try {
            renderHealthBar(living, poseStack, buffers, camera, entityRenderer, partialTicks, x, y, z);
        } catch (Exception e) {
            LOGGER.error("Failed to render health bar for {}", entity.getName().getString(), e);
        }
    }

    private static boolean shouldShowBar(LivingEntity living, Entity cameraEntity) {
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
        if (EntityHealthBarConfig.instance.blacklist().contains(id.toString())) {
            return false;
        }

        float distance = living.distanceTo(cameraEntity);
        if (distance > EntityHealthBarConfig.instance.maxDistance()) {
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
            float partialTicks, double x, double y, double z) {

        final int light = 0xF000F0;
        final double heightAbove = EntityHealthBarConfig.instance.heightAbove();

        var vec3 = entityRenderer.getRenderOffset(living, partialTicks);
        double d0 = x + vec3.x();
        double d1 = y + vec3.y();
        double d2 = z + vec3.z();

        poseStack.pushPose();
        poseStack.translate(d0, d1, d2);
        poseStack.translate(0, living.getBbHeight() + heightAbove, 0);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-GLOBAL_SCALE, -GLOBAL_SCALE, GLOBAL_SCALE);

        float hpRatio = living.getHealth() / living.getMaxHealth();
        int barColor = getBarColor(living);

        float halfWidth = BAR_WIDTH / 2.0F;
        float filledWidth = BAR_WIDTH * hpRatio;

        VertexConsumer builder = buffers.getBuffer(HealthBarRenderType.BAR_TYPE);

        var pose = poseStack.last().pose();

        builder.vertex(pose, -halfWidth, 0, -0.01F).color(COLOR_BACKGROUND).uv(0.0F, 0.0F).uv2(light).endVertex();
        builder.vertex(pose, -halfWidth, BAR_HEIGHT, -0.01F).color(COLOR_BACKGROUND).uv(0.0F, 1.0F).uv2(light).endVertex();
        builder.vertex(pose, halfWidth, BAR_HEIGHT, -0.01F).color(COLOR_BACKGROUND).uv(1.0F, 1.0F).uv2(light).endVertex();
        builder.vertex(pose, halfWidth, 0, -0.01F).color(COLOR_BACKGROUND).uv(1.0F, 0.0F).uv2(light).endVertex();

        if (filledWidth > 0) {
            builder.vertex(pose, -halfWidth, 0, -0.02F).color(barColor).uv(0.0F, 0.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth, BAR_HEIGHT, -0.02F).color(barColor).uv(0.0F, 1.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, BAR_HEIGHT, -0.02F).color(barColor).uv(1.0F, 1.0F).uv2(light).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, 0, -0.02F).color(barColor).uv(1.0F, 0.0F).uv2(light).endVertex();
        }

        poseStack.popPose();
    }

    private static int getBarColor(LivingEntity entity) {
        if (isHostile(entity)) {
            return COLOR_HOSTILE;
        }
        return COLOR_FRIENDLY;
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Monster) {
            return true;
        }
        return false;
    }
}