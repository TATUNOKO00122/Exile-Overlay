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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DamagePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupManager");
    private static DamagePopupManager instance;

    private final List<DamageNumber> damageNumbers = new ArrayList<>();
    private final Map<Integer, Float> lastHealthMap = new ConcurrentHashMap<>();
    
    // HJUD機能: 円形配置管理（重ならない配置用）
    private final Map<Integer, CircularPlacementInfo> circularPlacementMap = new HashMap<>();
    
    // HJUD機能: 配置情報のクリーンアップカウンター
    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100;

    private int physicsTickCounter = 0;
    private static final int PHYSICS_INTERVAL = 2;

    private DamagePopupManager() {
    }

    public static DamagePopupManager getInstance() {
        if (instance == null) {
            instance = new DamagePopupManager();
        }
        return instance;
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
        Minecraft mc = Minecraft.getInstance();

        if (!config.isShowPlayerDamage() && entity == mc.player) {
            return;
        }

        LOGGER.debug("Damage detected: {} to {}", damage, entity.getType().toShortString());

        int color = config.getNormalDamageColor();

        boolean isCrit = false;
        if (entity.getLastHurtByMob() instanceof Player player) {
            isCrit = player.getAttackStrengthScale(0.5f) > 0.9f;
            if (isCrit) {
                color = config.getCriticalDamageColor();
            }
        }

        // 肩あたりに表示
        Vec3 position = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
        addDamageNumber(position, damage, color, isCrit, DamageType.NORMAL, entity.getId());
    }

    private void onHeal(LivingEntity entity, float healAmount) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        if (!config.isShowHealing()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!config.isShowPlayerDamage() && entity == mc.player) {
            return;
        }

        LOGGER.debug("Heal detected: {} to {}", healAmount, entity.getType().toShortString());

        Vec3 position = entity.position().add(0, entity.getBbHeight() * 0.75, 0);
        addDamageNumber(position, healAmount, config.getHealingColor(), false, DamageType.HEALING, entity.getId());
    }

    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit, 
                                DamageType type, int entityId) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        LOGGER.info("Adding damage number: {} damage at {}, isCrit={}, type={}, entity={}", 
                   damage, position, isCrit, type, entityId);

        // HJUD機能: 円形配置処理（重ならないように配置）
        Vec3 displayPosition = calculateCircularPosition(position, entityId);

        // HJUD機能: ダメージスタッキング（近くのダメージを統合）
        if (config.isEnableDamageStacking()) {
            DamageNumber stacked = findStackableNumber(displayPosition, config.getStackingRadius());
            if (stacked != null) {
                stacked.addDamage(damage);
                stacked.resetLife();
                LOGGER.debug("Stacked damage with existing number, new total: {}", stacked.getDamage());
                return;
            }
        }

        // HJUD機能: 優先度付き削除
        if (damageNumbers.size() >= config.getMaxDamageTexts()) {
            removeLowPriorityNumber();
        }

        // コンボは不要なので comboCount = 0 固定
        damageNumbers.add(new DamageNumber(displayPosition, damage, color, isCrit, type, entityId, 0));
        LOGGER.debug("Damage number added. Total count: {}", damageNumbers.size());
    }

    // HJUD機能: 円形配置計算
    private Vec3 calculateCircularPosition(Vec3 basePosition, int entityId) {
        if (entityId <= 0) return basePosition;
        
        long currentTime = System.currentTimeMillis();
        CircularPlacementInfo placement = circularPlacementMap.get(entityId);
        
        // 500ms以上経過していたらリセット
        if (placement != null && (currentTime - placement.lastTime) > 500) {
            placement = null;
        }
        
        if (placement == null) {
            placement = new CircularPlacementInfo(currentTime);
            circularPlacementMap.put(entityId, placement);
            return basePosition;
        } else {
            // 円形に配置（高さも変化）
            double angleRadians = Math.toRadians(placement.nextAngleIndex * 45); // 45度ずつ
            float radius = 1.0f + (placement.circleLevel * 0.5f);
            
            float xOffset = (float) (Math.cos(angleRadians) * radius);
            float zOffset = (float) (Math.sin(angleRadians) * radius);
            float yOffset = (float) (Math.sin(angleRadians * 2) * 1.5);
            
            // 次の位置を更新
            placement.nextAngleIndex++;
            if (placement.nextAngleIndex >= 8) {
                placement.nextAngleIndex = 0;
                placement.circleLevel++;
                if (placement.circleLevel > 3) {
                    placement.circleLevel = 0;
                }
            }
            placement.lastTime = currentTime;
            
            return basePosition.add(xOffset, yOffset, zOffset);
        }
    }

    // HJUD機能: スタッキング可能なダメージを検索
    private DamageNumber findStackableNumber(Vec3 position, float radius) {
        for (DamageNumber num : damageNumbers) {
            if (num.getPosition().distanceTo(position) < radius && num.getLife() < 20) {
                return num;
            }
        }
        return null;
    }

    // HJUD機能: 優先度付き削除
    private void removeLowPriorityNumber() {
        if (damageNumbers.isEmpty()) return;

        DamageNumber candidate = null;
        float highestRemovalScore = -Float.MAX_VALUE;

        for (DamageNumber dn : damageNumbers) {
            // 削除スコア計算（高いほど削除されやすい）
            float score = dn.getLife();
            
            // クリティカルは削除されにくい
            if (dn.isCrit()) {
                score -= 10000;
            }
            
            // ダメージが大きいほど削除されにくい
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

    public void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }

        physicsTickCounter++;
        if (physicsTickCounter >= PHYSICS_INTERVAL) {
            physicsTickCounter = 0;
            applyPhysics();
        }

        Iterator<DamageNumber> it = damageNumbers.iterator();
        while (it.hasNext()) {
            DamageNumber dn = it.next();
            dn.tick();

            if (dn.isExpired()) {
                it.remove();
            }
        }
        
        // HJUD機能: 定期的なクリーンアップ
        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            cleanupCounter = 0;
            cleanupOldPlacements();
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

    // HJUD機能: 古い配置情報のクリーンアップ
    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        circularPlacementMap.entrySet().removeIf(entry -> (now - entry.getValue().lastTime) > 1000);
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

        LOGGER.debug("Rendering {} damage numbers", damageNumbers.size());

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
            // HJUDと同じ：y = 0 で描画
            float y = 0;

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
        circularPlacementMap.clear();
    }
    
    // HJUD機能: 円形配置情報クラス
    private static class CircularPlacementInfo {
        public int nextAngleIndex = 0;
        public int circleLevel = 0;
        public long lastTime;

        public CircularPlacementInfo(long time) {
            this.lastTime = time;
        }
    }
}
