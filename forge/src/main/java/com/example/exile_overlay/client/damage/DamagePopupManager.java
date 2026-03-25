package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DamagePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupManager");
    private static final DamagePopupManager INSTANCE = new DamagePopupManager();

    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final Map<Integer, Float> lastHealthMap = new ConcurrentHashMap<>();
    
    private final Map<Integer, PlacementInfo> lastPlacementMap = new ConcurrentHashMap<>();
    private static final long PLACEMENT_TIMEOUT_MS = 500;
    private static final float OFFSET_INCREMENT = 0.4f;

    private DamagePopupManager() {
    }

    public static DamagePopupManager getInstance() {
        return INSTANCE;
    }

    public void onHealthChanged(LivingEntity entity, float newHealth) {
        try {
            if (entity == null || !entity.level().isClientSide()) {
                return;
            }

            int entityId = entity.getId();
            Float lastHealth = lastHealthMap.get(entityId);

            if (lastHealth == null) {
                lastHealthMap.put(entityId, newHealth);
                return;
            }

            float diff = lastHealth - newHealth;

            if (diff > 0.1f) {
                onDamage(entity, diff);
            } else if (diff < -0.1f) {
                onHeal(entity, -diff);
            }

            lastHealthMap.put(entityId, newHealth);
        } catch (Exception e) {
            LOGGER.error("Failed to handle health change", e);
        }
    }

    private void onDamage(LivingEntity entity, float damage) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (!config.isShowPlayerDamage() && entity instanceof Player) {
            return;
        }

        int color = config.getNormalDamageColor();

        boolean isCrit = false;
        if (entity.getLastHurtByMob() instanceof Player player) {
            isCrit = player.getAttackStrengthScale(0.5f) > 0.9f;
            if (isCrit) {
                color = config.getCriticalDamageColor();
            }
        }

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        addDamageNumber(basePosition, damage, color, isCrit, DamageType.NORMAL, entity.getId());
    }

    private void onHeal(LivingEntity entity, float healAmount) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        if (!config.isShowHealing()) {
            return;
        }

        if (entity instanceof Player && !config.isShowPlayerHealing()) {
            return;
        }

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        addDamageNumber(basePosition, healAmount, config.getHealingColor(), false, DamageType.HEALING, entity.getId());
    }

    private Vec3 calculateNonOverlappingPosition(Vec3 basePosition, int entityId) {
        long now = System.currentTimeMillis();
        PlacementInfo last = lastPlacementMap.get(entityId);
        
        if (last != null && (now - last.timestamp) < PLACEMENT_TIMEOUT_MS) {
            last.offsetIndex++;
            double angle = last.offsetIndex * 45.0;
            double rad = Math.toRadians(angle);
            float offset = OFFSET_INCREMENT * ((last.offsetIndex / 8) + 1);
            
            double xOffset = Math.cos(rad) * offset;
            double zOffset = Math.sin(rad) * offset;
            double yOffset = (last.offsetIndex % 4) * 0.15;
            
            last.timestamp = now;
            return basePosition.add(xOffset, yOffset, zOffset);
        }
        
        PlacementInfo newPlacement = new PlacementInfo(now);
        lastPlacementMap.put(entityId, newPlacement);
        return basePosition;
    }

    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit, 
                                DamageType type, int entityId) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (damageNumbers.size() >= config.getMaxDamageTexts()) {
            damageNumbers.remove(0);
        }

        Vec3 finalPosition = calculateNonOverlappingPosition(position, entityId);
        damageNumbers.add(new DamageNumber(finalPosition, damage, color, isCrit, type, entityId, 0));
    }

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        Iterator<DamageNumber> it = damageNumbers.iterator();
        while (it.hasNext()) {
            DamageNumber dn = it.next();
            dn.tick();

            if (dn.isExpired()) {
                it.remove();
            }
        }
        
        cleanupOldPlacements();
    }
    
    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        lastPlacementMap.entrySet().removeIf(entry -> 
            (now - entry.getValue().timestamp) > PLACEMENT_TIMEOUT_MS * 2);
    }

    public void onRenderWorld(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!DamagePopupConfig.getInstance().isShowDamage()) {
            return;
        }

        if (damageNumbers.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.polygonOffset(-1.0f, -1.0f);
        RenderSystem.enablePolygonOffset();

        for (DamageNumber dn : damageNumbers) {
            renderDamageNumber(poseStack, bufferSource, dn);
        }

        bufferSource.endBatch();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void renderDamageNumber(PoseStack poseStack, MultiBufferSource bufferSource, DamageNumber dn) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        poseStack.pushPose();
        try {
            Vec3 pos = dn.getPosition();
            double dx = pos.x - camPos.x;
            double dy = pos.y - camPos.y;
            double dz = pos.z - camPos.z;
            poseStack.translate(dx, dy, dz);

            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

            float scale = dn.getScale();
            poseStack.scale(-scale, -scale, scale);

            int alpha = (int) (dn.getAlpha() * 255);
            int colorWithAlpha = (alpha << 24) | (dn.getDisplayColor() & 0xFFFFFF);

            float dmgValue = dn.getDamage();
            String text;
            if (dmgValue == Math.floor(dmgValue)) {
                text = String.format("%.0f", dmgValue);
            } else {
                text = String.format("%.1f", dmgValue);
            }

            if (dn.getType() == DamageType.HEALING) {
                text = "+" + text;
            }

            float textWidth = mc.font.width(text);
            float x = -textWidth / 2.0f;
            float centerY = mc.font.lineHeight / 2.0f;
            float y = -centerY;

            if (config.isEnableShadow()) {
                int shadowColor = ((int) (alpha * 0.5f) << 24) | 0x000000;
                DamageFontRenderer.renderText(poseStack, text, x + 1.0f, y + 1.0f, 
                    shadowColor, bufferSource, 0xF000F0, scale);
            }

            DamageFontRenderer.renderText(poseStack, text, x, y, colorWithAlpha, 
                bufferSource, 0xF000F0, scale);

        } finally {
            poseStack.popPose();
        }
    }

    public void clear() {
        damageNumbers.clear();
        lastHealthMap.clear();
        lastPlacementMap.clear();
    }
    
    private static class PlacementInfo {
        int offsetIndex = 0;
        long timestamp;
        
        PlacementInfo(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}