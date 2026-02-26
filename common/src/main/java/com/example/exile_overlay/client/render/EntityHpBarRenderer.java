package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.MobHealthBarConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
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

                // カーブ率に基づいて角度を計算
                float angleRange = (float) (Math.PI * 0.4 * curveAmount);
                float centerAngle = (float) (Math.PI * 1.5);

                // 湾曲したHPバーを描画（設定値に基づくサイズとカーブ）
                renderCurvedHpBar(poseStack, bufferSource, percentage, lodLevel, baseWidth, baseHeight, curveAmount);

                // HP数値テキストを描画（湾曲）
                float widthScale = baseWidth / 20.0f;
                float heightScale = baseHeight / 2.5f;
                float baseRadius = RADIUS * widthScale;
                float border = 0.02f * widthScale;
                float scaledHeight = HEIGHT * heightScale;
                float totalFrameHeight = (scaledHeight + 2 * border) * 2.0f;
                float midY = scaledHeight / 2.0f;
                float frameYMin = midY - (totalFrameHeight / 2.0f);
                float pixelToWorldScale = totalFrameHeight / (float) TEX_HEIGHT;
                float barYTop = frameYMin + (BAR_PIXEL_Y_START * pixelToWorldScale);

                renderHealthText(poseStack, bufferSource, health, maxHealth, percentage, config,
                        baseRadius, centerAngle, angleRange, barYTop, heightScale);

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

        // 描画する全体の角度範囲
        float angleRange = endAngle - startAngle;

        // 右が最大値（＝左から右へ増加）。HPが減ると右側から空になっていく
        // angleは小さい方(startAngle=右)から大きい方(endAngle=左)へ進むため、
        // HPの割合に応じて左側（大きい角度）を残す。
        float actualStartAngle = endAngle - (angleRange * percentage);

        for (int i = 0; i < segments; i++) {
            float tCurrent = (float) i / segments;
            float tNext = (float) (i + 1) / segments;

            // 角度は左端(startAngle)から右端(endAngle)へ進む
            float angleCurrent = startAngle + angleRange * tCurrent;
            float angleNext = startAngle + angleRange * tNext;

            // HP範囲外（空になった右側）をスキップ
            if (angleNext <= actualStartAngle)
                continue;

            // 境界をまたぐセグメントはHPの現在値でぶつ切りにする
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
        if (distanceSq <= 16 * 16)
            return 0;
        if (distanceSq <= 32 * 32)
            return 1;
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
        if (entity instanceof Enemy)
            return true;
        if (entity instanceof Mob mob && mob.isAggressive())
            return true;
        if (entity instanceof NeutralMob neutralMob) {
            Player player = Minecraft.getInstance().player;
            if (player != null && neutralMob.isAngryAt(player))
                return true;
        }
        if (entity instanceof Mob mob && mob.getTarget() == Minecraft.getInstance().player)
            return true;
        if (entity.getLastHurtByMob() == Minecraft.getInstance().player)
            return true;
        return false;
    }

    /**
     * HP数値テキストを描画（湾曲バージョン）
     * HPバーの円弧に沿って表示
     */
    private static void renderHealthText(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
            float health, float maxHealth, float percentage, MobHealthBarConfig config,
            float radius, float centerAngle, float angleRange, float barYTop, float heightScale) {

        boolean showValue = config.isShowHealthValue();
        boolean showMax = config.isShowMaxHealth();
        boolean showPercent = config.isShowPercentage();

        if (!showValue && !showMax && !showPercent) {
            return;
        }

        String text = formatHealthText(health, maxHealth, percentage, showValue, showMax, showPercent);
        if (text.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        int color = 0xFFFFFFFF;
        if (percentage <= config.getLowThreshold()) {
            color = 0xFFFF3333;
        } else if (percentage <= config.getMediumThreshold()) {
            color = 0xFFFFFF33;
        }

        // 湾曲テキストパラメータ
        float charScale = 0.009f; // 文字サイズを半減
        float textRadius = radius + 0.05f * heightScale;
        // テキストの下端が色付きバーの上部（barYTop）に接するように配置
        float textY = barYTop - (font.lineHeight / 2.0f * charScale) - 0.005f * heightScale;
        float textArcRange = angleRange * 0.9f;

        renderCurvedText(poseStack, bufferSource, font, text,
                textRadius, centerAngle, textArcRange, textY, color, charScale);
    }

    /**
     * テキストを描画（湾曲はせず、常にプレイヤー方向を向く1枚のビルボードとして中央へ配置）
     */
    private static void renderCurvedText(PoseStack poseStack, MultiBufferSource.BufferSource buffer,
            Font font, String text,
            float radius, float centerAngle, float arcRange, float yPosition, int color, float charScale) {

        if (text.isEmpty()) {
            return;
        }

        float totalWidth = font.width(text);
        if (totalWidth <= 0) {
            return;
        }

        RenderSystem.disableCull();
        poseStack.pushPose();

        // 描画位置：バーの半径方向の少し外側、中心角度（手前側）の位置に配置
        float x = (float) Math.cos(centerAngle) * radius;
        float z = (float) Math.sin(centerAngle) * radius;
        poseStack.translate(x, yPosition, z);

        // 文字の平面をカメラ（プレイヤー）に向ける
        // centerAngleの向きに合わせて回転させる（270度は外側を向く）
        float rotationY = 270.0f - (float) Math.toDegrees(centerAngle);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotationY));

        // 右手座標系にスケールを設定（鏡文字防止、左右正立に）
        poseStack.scale(charScale, charScale, -charScale);

        // X軸（左右）のセンタリング、Y軸（上下）のセンタリング
        float charX = -totalWidth / 2.0f;
        float charY = -font.lineHeight / 2.0f;

        // 表から正しく読めるように表面描画
        font.drawInBatch(Component.literal(text), charX, charY, color, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        // 真裏からも確認できるように、裏面用として180度回転させて描画
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0f));
        font.drawInBatch(Component.literal(text), charX, charY, color, false,
                poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);

        poseStack.popPose();
        RenderSystem.enableCull();
    }

    /**
     * HP表示テキストをフォーマット
     */
    private static String formatHealthText(float health, float maxHealth, float percentage,
            boolean showValue, boolean showMax, boolean showPercent) {

        StringBuilder sb = new StringBuilder();

        if (showValue) {
            if (showMax) {
                sb.append(formatNumber(health)).append("/").append(formatNumber(maxHealth));
            } else {
                sb.append(formatNumber(health));
            }
        }

        if (showPercent) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(Math.round(percentage * 100)).append("%");
        }

        return sb.toString();
    }

    /**
     * 数値をフォーマット（小数点以下を丸める）
     */
    private static String formatNumber(float value) {
        if (value == Math.floor(value)) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }
}
