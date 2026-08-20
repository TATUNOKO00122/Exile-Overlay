package com.example.exile_overlay.client.damage;

import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class DamageNumber {
    private static final float RISE_SPEED_MIN = 0.014f;
    private static final float RISE_SPEED_MAX = 0.03f;
    private static final float RISE_ACCELERATION = 0.998f;
    private static final float KNOCKBACK_DECAY = 0.88f;
    private static final float MAX_DAMAGE_SCALE = 2.0f;
    private static final float DAMAGE_SCALE_FACTOR = 0.15f;

    private static final float VERTICAL_FLOAT_SPEED = 0.022f;
    private static final float VERTICAL_FLOAT_DECAY = 0.97f;
    private static final float MAX_FLOAT_HEIGHT = 0.65f;
    private static final float POP_ARC_INITIAL_Y = 0.075f;
    private static final float POP_ARC_GRAVITY = 0.0035f;

    private Vec3 position;
    private final Vec3 initialPosition;
    private Vec3 velocity;
    private float damage;
    private final boolean isCrit;
    private DamageType type;
    private final int entityId;
    private int life;
    private float riseSpeed;
    private int slotIndex;
    private final DamagePopupMode mode;

    public DamageNumber(Vec3 position, float damage, boolean isCrit,
                        DamageType type, int entityId, Vec3 knockbackVelocity, int slotIndex) {
        this(position, damage, isCrit, type, entityId, knockbackVelocity, slotIndex, DamagePopupConfig.getInstance().getPopupMode());
    }

    public DamageNumber(Vec3 position, float damage, boolean isCrit,
                        DamageType type, int entityId, Vec3 knockbackVelocity, int slotIndex, DamagePopupMode mode) {
        this.damage = damage;
        this.isCrit = isCrit;
        this.type = type;
        this.entityId = entityId;
        this.life = 0;
        this.slotIndex = slotIndex;
        this.mode = mode != null ? mode : DamagePopupMode.SPIRAL_SPREAD;
        this.position = position;
        this.initialPosition = position;

        switch (this.mode) {
            case VERTICAL_FLOAT -> {
                this.riseSpeed = VERTICAL_FLOAT_SPEED + (isCrit ? 0.005f : 0.0f);
                this.velocity = Vec3.ZERO;
            }
            case POP_ARC -> {
                this.riseSpeed = 0;
                float arcY = POP_ARC_INITIAL_Y * (isCrit ? 1.3f : 1.0f);
                double randX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.04;
                double randZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.04;
                this.velocity = new Vec3(randX, arcY, randZ);
            }
            case SPIRAL_SPREAD -> {
                this.riseSpeed = RISE_SPEED_MIN + ThreadLocalRandom.current().nextFloat() * (RISE_SPEED_MAX - RISE_SPEED_MIN);
                this.velocity = knockbackVelocity != null ? knockbackVelocity : Vec3.ZERO;
            }
        }
    }

    public void tick() {
        life++;
        switch (mode) {
            case VERTICAL_FLOAT -> {
                position = position.add(0, riseSpeed, 0);
            }
            case POP_ARC -> {
                position = position.add(velocity);
                velocity = new Vec3(
                    velocity.x * 0.92,
                    velocity.y - POP_ARC_GRAVITY,
                    velocity.z * 0.92
                );
            }
            case SPIRAL_SPREAD -> {
                position = position.add(velocity.x, riseSpeed + velocity.y, velocity.z);
                velocity = new Vec3(
                    velocity.x * KNOCKBACK_DECAY,
                    velocity.y * KNOCKBACK_DECAY,
                    velocity.z * KNOCKBACK_DECAY
                );
                riseSpeed *= RISE_ACCELERATION;
            }
        }
    }

    public boolean isExpired() {
        if (mode == DamagePopupMode.VERTICAL_FLOAT) {
            double traveledY = position.y - initialPosition.y;
            return traveledY >= MAX_FLOAT_HEIGHT || life >= 100;
        }
        return life >= DamagePopupConfig.getInstance().getDisplayDuration();
    }

    public Vec3 getPosition() { return position; }
    public float getDamage() { return damage; }
    public boolean isCrit() { return isCrit; }
    public DamageType getType() { return type; }
    public int getEntityId() { return entityId; }
    public int getLife() { return life; }
    public int getSlotIndex() { return slotIndex; }
    public DamagePopupMode getMode() { return mode; }

    public int getDisplayColor() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        if (type == DamageType.HEALING) {
            return config.getHealingColor();
        }
        if (isCrit) {
            return config.getCriticalDamageColor();
        }
        return config.getColorForType(type);
    }

    public float getScale() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        float baseScale = isCrit ? config.getCriticalScale() : config.getBaseScale();

        if (config.isEnableDamageScale() && damage > 0) {
            float log = (float) Math.log10(Math.max(1.0, damage));
            baseScale *= Math.min(MAX_DAMAGE_SCALE, 1.0f + log * DAMAGE_SCALE_FACTOR);
        }

        // VERTICAL_FLOAT モードでは拡大・縮小アニメーションを行わず一定サイズを維持
        if (mode == DamagePopupMode.VERTICAL_FLOAT) {
            return baseScale;
        }

        int fadeIn = config.getFadeInDuration();

        if (fadeIn > 0 && life < fadeIn) {
            float progress = life / (float) fadeIn;
            float bounce = 1.0f + (float) Math.sin(progress * Math.PI) * 0.3f;
            return baseScale * progress * bounce;
        }

        return baseScale;
    }

    public float getAlpha() {
        // VERTICAL_FLOAT モードでは透明度フェードを行わず完全不透明(1.0f)のまま単に消える
        if (mode == DamagePopupMode.VERTICAL_FLOAT) {
            return 1.0f;
        }

        int displayDuration = DamagePopupConfig.getInstance().getDisplayDuration();
        int fadeIn = DamagePopupConfig.getInstance().getFadeInDuration();
        int fadeOut = DamagePopupConfig.getInstance().getFadeOutDuration();

        if (fadeIn > 0 && life < fadeIn) {
            return life / (float) fadeIn;
        }

        int fadeOutStart = displayDuration - fadeOut;
        if (fadeOut > 0 && life > fadeOutStart) {
            float fadeProgress = (displayDuration - life) / (float) fadeOut;
            return Math.max(0.0f, fadeProgress);
        }

        return 1.0f;
    }
}
