package com.example.exile_overlay.client.damage;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.BitSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DamagePopupManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/DamagePopupManager");
    private static final DamagePopupManager INSTANCE = new DamagePopupManager();

    private final List<DamageNumber> damageNumbers = new CopyOnWriteArrayList<>();

    private final StringBuilder textBuilder = new StringBuilder(16);

    private static final float KNOCKBACK_STRENGTH = 0.06f;
    private static final float SPREAD_RADIUS_BASE = 0.6f;
    private static final float SPREAD_RADIUS_INCREMENT = 0.3f;

    private DamagePopupConfig config() {
        return DamagePopupConfig.getInstance();
    }

    private DamagePopupManager() {
    }

    public static DamagePopupManager getInstance() {
        return INSTANCE;
    }

    // ========== Health Change Detection ==========

    public void onHealthChanged(LivingEntity entity, float oldHealth, float newHealth) {
        try {
            if (entity == null || !entity.level().isClientSide()) {
                return;
            }

            float diff = oldHealth - newHealth;

            if (diff > 0.1f) {
                onDamage(entity, diff);
            } else if (diff < -0.1f) {
                onHeal(entity, -diff);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to handle health change", e);
        }
    }

    private void onDamage(LivingEntity entity, float damage) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (!config.isShowPlayerDamage() && entity instanceof Player) {
            return;
        }

        boolean isCrit = false;
        if (entity.getLastHurtByMob() instanceof Player player) {
            isCrit = player.getAttackStrengthScale(0.5f) > 0.9f;
        }

        Vec3 basePosition = entity.position().add(0, entity.getBbHeight() * config.getPopupHeightRatio(), 0);
        Vec3 knockback = calculateKnockback(entity);
        addDamageNumber(basePosition, damage, isCrit, DamageType.NORMAL, entity.getId(), knockback);
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
        addDamageNumber(basePosition, healAmount, false, DamageType.HEALING, entity.getId(), Vec3.ZERO);
    }

    // ========== Knockback Direction ==========

    /**
     * 【設計思想】attacker→entity の方向ベクトルをノックバック初期速度として返す。
     * Y成分は除外し、純粋な水平方向の飛散とする。
     */
    private Vec3 calculateKnockback(LivingEntity entity) {
        if (!(entity.getLastHurtByMob() instanceof LivingEntity)) {
            return Vec3.ZERO;
        }
        LivingEntity attacker = (LivingEntity) entity.getLastHurtByMob();

        Vec3 dir = entity.position().subtract(attacker.position());
        double horizontalLen = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        if (horizontalLen < 0.01) {
            return Vec3.ZERO;
        }

        double nx = dir.x / horizontalLen;
        double nz = dir.z / horizontalLen;

        return new Vec3(nx * KNOCKBACK_STRENGTH, 0, nz * KNOCKBACK_STRENGTH);
    }

    // ========== Initial Placement (World-Space Spiral) ==========

    /**
     * 【設計思想】同一エンティティのアクティブポップアップが使用中のスロットインデックスを追跡し、
     * 空いたスロットを最優先で再利用する。offsetIndexが無制限に増加しないよう上限を設ける。
     */
    private int findAvailableSlot(int entityId) {
        int maxSlots = config().getMaxDamageTexts();
        BitSet used = new BitSet(maxSlots);

        for (DamageNumber dn : damageNumbers) {
            if (dn.getEntityId() == entityId) {
                int slot = dn.getSlotIndex();
                if (slot >= 0 && slot < maxSlots) {
                    used.set(slot);
                }
            }
        }

        int free = used.nextClearBit(0);
        return free < maxSlots ? free : maxSlots - 1;
    }

    private Vec3 calculatePositionForSlot(Vec3 basePosition, int slot) {
        double angle = slot * 137.508;
        double rad = Math.toRadians(angle);
        int ring = (slot / 8) + 1;
        float radius = SPREAD_RADIUS_BASE + ring * SPREAD_RADIUS_INCREMENT;

        double xOffset = Math.cos(rad) * radius;
        double zOffset = Math.sin(rad) * radius;
        double yOffset = ring * 0.2;

        return basePosition.add(xOffset, yOffset, zOffset);
    }

    // ========== Popup Creation ==========

    public void addDamageNumber(Vec3 position, float damage, boolean isCrit,
                                DamageType type, int entityId, Vec3 knockback) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (damageNumbers.size() >= config.getMaxDamageTexts()) {
            damageNumbers.remove(0);
        }

        int slot = findAvailableSlot(entityId);
        Vec3 finalPosition = calculatePositionForSlot(position, slot);
        damageNumbers.add(new DamageNumber(finalPosition, damage, isCrit, type, entityId, knockback, slot));
    }

    /**
     * 後方互換: 外部Mixinから色指定で呼ばれる旧パス。
     * color引数は無視され、DamageTypeベースでconfigから色が決定される。
     */
    public void addDamageNumber(Vec3 position, float damage, int color, boolean isCrit,
                                DamageType type, int entityId) {
        addDamageNumber(position, damage, isCrit, type, entityId, Vec3.ZERO);
    }

    // ========== Tick ==========

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
    }

    // ========== Rendering ==========

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

        DamagePopupConfig config = DamagePopupConfig.getInstance();
        FontPreset preset = config.getFontPreset();
        ResourceLocation fontLoc = preset.getFontLocation();
        Style fontStyle = fontLoc != null ? Style.EMPTY.withFont(fontLoc) : Style.EMPTY;
        Font font = mc.font;
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.polygonOffset(-1.0f, -1.0f);
        RenderSystem.enablePolygonOffset();

        for (DamageNumber dn : damageNumbers) {
            renderDamageNumber(poseStack, bufferSource, dn, camPos, font, fontStyle);
        }

        bufferSource.endBatch();

        RenderSystem.disablePolygonOffset();
        RenderSystem.polygonOffset(0.0f, 0.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private void renderDamageNumber(PoseStack poseStack, MultiBufferSource bufferSource,
                                    DamageNumber dn, Vec3 camPos, Font font, Style fontStyle) {
        poseStack.pushPose();
        try {
            Vec3 pos = dn.getPosition();
            float scale = dn.getScale();

            poseStack.translate(pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z);
            poseStack.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
            poseStack.scale(scale, scale, scale);

            int alpha = (int) (dn.getAlpha() * 255);
            int colorWithAlpha = (alpha << 24) | (dn.getDisplayColor() & 0xFFFFFF);

            String text = formatDamageText(dn.getDamage(), dn.getType() == DamageType.HEALING);
            float textWidth = font.width(Component.literal(text).withStyle(fontStyle));
            float x = -textWidth / 2.0f;
            float y = -font.lineHeight / 2.0f;

            if (DamagePopupConfig.getInstance().isEnableShadow()) {
                int shadowColor = ((int) (alpha * 0.5f) << 24) | 0x000000;
                DamageFontRenderer.renderText(poseStack, text, x + 1.0f, y + 1.0f,
                    shadowColor, bufferSource, 0xF000F0);
            }

            DamageFontRenderer.renderText(poseStack, text, x, y, colorWithAlpha,
                bufferSource, 0xF000F0);

        } finally {
            poseStack.popPose();
        }
    }

    // ========== Cleanup ==========

    public void clear() {
        damageNumbers.clear();
    }

    // ========== Text Formatting ==========

    private String formatDamageText(float value, boolean isHealing) {
        textBuilder.setLength(0);

        if (isHealing) {
            textBuilder.append('+');
        }

        float absValue = Math.abs(value);
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        if (config.isCompactNumbers()) {
            if (absValue >= 1_000_000f) {
                appendCompact(textBuilder, value, absValue, 1_000_000, 'm');
                return textBuilder.toString();
            } else if (absValue >= 1_000f) {
                appendCompact(textBuilder, value, absValue, 1_000, 'k');
                return textBuilder.toString();
            }
        }

        if (absValue < config.getDecimalThreshold()) {
            appendOneDecimal(textBuilder, value);
        } else {
            textBuilder.append(Math.round(value));
        }

        return textBuilder.toString();
    }

    private void appendCompact(StringBuilder sb, float value, float absValue, int divisor, char suffix) {
        int intPart = (int) (value / divisor);
        int absRemainder = (int) absValue % divisor;
        int dec = absRemainder / (divisor / 10);
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
}
