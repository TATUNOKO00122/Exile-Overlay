package com.example.exile_overlay.client.damage;

import net.minecraft.world.phys.Vec3;

public class DamageNumber {
    private Vec3 position;
    private Vec3 velocity;
    private float damage;
    private final int color;
    private final boolean isCrit;
    private final DamageType type;
    private final int comboCount;
    private int life;
    private final DamagePopupConfig config;

    public DamageNumber(Vec3 position, float damage, int color, boolean isCrit, DamageType type, int comboCount) {
        this.config = DamagePopupConfig.getInstance();
        this.damage = damage;
        this.color = color;
        this.isCrit = isCrit;
        this.type = type;
        this.comboCount = comboCount;
        this.life = 0;

        float spread = config.getHorizontalSpread();
        float xOffset = (float) (Math.random() - 0.5) * spread;
        float zOffset = (float) (Math.random() - 0.5) * spread;
        this.position = position.add(xOffset, 0, zOffset);

        this.velocity = new Vec3(0, 0.02, 0);
    }

    public void tick() {
        life++;
        position = position.add(velocity);
        velocity = velocity.scale(0.9f);
    }

    public void addDamage(float amount) {
        this.damage += amount;
    }

    public void resetLife() {
        this.life = 0;
    }

    public boolean isExpired() {
        return life >= config.getDisplayDuration();
    }

    public Vec3 getPosition() {
        return position;
    }

    public float getDamage() {
        return damage;
    }

    public int getColor() {
        return switch (type) {
            case FIRE -> config.getFireDamageColor();
            case ICE -> config.getIceDamageColor();
            case LIGHTNING -> config.getLightningDamageColor();
            case POISON -> config.getPoisonDamageColor();
            case HEALING -> config.getHealingColor();
            default -> isCrit ? config.getCriticalDamageColor() : color;
        };
    }

    public float getScale() {
        float baseScale = isCrit ? config.getCriticalScale() : config.getBaseScale();

        if (comboCount > 1) {
            baseScale *= 1.0f + (Math.min(comboCount, 10) * 0.05f);
        }

        int fadeIn = config.getFadeInDuration();
        if (life < fadeIn) {
            float progress = life / (float) fadeIn;
            float bounce = 1.0f + (float) Math.sin(progress * Math.PI) * 0.3f;
            return baseScale * progress * bounce;
        }

        int fadeOut = config.getFadeOutDuration();
        int fadeOutStart = config.getDisplayDuration() - fadeOut;
        if (life > fadeOutStart) {
            float fadeProgress = (config.getDisplayDuration() - life) / (float) fadeOut;
            return baseScale * Math.max(0.0f, fadeProgress);
        }

        return baseScale;
    }

    public float getAlpha() {
        int fadeIn = config.getFadeInDuration();
        if (life < fadeIn) {
            return life / (float) fadeIn;
        }

        int fadeOut = config.getFadeOutDuration();
        int fadeOutStart = config.getDisplayDuration() - fadeOut;
        if (life > fadeOutStart) {
            float fadeProgress = (config.getDisplayDuration() - life) / (float) fadeOut;
            return Math.max(0.0f, fadeProgress);
        }

        return 1.0f;
    }

    public boolean isCrit() {
        return isCrit;
    }

    public Vec3 getVelocity() {
        return velocity;
    }

    public void setVelocity(Vec3 velocity) {
        this.velocity = velocity;
    }

    public int getLife() {
        return life;
    }
}
