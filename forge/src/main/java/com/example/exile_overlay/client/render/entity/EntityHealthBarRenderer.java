package com.example.exile_overlay.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.render.ailment.ClientAilmentTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EntityHealthBarRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/EntityHealthBarRenderer");
    private static final EntityHealthBarConfig CONFIG = EntityHealthBarConfig.getInstance();

    private static final float BASE_SCALE = 0.0267F;
    private static final int COLOR_BACKGROUND = 0x7F401010;

    private EntityHealthBarRenderer() {}

    public static void hookRender(Entity entity, PoseStack poseStack, MultiBufferSource buffers,
            Camera camera, EntityRenderer<? super Entity> entityRenderer,
            float partialTicks, double x, double y, double z) {
        // 3D HPBar無効化のためコメントアウト
        /*
        EntityHealthBarConfig config = CONFIG;
        if (!config.isEnabled() || camera == null || camera.getEntity() == null || entityRenderer == null) {
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
        */
    }

    private static boolean isHostile(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof WitherBoss) return true;
        if (entity instanceof EnderDragon) return true;
        return false;
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

        if (!EntityHealthBarTimer.getInstance().shouldShow(living, config.getDisplayDuration())) {
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

        if (cameraEntity instanceof Player player && !player.hasLineOfSight(living)) {
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

        var vec3 = entityRenderer.getRenderOffset(living, partialTicks);
        double d0 = x + vec3.x();
        double d1 = y + vec3.y();
        double d2 = z + vec3.z();

        int barWidth = config.getBarWidth();
        int barHeight = config.getBarHeight();
        float scale = BASE_SCALE * config.getScale();

        int colorHealth = (config.isShowFriendlyColor() && !isHostile(living))
                ? config.getFriendlyBarColorHex(0xCC)
                : config.getHealthBarColorHex(0xCC);

        poseStack.pushPose();
        poseStack.translate(d0, d1, d2);
        poseStack.translate(0, living.getBbHeight() + config.getHeightAbove(), 0);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-scale, -scale, scale);

        float maxHp = living.getMaxHealth();
        float health = living.getHealth();
        if (MethodHandlesUtil.isAvailable()) {
            try {
                health = MethodHandlesUtil.getCurrentHealth(living);
            } catch (Throwable t) {
                health = living.getHealth();
            }
            try {
                maxHp = MethodHandlesUtil.getMaxHealth(living);
            } catch (Throwable t) {
                maxHp = living.getMaxHealth();
            }
        }

        float hpRatio = (maxHp > 0.0F && !Float.isNaN(health) && !Float.isNaN(maxHp))
                ? Math.min(Math.max(health / maxHp, 0.0F), 1.0F) : 0.0F;

        boolean isPoisoned = config.isShowPoison() && ClientAilmentTracker.getInstance().isPoisoned(living);
        int effectiveColorHealth = isPoisoned ? config.getPoisonBarColorHex(0xCC) : colorHealth;

        float bloodLossEndRatio = config.isShowBleed()
                ? ClientAilmentTracker.getInstance().getBloodLossEndRatio(living, hpRatio, maxHp)
                : hpRatio;
        int colorBloodLoss = config.getBleedBarColorHex(0xCC);

        float halfWidth = barWidth / 2.0F;
        float filledWidth = barWidth * hpRatio;
        float bloodEndWidth = barWidth * bloodLossEndRatio;

        VertexConsumer builder = buffers.getBuffer(HealthBarRenderType.BAR_TYPE);

        var pose = poseStack.last().pose();
        int packedLight = 15728880;

        // 1. 背景描画
        if (filledWidth < barWidth) {
            float bgStart = -halfWidth + filledWidth;
            builder.vertex(pose, bgStart, 0, 0.0F).color(COLOR_BACKGROUND).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, bgStart, barHeight, 0.0F).color(COLOR_BACKGROUND).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, halfWidth, barHeight, 0.0F).color(COLOR_BACKGROUND).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, halfWidth, 0, 0.0F).color(COLOR_BACKGROUND).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        }

        // 2. 出血（失血）蓄積セグメント描画 (暗赤色)
        if (bloodEndWidth > filledWidth) {
            float bStart = -halfWidth + filledWidth;
            float bEnd = -halfWidth + bloodEndWidth;
            builder.vertex(pose, bStart, 0, 0.0F).color(colorBloodLoss).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, bStart, barHeight, 0.0F).color(colorBloodLoss).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, bEnd, barHeight, 0.0F).color(colorBloodLoss).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, bEnd, 0, 0.0F).color(colorBloodLoss).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        }

        // 3. 現在HPバー描画 (通常赤 / 毒時深緑)
        if (filledWidth > 0) {
            builder.vertex(pose, -halfWidth, 0, 0.0F).color(effectiveColorHealth).uv(0.0F, 0.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, -halfWidth, barHeight, 0.0F).color(effectiveColorHealth).uv(0.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, barHeight, 0.0F).color(effectiveColorHealth).uv(1.0F, 1.0F).uv2(packedLight).endVertex();
            builder.vertex(pose, -halfWidth + filledWidth, 0, 0.0F).color(effectiveColorHealth).uv(1.0F, 0.0F).uv2(packedLight).endVertex();
        }

        poseStack.popPose();
    }
}