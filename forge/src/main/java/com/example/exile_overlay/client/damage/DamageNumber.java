package com.example.exile_overlay.client.damage;

import net.minecraft.world.phys.Vec3;

public class DamageNumber {
    private static final float RISE_SPEED = 0.02f;
    private static final float RISE_ACCELERATION = 0.998f;
    private static final float KNOCKBACK_DECAY = 0.88f;
    private static final float MAX_DAMAGE_SCALE = 2.0f;
    private static final float DAMAGE_SCALE_FACTOR = 0.15f;

    private Vec3 position;
    private Vec3 velocity;
    private float damage;
    private final boolean isCrit;
    private DamageType type;
    private final int entityId;
    private int life;
    private float riseSpeed;
    private int slotIndex;

    public DamageNumber(Vec3 position, float damage, boolean isCrit,
                        DamageType type, int entityId, Vec3 knockbackVelocity, int slotIndex) {
        this.damage = damage;
        this.isCrit = isCrit;
        this.type = type;
        this.entityId = entityId;
        this.life = 0;
        this.riseSpeed = RISE_SPEED;
        this.slotIndex = slotIndex;

        this.position = position;
        this.velocity = knockbackVelocity != null ? knockbackVelocity : Vec3.ZERO;
    }

    public void tick() {
        life++;
        position = position.add(velocity.x, riseSpeed + velocity.y, velocity.z);
        velocity = new Vec3(
            velocity.x * KNOCKBACK_DECAY,
            velocity.y * KNOCKBACK_DECAY,
            velocity.z * KNOCKBACK_DECAY
        );
        riseSpeed *= RISE_ACCELERATION;
    }

    public boolean isExpired() {
        return life >= DamagePopupConfig.getInstance().getDisplayDuration();
    }

    public Vec3 getPosition() { return position; }
    public float getDamage() { return damage; }
    public boolean isCrit() { return isCrit; }
    public DamageType getType() { return type; }
    public int getEntityId() { return entityId; }
    public int getLife() { return life; }
    public int getSlotIndex() { return slotIndex; }

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

        int displayDuration = config.getDisplayDuration();
        int fadeIn = config.getFadeInDuration();
        int fadeOut = config.getFadeOutDuration();

        if (fadeIn > 0 && life < fadeIn) {
            float progress = life / (float) fadeIn;
            float bounce = 1.0f + (float) Math.sin(progress * Math.PI) * 0.3f;
            return baseScale * progress * bounce;
        }

        int fadeOutStart = displayDuration - fadeOut;
        if (fadeOut > 0 && life > fadeOutStart) {
            float fadeProgress = (displayDuration - life) / (float) fadeOut;
            return baseScale * Math.max(0.0f, fadeProgress);
        }

        return baseScale;
    }

    public float getAlpha() {
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
