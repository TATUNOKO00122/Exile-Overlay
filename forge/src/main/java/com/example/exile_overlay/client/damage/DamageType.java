package com.example.exile_overlay.client.damage;

public enum DamageType {
    NORMAL(0xFFFFFF, "normal"),
    PHYSICAL(0xFFAA00, "physical"),
    FIRE(0xFF5555, "fire"),
    ICE(0x55FFFF, "ice"),
    LIGHTNING(0xFFFF55, "lightning"),
    NATURE(0xFFFF55, "nature"),
    POISON(0x00FF00, "poison"),
    MAGIC(0xAA00AA, "magic"),
    ELEMENTAL(0xFF77FF, "elemental"),
    WITHER(0x2F2F2F, "wither"),
    HEALING(0x55FF55, "healing");

    private final int defaultColor;
    private final String id;

    DamageType(int defaultColor, String id) {
        this.defaultColor = defaultColor;
        this.id = id;
    }

    public int getDefaultColor() {
        return defaultColor;
    }

    public String getId() {
        return id;
    }

    public static DamageType fromDamageSource(net.minecraft.world.damagesource.DamageSource source) {
        if (source == null) {
            return NORMAL;
        }

        String msgId = source.getMsgId();
        if (msgId == null) {
            return NORMAL;
        }

        if (msgId.contains("fire") || msgId.contains("burn") || msgId.contains("lava") || msgId.contains("hotFloor")) {
            return FIRE;
        } else if (msgId.contains("freeze") || msgId.contains("cold")) {
            return ICE;
        } else if (msgId.contains("lightning")) {
            return LIGHTNING;
        } else if (msgId.contains("poison")) {
            return POISON;
        } else if (msgId.contains("magic") || msgId.contains("thorns") || msgId.contains("indirectMagic")) {
            return MAGIC;
        } else if (msgId.contains("wither")) {
            return WITHER;
        }

        return NORMAL;
    }
}
