package com.example.exile_overlay.client.damage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Phase 2: スレッド安全性を確保したDamageRenderer
 * 
 * 設計原則:
 * - ゲームスレッド（onClientTick）とレンダースレッド（onRenderWorld）の分離
 * - ConcurrentLinkedQueueによる安全なデータ転送
 * - CopyOnWriteArrayListによるレンダースレッドでの安全な読み取り
 */
public class ThreadSafeDamageRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/ThreadSafeDamageRenderer");
    private static ThreadSafeDamageRenderer instance;
    
    // ゲームスレッド → レンダースレッドへの安全な転送
    private final Queue<DamageNumber> pendingNumbers = new ConcurrentLinkedQueue<>();
    
    // レンダースレッド専用（読み取りのみ）
    private final List<DamageNumber> renderNumbers = new CopyOnWriteArrayList<>();
    
    // ゲームスレッド専用（書き込みのみ）
    private final Map<Long, Integer> comboMap = new ConcurrentHashMap<>();
    private final Map<Long, Long> comboTimeMap = new ConcurrentHashMap<>();
    private final Map<Long, CircularPlacementInfo> circularPlacementMap = new ConcurrentHashMap<>();
    
    private int cleanupCounter = 0;
    private static final int CLEANUP_INTERVAL = 100;
    
    // Phase 1: 物理演算の間引き
    private int physicsTickCounter = 0;
    private static final int PHYSICS_INTERVAL = 3;
    
    // Phase 3: 空間分割グリッド
    private final SpatialGrid spatialGrid = new SpatialGrid();
    
    private static class CircularPlacementInfo {
        public int nextAngleIndex = 0;
        public int circleLevel = 0;
        public long lastTime;
        
        public CircularPlacementInfo(long time) {
            this.lastTime = time;
        }
    }
    
    private ThreadSafeDamageRenderer() {
    }
    
    public static ThreadSafeDamageRenderer getInstance() {
        if (instance == null) {
            instance = new ThreadSafeDamageRenderer();
        }
        return instance;
    }
    
    /**
     * ゲームスレッドから呼び出し - ダメージ数字を追加
     */
    public void addDamageNumber(net.minecraft.world.phys.Vec3 position, float damage, int color, boolean isCrit) {
        addDamageNumber(position, damage, color, isCrit, DamageType.NORMAL, 0);
    }
    
    /**
     * ゲームスレッドから呼び出し - ダメージ数字を追加（詳細版）
     */
    public void addDamageNumber(net.minecraft.world.phys.Vec3 position, float damage, int color, boolean isCrit, 
            DamageType type, long entityId) {
        try {
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
            
            net.minecraft.world.phys.Vec3 displayPosition = calculateDisplayPosition(position, entityId);
            
            if (config.isEnableDamageStacking()) {
                DamageNumber stacked = findStackableNumber(displayPosition, config.getStackingRadius());
                if (stacked != null) {
                    stacked.addDamage(damage);
                    stacked.resetLife();
                    return;
                }
            }
            
            // キューに追加（スレッド安全）
            pendingNumbers.offer(new DamageNumber(displayPosition, damage, color, isCrit, type, comboCount));
            
        } catch (Exception e) {
            LOGGER.error("Failed to add damage number", e);
        }
    }
    
    private net.minecraft.world.phys.Vec3 calculateDisplayPosition(net.minecraft.world.phys.Vec3 position, long entityId) {
        if (entityId <= 0) {
            return position;
        }
        
        long currentTime = System.currentTimeMillis();
        CircularPlacementInfo placement = circularPlacementMap.get(entityId);
        
        if (placement != null && (currentTime - placement.lastTime) > 500) {
            placement = null;
        }
        
        if (placement == null) {
            placement = new CircularPlacementInfo(currentTime);
            circularPlacementMap.put(entityId, placement);
            return position;
        }
        
        double angleRadians = Math.toRadians(placement.nextAngleIndex * 45);
        float radius = 1.0f + (placement.circleLevel * 0.5f);
        
        float xOffset = (float) (Math.cos(angleRadians) * radius);
        float zOffset = (float) (Math.sin(angleRadians) * radius);
        float yOffset = (float) (Math.sin(angleRadians * 2) * 1.5);
        
        net.minecraft.world.phys.Vec3 result = position.add(xOffset, yOffset, zOffset);
        
        placement.nextAngleIndex++;
        if (placement.nextAngleIndex >= 8) {
            placement.nextAngleIndex = 0;
            placement.circleLevel++;
            if (placement.circleLevel > 3) {
                placement.circleLevel = 0;
            }
        }
        placement.lastTime = currentTime;
        
        return result;
    }
    
    private DamageNumber findStackableNumber(net.minecraft.world.phys.Vec3 position, float radius) {
        for (DamageNumber num : renderNumbers) {
            if (num.getPosition().distanceTo(position) < radius && num.getLife() < 20) {
                return num;
            }
        }
        return null;
    }
    
    /**
     * ゲームスレッドから呼び出し（ClientTick）
     */
    public void onClientTick() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        
        try {
            // 物理演算の間引き
            physicsTickCounter++;
            if (physicsTickCounter >= PHYSICS_INTERVAL) {
                physicsTickCounter = 0;
                applyPhysics();
            }
            
            // 寿命更新
            Iterator<DamageNumber> it = renderNumbers.iterator();
            while (it.hasNext()) {
                DamageNumber damage = it.next();
                damage.tick();
                
                if (damage.isExpired()) {
                    it.remove();
                }
            }
            
            // クリーンアップ
            cleanupCounter++;
            if (cleanupCounter >= CLEANUP_INTERVAL) {
                cleanupCounter = 0;
                cleanupOldPlacements();
            }
            
        } catch (Exception e) {
            LOGGER.error("Error in client tick", e);
        }
    }
    
    /**
     * レンダースレッドから呼び出し（WorldRender）
     */
    public void onRenderWorld(com.mojang.blaze3d.vertex.PoseStack poseStack) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        
        if (!DamagePopupConfig.getInstance().isShowDamage()) {
            return;
        }
        
        try {
            // キューからレンダーリストに移動（スレッド安全）
            syncPendingNumbers();
            
            // 描画
            renderAllDamageNumbers(poseStack, mc);
            
        } catch (Exception e) {
            LOGGER.error("Error in render world", e);
        }
    }
    
    /**
     * キューからレンダーリストに安全に転送
     */
    private void syncPendingNumbers() {
        DamageNumber num;
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        
        while ((num = pendingNumbers.poll()) != null) {
            // 表示数制限チェック
            if (renderNumbers.size() >= config.getMaxDamageTexts()) {
                removeLowPriorityNumber();
            }
            renderNumbers.add(num);
        }
    }
    
    private void removeLowPriorityNumber() {
        if (renderNumbers.isEmpty()) {
            return;
        }
        
        DamageNumber candidate = null;
        float highestRemovalScore = -Float.MAX_VALUE;
        
        for (DamageNumber dn : renderNumbers) {
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
            renderNumbers.remove(candidate);
        } else {
            renderNumbers.remove(0);
        }
    }
    
    /**
     * Phase 3: 空間分割によるO(N)物理演算
     */
    private void applyPhysics() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        float radius = config.getRepulsionRadius();
        float strength = config.getRepulsionStrength();
        
        try {
            // 1. グリッド構築（O(N)）
            spatialGrid.clear();
            for (DamageNumber num : renderNumbers) {
                spatialGrid.insert(num);
            }
            
            // 2. 隣接セルのみをチェック（O(N)）
            Set<Long> processedPairs = new HashSet<>();
            
            for (DamageNumber d1 : renderNumbers) {
                Set<DamageNumber> neighbors = spatialGrid.getNeighbors(d1);
                
                for (DamageNumber d2 : neighbors) {
                    // 重複チェック防止
                    long pairKey = Math.min(d1.hashCode(), d2.hashCode()) * 31L + 
                                   Math.max(d1.hashCode(), d2.hashCode());
                    if (processedPairs.contains(pairKey)) {
                        continue;
                    }
                    processedPairs.add(pairKey);
                    
                    double distSq = d1.getPosition().distanceToSqr(d2.getPosition());
                    double combinedRadius = radius * 2;
                    
                    if (distSq < combinedRadius * combinedRadius && distSq > 0.0001) {
                        double dist = Math.sqrt(distSq);
                        double force = strength * (1.0 - dist / combinedRadius);
                        
                        net.minecraft.world.phys.Vec3 dir = d1.getPosition()
                                .subtract(d2.getPosition()).normalize();
                        net.minecraft.world.phys.Vec3 forceVec = dir.scale(force);
                        
                        d1.setVelocity(d1.getVelocity().add(forceVec));
                        d2.setVelocity(d2.getVelocity().subtract(forceVec));
                    }
                }
            }
            
            // デバッグログ（開発時のみ有効化）
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Physics applied: {} objects in {} cells", 
                        spatialGrid.getTotalObjectCount(), spatialGrid.getCellCount());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error in physics calculation", e);
        }
    }
    
    private void cleanupOldPlacements() {
        long now = System.currentTimeMillis();
        circularPlacementMap.entrySet().removeIf(entry -> (now - entry.getValue().lastTime) > 1000);
        comboTimeMap.entrySet().removeIf(entry -> (now - entry.getValue()) > 5000);
        comboMap.keySet().removeIf(key -> !comboTimeMap.containsKey(key));
    }
    
    private void renderAllDamageNumbers(com.mojang.blaze3d.vertex.PoseStack poseStack, 
            net.minecraft.client.Minecraft mc) {
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        com.mojang.blaze3d.systems.RenderSystem.disableCull();
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        
        com.mojang.blaze3d.systems.RenderSystem.polygonOffset(-1.0f, -1.0f);
        com.mojang.blaze3d.systems.RenderSystem.enablePolygonOffset();
        
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        for (DamageNumber damage : renderNumbers) {
            renderDamageNumber(poseStack, bufferSource, damage, mc);
        }
        
        bufferSource.endBatch();
        
        com.mojang.blaze3d.systems.RenderSystem.disablePolygonOffset();
        com.mojang.blaze3d.systems.RenderSystem.polygonOffset(0.0f, 0.0f);
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
        com.mojang.blaze3d.systems.RenderSystem.enableCull();
    }
    
    private void renderDamageNumber(com.mojang.blaze3d.vertex.PoseStack poseStack, 
            net.minecraft.client.renderer.MultiBufferSource bufferSource, 
            DamageNumber damage, net.minecraft.client.Minecraft mc) {
        
        net.minecraft.world.phys.Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        
        poseStack.pushPose();
        try {
            net.minecraft.world.phys.Vec3 pos = damage.getPosition();
            poseStack.translate(
                    pos.x - camPos.x,
                    pos.y - camPos.y,
                    pos.z - camPos.z);
            
            poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            
            float scale = damage.getScale();
            poseStack.scale(-scale, -scale, scale);
            
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
