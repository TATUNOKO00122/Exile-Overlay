package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.MobHealthBarConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.WeakHashMap;

/**
 * 3D HPバーレンダラー（湾曲したアーク形状）
 * 
 * HJUD MODのAngelRingRendererを参考に実装
 * 正確なビルボード回転とテクスチャ描画
 */
public class EntityHpBarRenderer {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityHpBarRenderer.class);
    
    // テクスチャ
    private static final ResourceLocation FRAME_TEXTURE = new ResourceLocation("exile_overlay", 
            "textures/gui/entity_hp_bar_frame.png");
    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", 
            "textures/misc/white.png");
    
    // デバッグ用
    private static int frameCount = 0;
    
    // 敵対判定キャッシュ
    private static final WeakHashMap<LivingEntity, Long> hostileCache = new WeakHashMap<>();
    private static final long HOSTILE_RETENTION_MS = 3000;
    
    // === 設定（HJUDと同じ） ===
    private static final float RADIUS = 0.8f;
    private static final float HEIGHT = 0.075f;
    private static final float THICKNESS = 0.025f;
    private static final float Z_OFFSET = 0.5f;
    private static final float HEIGHT_ABOVE_MOB = 0.5f;
    
    // アーク範囲（ラジアン）- HJUDと同じ
    private static final float START_ANGLE = (float) (Math.PI * 1.3);
    private static final float END_ANGLE = (float) (Math.PI * 1.7);
    private static final float ANGLE_PADDING = 0.03f;
    
    // テクスチャ設定
    private static final int TEX_WIDTH = 256;
    private static final int TEX_HEIGHT = 64;
    private static final int BAR_PIXEL_Y_START = 47;
    private static final int BAR_PIXEL_Y_END = 61;
    
    // LODセグメント数
    private static final int SEGMENTS_HIGH = 64;
    private static final int SEGMENTS_MID = 32;
    private static final int SEGMENTS_LOW = 16;
    
    // オブジェクトプール
    private static final ThreadLocal<Vector3f> NORMAL_POOL = ThreadLocal.withInitial(Vector3f::new);
    
    /**
     * HPバーをレンダリング
     * HJUDのEntityHpBarEventHandlerと同じ構造
     */
    public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
            Vec3 cameraPos, float cameraYaw, float partialTicks) {
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        
        // 設定を取得
        MobHealthBarConfig config = MobHealthBarConfig.getInstance();
        
        // ヘルスバー表示が無効の場合は何もしない
        if (!config.isShowHealthBar()) {
            return;
        }
        
        // スケール係数を計算（設定値に基づく）
        float scale = config.getScale();
        float baseWidth = config.getBarWidth();
        float baseHeight = config.getBarHeight();
        
        frameCount++;
        int renderedCount = 0;
        
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            
            // プレイヤーの表示設定をチェック
            if (living instanceof Player) {
                if (!config.isShowPlayerHealthBar()) {
                    continue;
                }
            }
            
            // 距離チェック
            double distanceSq = entity.distanceToSqr(cameraPos);
            if (distanceSq > 64 * 64) {
                continue;
            }
            
            // LODレベル
            int lodLevel = calculateLodLevel(distanceSq);
            
            // 位置計算（補間あり）
            double x = entity.xOld + (entity.getX() - entity.xOld) * partialTicks - cameraPos.x;
            double y = entity.yOld + (entity.getY() - entity.yOld) * partialTicks - cameraPos.y;
            double z = entity.zOld + (entity.getZ() - entity.zOld) * partialTicks - cameraPos.z;
            
            poseStack.pushPose();
            try {
                float entityHeight = living.getBbHeight();
                float verticalOffset = config.getVerticalOffset();
                float horizontalOffset = config.getHorizontalOffset();
                poseStack.translate(x + horizontalOffset, y + entityHeight + verticalOffset, z);
                
                // === HJUDと同じビルボード回転 ===
                // Billboard effect: Only rotate around Y axis (yaw only, no pitch)
                // This keeps the HP bar vertical and facing the player horizontally
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-cameraYaw));
                
                // Fix upside-down rendering after Y rotation
                poseStack.scale(1.0f, -1.0f, 1.0f);
                
                // Apply Z offset (push towards camera)
                poseStack.translate(0.0, 0.0, Z_OFFSET);
                
                // スケールを適用
                poseStack.scale(scale, scale, scale);
                
                // HP計算
                float health = living.getHealth();
                float maxHealth = living.getMaxHealth();
                float percentage = Math.min(1.0f, health / maxHealth);
                
                // カーブ率を取得
                float curveAmount = config.getCurveAmount();
                
                // 湾曲したHPバーを描画（設定値に基づくサイズとカーブ）
                renderCurvedHpBar(poseStack, bufferSource, percentage, lodLevel, baseWidth, baseHeight, curveAmount);
                
                renderedCount++;
                
            } catch (Exception e) {
                LOGGER.error("Failed to render HP bar for entity: {}", entity.getId(), e);
            } finally {
                poseStack.popPose();
            }
        }
        
        // バッファをフラッシュ
        bufferSource.endBatch();
    }
    
    /**
     * 湾曲したHPバーを描画（HJUDのAngelRingRendererと同じ）
     */
    private static void renderCurvedHpBar(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
            float healthPercentage, int lodLevel, float baseWidth, float baseHeight, float curveAmount) {
        
        // LODに基づいてセグメント数を決定
        int segments = getSegmentsForLod(lodLevel);
        
        // 設定値に基づいてサイズを計算（デフォルト20px幅、2.5px高さを基準）
        float widthScale = baseWidth / 20.0f;
        float heightScale = baseHeight / 2.5f;
        
        // カーブ率に基づいて角度を計算（0.0=直線、0.5=デフォルト、1.0=半円以上）
        float angleRange = (float) (Math.PI * 0.4 * curveAmount); // 0〜約72度
        float centerAngle = (float) (Math.PI * 1.5); // 270度（下向き）
        float startAngle = centerAngle - angleRange;
        float endAngle = centerAngle + angleRange;
        
        // 色を計算（鮮やかな色）
        float r, g, b;
        if (healthPercentage > 0.5f) {
            r = 0.3f;
            g = 1.0f;
            b = 0.4f; // 鮮やかな緑
        } else if (healthPercentage > 0.25f) {
            r = 1.0f;
            g = 1.0f;
            b = 0.3f; // 鮮やかな黄色
        } else {
            r = 1.0f;
            g = 0.3f;
            b = 0.3f; // 鮮やかな赤
        }
        float a = 1.0f;
        
        float border = 0.02f * widthScale;
        float baseRadius = RADIUS * widthScale;
        float offset = 0.001f;
        
        // フレームの高さを計算（高さスケール適用）
        float scaledHeight = HEIGHT * heightScale;
        float totalFrameHeight = (scaledHeight + 2 * border) * 2.0f;
        float midY = scaledHeight / 2.0f;
        float frameYMin = midY - (totalFrameHeight / 2.0f);
        float frameYMax = midY + (totalFrameHeight / 2.0f);
        
        // ピクセルスケール
        float pixelToWorldScale = totalFrameHeight / (float) TEX_HEIGHT;
        
        // HPバーのY位置
        float barYTop = frameYMin + (BAR_PIXEL_Y_START * pixelToWorldScale);
        float barYBottom = frameYMin + (BAR_PIXEL_Y_END * pixelToWorldScale);
        
        float scaledThickness = THICKNESS * heightScale;
        
        // 1. 背景アーク（グレー）
        drawArc(poseStack, buffer, WHITE_TEXTURE, 1.0f, 0.4f, 0.4f, 0.4f, 0.8f,
                0.5f, 0.5f,
                barYTop, barYBottom, baseRadius,
                startAngle, endAngle, segments, scaledThickness);
        
        // 2. HPアーク（色付き）
        drawArc(poseStack, buffer, WHITE_TEXTURE, healthPercentage, r, g, b, a,
                0.5f, 0.5f,
                barYTop, barYBottom, baseRadius + offset,
                startAngle, endAngle, segments, scaledThickness);
        
        // 3. フレーム（テクスチャ付き）
        drawArc(poseStack, buffer, FRAME_TEXTURE, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
                0.0f, 1.0f,
                frameYMin, frameYMax, baseRadius + offset * 2.0f,
                startAngle, endAngle, segments, scaledThickness);
    }
    
    /**
     * アークを描画（HJUDと同じ）
     */
    private static void drawArc(PoseStack poseStack, MultiBufferSource buffer,
            ResourceLocation texture,
            float percentage, float r, float g, float b, float a,
            float uMin, float uMax,
            float yMin, float yMax, float radius, float startAngle, float endAngle, int segments, float thickness) {
        
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.text(texture));
        
        Matrix4f matrix = poseStack.last().pose();
        var normalMatrix = poseStack.last().normal();
        
        float angleRange = endAngle - startAngle;
        
        // HPが右から左へ減少（HJUDと同じ）
        float actualStartAngle = endAngle - (angleRange * percentage);
        
        for (int i = 0; i < segments; i++) {
            float tCurrent = (float) i / segments;
            float tNext = (float) (i + 1) / segments;
            
            float angleCurrent = startAngle + angleRange * tCurrent;
            float angleNext = startAngle + angleRange * tNext;
            
            // HP範囲外をスキップ
            if (angleNext <= actualStartAngle) continue;
            if (angleCurrent < actualStartAngle) {
                angleCurrent = actualStartAngle;
                tCurrent = (angleCurrent - startAngle) / angleRange;
            }
            
            float u1 = uMin + (uMax - uMin) * tCurrent;
            float u2 = uMin + (uMax - uMin) * tNext;
            
            drawSegmentQuad(vertexConsumer, matrix, normalMatrix, r, g, b, a,
                    angleCurrent, angleNext, radius, yMin, yMax, thickness,
                    u1, u2);
        }
    }
    
    /**
     * セグメントの四角形を描画（HJUDと同じ）
     */
    private static void drawSegmentQuad(VertexConsumer builder, Matrix4f matrix, org.joml.Matrix3f normalMatrix,
            float r, float g, float b, float a,
            float angle1, float angle2,
            float radius, float yBottom, float yTop, float thickness,
            float u1, float u2) {
        
        float cos1 = (float) Math.cos(angle1);
        float sin1 = (float) Math.sin(angle1);
        float cos2 = (float) Math.cos(angle2);
        float sin2 = (float) Math.sin(angle2);
        
        float x1_out = cos1 * radius;
        float z1_out = sin1 * radius;
        float x2_out = cos2 * radius;
        float z2_out = sin2 * radius;
        
        float nx1 = cos1;
        float nz1 = sin1;
        float nx2 = cos2;
        float nz2 = sin2;
        
        // 外側面（反時計回り）
        vertex(builder, matrix, normalMatrix, x1_out, yBottom, z1_out, r, g, b, a, u1, 0.0f, nx1, 0, nz1);
        vertex(builder, matrix, normalMatrix, x2_out, yBottom, z2_out, r, g, b, a, u2, 0.0f, nx2, 0, nz2);
        vertex(builder, matrix, normalMatrix, x2_out, yTop, z2_out, r, g, b, a, u2, 1.0f, nx2, 0, nz2);
        vertex(builder, matrix, normalMatrix, x1_out, yTop, z1_out, r, g, b, a, u1, 1.0f, nx1, 0, nz1);
        
        // 裏面（時計回り）
        vertex(builder, matrix, normalMatrix, x1_out, yTop, z1_out, r, g, b, a, u1, 1.0f, -nx1, 0, -nz1);
        vertex(builder, matrix, normalMatrix, x2_out, yTop, z2_out, r, g, b, a, u2, 1.0f, -nx2, 0, -nz2);
        vertex(builder, matrix, normalMatrix, x2_out, yBottom, z2_out, r, g, b, a, u2, 0.0f, -nx2, 0, -nz2);
        vertex(builder, matrix, normalMatrix, x1_out, yBottom, z1_out, r, g, b, a, u1, 0.0f, -nx1, 0, -nz1);
    }
    
    private static void vertex(VertexConsumer builder, Matrix4f matrix, org.joml.Matrix3f normalMatrix,
            float x, float y, float z,
            float r, float g, float b, float a,
            float u, float v,
            float nx, float ny, float nz) {
        
        Vector3f normal = NORMAL_POOL.get();
        normal.set(nx, ny, nz);
        normal.mul(normalMatrix);
        
        builder.vertex(matrix, x, y, z)
                .color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(0, 10)
                .uv2(0xF000F0)
                .normal(normal.x, normal.y, normal.z)
                .endVertex();
    }
    
    private static int getSegmentsForLod(int lodLevel) {
        return switch (lodLevel) {
            case 0 -> SEGMENTS_HIGH;
            case 1 -> SEGMENTS_MID;
            default -> SEGMENTS_LOW;
        };
    }
    
    private static int calculateLodLevel(double distanceSq) {
        if (distanceSq <= 16 * 16) return 0;
        if (distanceSq <= 32 * 32) return 1;
        return 2;
    }
    
    private static boolean isHostile(LivingEntity entity) {
        boolean currentHostile = checkHostileCondition(entity);
        long now = System.currentTimeMillis();
        
        if (currentHostile) {
            hostileCache.put(entity, now);
            return true;
        }
        
        Long lastHostile = hostileCache.get(entity);
        if (lastHostile != null) {
            if (now - lastHostile < HOSTILE_RETENTION_MS) {
                return true;
            } else {
                hostileCache.remove(entity);
            }
        }
        
        return false;
    }
    
    private static boolean checkHostileCondition(LivingEntity entity) {
        if (entity instanceof Enemy) return true;
        if (entity instanceof Mob mob && mob.isAggressive()) return true;
        if (entity instanceof NeutralMob neutralMob) {
            Player player = Minecraft.getInstance().player;
            if (player != null && neutralMob.isAngryAt(player)) return true;
        }
        if (entity instanceof Mob mob && mob.getTarget() == Minecraft.getInstance().player) return true;
        if (entity.getLastHurtByMob() == Minecraft.getInstance().player) return true;
        return false;
    }
}
