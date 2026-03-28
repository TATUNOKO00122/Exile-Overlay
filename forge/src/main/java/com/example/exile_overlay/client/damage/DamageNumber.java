package com.example.exile_overlay.client.damage;

import net.minecraft.world.phys.Vec3;

public class DamageNumber {
    private Vec3 position;
    private Vec3 velocity;
    private float damage;
    private final int color;
    private final boolean isCrit;
    private final DamageType type;
    private final int entityId;
    private int life;

    public DamageNumber(Vec3 position, float damage, int color, boolean isCrit, 
                       DamageType type, int entityId, int comboCount) {
        this.damage = damage;
        this.color = color;
        this.isCrit = isCrit;
        this.type = type;
        this.entityId = entityId;
        this.life = 0;

        DamagePopupConfig config = DamagePopupConfig.getInstance();
        float spread = config.getHorizontalSpread();
        float xOffset = (float) (Math.random() - 0.5) * spread;
        float zOffset = (float) (Math.random() - 0.5) * spread;
        this.position = position.add(xOffset, 0, zOffset);

        this.velocity = Vec3.ZERO;
    }

    public void tick() {
        life++;
    }

    public void addDamage(float amount) {
        this.damage += amount;
    }

    public void resetLife() {
        this.life = 0;
    }

    public boolean isExpired() {
        return life >= DamagePopupConfig.getInstance().getDisplayDuration();
    }

    public Vec3 getPosition() { return position; }
    public void setPosition(Vec3 pos) { this.position = pos; }
    public float getDamage() { return damage; }
    public int getColor() { return color; }
    public boolean isCrit() { return isCrit; }
    public DamageType getType() { return type; }
    public int getEntityId() { return entityId; }
    public int getLife() { return life; }
    public Vec3 getVelocity() { return velocity; }
    public void setVelocity(Vec3 vel) { this.velocity = vel; }

    public int getDisplayColor() {
        if (type == DamageType.HEALING) {
            return DamagePopupConfig.getInstance().getHealingColor();
        }
        if (isCrit) {
            return DamagePopupConfig.getInstance().getCriticalDamageColor();
        }
        return color;
    }

    public float getScale() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        float baseScale = isCrit ? config.getCriticalScale() : config.getBaseScale();

        int fadeIn = config.getFadeInDuration();
        if (fadeIn > 0 && life < fadeIn) {
            float progress = life / (float) fadeIn;
            float bounce = 1.0f + (float) Math.sin(progress * Math.PI) * 0.3f;
            return baseScale * progress * bounce;
        }

        int fadeOut = config.getFadeOutDuration();
        int fadeOutStart = config.getDisplayDuration() - fadeOut;
        if (fadeOut > 0 && life > fadeOutStart) {
            float fadeProgress = (config.getDisplayDuration() - life) / (float) fadeOut;
            return baseScale * Math.max(0.0f, fadeProgress);
        }

        return baseScale;
    }

    public float getAlpha() {
        DamagePopupConfig config = DamagePopupConfig.getInstance();

        int fadeIn = config.getFadeInDuration();
        if (fadeIn > 0 && life < fadeIn) {
            return life / (float) fadeIn;
        }

        int fadeOut = config.getFadeOutDuration();
        int fadeOutStart = config.getDisplayDuration() - fadeOut;
        if (fadeOut > 0 && life > fadeOutStart) {
            float fadeProgress = (config.getDisplayDuration() - life) / (float) fadeOut;
            return Math.max(0.0f, fadeProgress);
        }

        return 1.0f;
    }
}
