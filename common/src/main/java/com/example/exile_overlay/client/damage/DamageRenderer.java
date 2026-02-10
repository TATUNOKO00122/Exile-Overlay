package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DamageRenderer {
    private static DamageRenderer instance;
    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final Map<Long, Integer> comboMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> comboTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, CircularPlacementInfo> circularPlacementMap = new ConcurrentHashMap<>();

    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100;
    
    // Phase 1: 物理演算の間引き
    private int physicsTickCounter = 0;
    private static final int PHYSICS_INTERVAL = 3; // 3ティックに1回実行

    private static class CircularPlacementInfo {
        public int nextAngleIndex = 0;
        public int circleLevel = 0;
        public long lastTime;

        public CircularPlacementInfo(long time) {
            this.lastTime = time;
        }
    }

    private DamageRenderer() {
    }

    public static DamageRenderer getInstance() {
        if (instance == null) {
            instance = new DamageRenderer();
        }
        return instance;
    }

    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit) {
        addDamageNumber(position, damage, color, isCrit, DamageType.NORMAL, 0);
    }

    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit, DamageType type,
            long entityId) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        int comboCount = 0;
        if (entityId > 0) {
            long currentTime = System.currentTimeMillis();
            Long lastHitTime = comboTimeMap.get(entityId);

            if (lastHitTime != null && (currentTime - lastHitTime) < config.getComboTimeWindow() * 50) {
                comboCount = comboMap.getOrDefault(entityId, 0) + 1;
            } else {
                comboCount = 1;
            }

            comboMap.put(entityId, comboCount);
            comboTimeMap.put(entityId, currentTime);
        }

        Vec3 displayPosition = position;
        if (entityId > 0) {
            long currentTime = System.currentTimeMillis();
            CircularPlacementInfo placement = circularPlacementMap.get(entityId);

            if (placement != null && (currentTime - placement.lastTime) > 500) {
                placement = null;
            }

            if (placement == null) {
                placement = new CircularPlacementInfo(currentTime);
                circularPlacementMap.put(entityId, placement);
            } else {
                double angleRadians = Math.toRadians(placement.nextAngleIndex * 45);
                float radius = 1.0f + (placement.circleLevel * 0.5f);

                float xOffset = (float) (Math.cos(angleRadians) * radius);
                float zOffset = (float) (Math.sin(angleRadians) * radius);
                float yOffset = (float) (Math.sin(angleRadians * 2) * 1.5);

                displayPosition = position.add(xOffset, yOffset, zOffset);

                placement.nextAngleIndex++;
                if (placement.nextAngleIndex >= 8) {
                    placement.nextAngleIndex = 0;
                    placement.circleLevel++;
                    if (placement.circleLevel > 3) {
                        placement.circleLevel = 0;
                    }
                }
                placement.lastTime = currentTime;
            }
        }

        if (config.isEnableDamageStacking()) {
            DamageNumber stacked = findStackableNumber(displayPosition, config.getStackingRadius());
            if (stacked != null) {
                stacked.addDamage(damage);
                stacked.resetLife();
                return;
            }
        }

        if (damageNumbers.size() >= config.getMaxDamageTexts()) {
            removeLowPriorityNumber();
        }

        damageNumbers.add(new DamageNumber(displayPosition, damage, color, isCrit, type, comboCount));
    }

    private void removeLowPriorityNumber() {
        if (damageNumbers.isEmpty()) {
            return;
        }

        DamageNumber candidate = null;
        float highestRemovalScore = -Float.MAX_VALUE;

        for (DamageNumber dn : damageNumbers) {
            float score = dn.getLife();

            if (dn.isCrit()) {
                score -= 10000;
            }

            score -= Math.min(dn.getDamage(), 1000);

            if (score > highestRemovalScore) {
                highestRemovalScore = score;
                candidate = dn;
            }
        }

        if (candidate != null) {
            damageNumbers.remove(candidate);
        } else {
            damageNumbers.remove(0);
        }
    }

    private void applyPhysics() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        float radius = config.getRepulsionRadius();
        float strength = config.getRepulsionStrength();

        for (int i = 0; i < damageNumbers.size(); i++) {
            DamageNumber d1 = damageNumbers.get(i);

            for (int j = i + 1; j < damageNumbers.size(); j++) {
                DamageNumber d2 = damageNumbers.get(j);

                double distSq = d1.getPosition().distanceToSqr(d2.getPosition());
                double combinedRadius = radius * 2;

                if (distSq < combinedRadius * combinedRadius && distSq > 0.0001) {
                    double dist = Math.sqrt(distSq);
                    double force = strength * (1.0 - dist / combinedRadius);

                    Vec3 dir = d1.getPosition().subtract(d2.getPosition()).normalize();
                    Vec3 forceVec = dir.scale(force);

                    d1.setVelocity(d1.getVelocity().add(forceVec));
                    d2.setVelocity(d2.getVelocity().subtract(forceVec));
                }
            }
        }
    }

    private DamageNumber findStackableNumber(Vec3 position, float radius) {
        for (DamageNumber num : damageNumbers) {
            if (num.getPosition().distanceTo(position) < radius && num.getLife() < 20) {
                return num;
            }
        }
        return null;
    }

    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        circularPlacementMap.entrySet().removeIf(entry -> (now - entry.getValue().lastTime) > 1000);
        comboTimeMap.entrySet().removeIf(entry -> (now - entry.getValue()) > 5000);
        comboMap.keySet().removeIf(key -> !comboTimeMap.containsKey(key));
    }

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        // Phase 1: 物理演算の間引き（3ティックに1回）
        physicsTickCounter++;
        if (physicsTickCounter >= PHYSICS_INTERVAL) {
            physicsTickCounter = 0;
            applyPhysics();
        }

        Iterator<DamageNumber> it = damageNumbers.iterator();
        while (it.hasNext()) {
            DamageNumber damage = it.next();
            damage.tick();

            if (damage.isExpired()) {
                it.remove();
            }
        }

        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupOldPlacements();
        }
    }

    public void onRenderWorld(PoseStack poseStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        if (!DamagePopupConfig.getInstance().isShowDamage()) {
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

        for (DamageNumber damage : damageNumbers) {
            renderDamageNumber(poseStack, bufferSource, damage);
        }

        bufferSource.endBatch();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void renderDamageNumber(PoseStack poseStack, MultiBufferSource bufferSource, DamageNumber damage) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        poseStack.pushPose();
        try {
            Vec3 pos = damage.getPosition();
            poseStack.translate(
                    pos.x - camPos.x,
                    pos.y - camPos.y,
                    pos.z - camPos.z);

            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

            float scale = damage.getScale();
            poseStack.scale(-scale, scale, scale);

            int alpha = (int) (damage.getAlpha() * 255);
            int colorWithAlpha = (alpha << 24) | (damage.getColor() & 0xFFFFFF);

            float dmgValue = damage.getDamage();
            String text;
            if (dmgValue == Math.floor(dmgValue)) {
                text = String.format("%.0f", dmgValue);
            } else {
                text = String.format("%.1f", dmgValue);
            }

            float textWidth = mc.font.width(text);
            float x = -textWidth / 2.0f;
            float y = 0;

            if (config.isEnableShadow()) {
                int shadowColor = ((int) (alpha * 0.5f * 255) << 24) | 0x000000;
                DamageFontRenderer.renderText(
                        poseStack,
                        text,
                        x + 1.0f,
                        y + 1.0f,
                        shadowColor,
                        bufferSource,
                        0xF000F0);
            }

            DamageFontRenderer.renderText(
                    poseStack,
                    text,
                    x,
                    y,
                    colorWithAlpha,
                    bufferSource,
                    0xF000F0);

        } finally {
            poseStack.popPose();
        }
    }
}
