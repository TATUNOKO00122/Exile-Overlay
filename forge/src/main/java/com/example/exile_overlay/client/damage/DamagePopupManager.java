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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DamagePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupManager");
    private static final DamagePopupManager INSTANCE = new DamagePopupManager();

    private final List<DamageNumber> damageNumbers = new CopyOnWriteArrayList<>();
    private final Map<Integer, Float> lastHealthMap = new ConcurrentHashMap<>();
    
    private final Map<Integer, PlacementInfo> lastPlacementMap = new ConcurrentHashMap<>();
    private static final long PLACEMENT_TIMEOUT_MS = 500;
    private static final float OFFSET_INCREMENT = 0.4f;
    
    private final StringBuilder textBuilder = new StringBuilder(16);

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
        
        if (last != null && (now - last.timestamp()) < PLACEMENT_TIMEOUT_MS) {
            int newIndex = last.offsetIndex() + 1;
            double angle = newIndex * 45.0;
            double rad = Math.toRadians(angle);
            float offset = OFFSET_INCREMENT * ((newIndex / 8) + 1);
            
            double xOffset = Math.cos(rad) * offset;
            double zOffset = Math.sin(rad) * offset;
            double yOffset = (newIndex % 4) * 0.15;
            
            lastPlacementMap.put(entityId, new PlacementInfo(newIndex, now));
            return basePosition.add(xOffset, yOffset, zOffset);
        }
        
        PlacementInfo newPlacement = new PlacementInfo(0, now);
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

        for (DamageNumber dn : damageNumbers) {
            dn.tick();
            if (dn.isExpired()) {
                damageNumbers.remove(dn);
            }
        }
        
        cleanupOldPlacements();
        cleanupInvalidEntities(mc);
    }
    
    private void cleanupInvalidEntities(Minecraft mc) {
        if (mc.level == null) return;
        
        lastHealthMap.keySet().removeIf(entityId -> {
            var entity = mc.level.getEntity(entityId);
            return entity == null || !entity.isAlive();
        });
    }
    
    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        lastPlacementMap.entrySet().removeIf(entry -> 
            (now - entry.getValue().timestamp()) > PLACEMENT_TIMEOUT_MS * 2);
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
            String text = formatDamageText(dmgValue, dn.getType() == DamageType.HEALING);

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
    
    private String formatDamageText(float value, boolean isHealing) {
        textBuilder.setLength(0);
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (isHealing) {
            textBuilder.append('+');
        }

        if (config.isCompactNumbers()) {
            if (value >= 1_000_000f || value <= -1_000_000f) {
                appendCompact(textBuilder, value, 1_000_000, 'm');
            } else if (value >= 1_000f || value <= -1_000f) {
                appendCompact(textBuilder, value, 1_000, 'k');
            } else if (config.isRoundDamageNumbers()) {
                textBuilder.append(Math.round(value));
            } else if (value == Math.floor(value) && value < 100000) {
                textBuilder.append((int) value);
            } else {
                appendOneDecimal(textBuilder, value);
            }
        } else if (config.isRoundDamageNumbers()) {
            textBuilder.append(Math.round(value));
        } else {
            if (value == Math.floor(value) && value < 100000) {
                textBuilder.append((int) value);
            } else if (value < 1000) {
                appendOneDecimal(textBuilder, value);
            } else {
                textBuilder.append((int) Math.round(value));
            }
        }

        return textBuilder.toString();
    }

    private void appendCompact(StringBuilder sb, float value, int divisor, char suffix) {
        int intPart = (int) (value / divisor);
        int remainder = Math.abs((int) value) % divisor;
        int dec = remainder / (divisor / 10);
        if (dec > 0) {
            sb.append(intPart).append('.').append(dec).append(suffix);
        } else {
            sb.append(intPart).append(suffix);
        }
    }
    
    private void appendOneDecimal(StringBuilder sb, float value) {
        int intPart = (int) value;
        int decPart = Math.abs((int) ((value - intPart) * 10));
        sb.append(intPart).append('.').append(decPart);
    }
    
    private record PlacementInfo(int offsetIndex, long timestamp) {}
}