package com.example.exile_overlay.client.render.entity;

import java.util.List;

public final class EntityHealthBarConfig {

    private EntityHealthBarConfig() {}

    public static boolean enabled = true;

    public interface ConfigAccess {
        int maxDistance();
        double heightAbove();
        List<String> blacklist();
    }

    public static final List<String> DEFAULT_BLACKLIST = List.of(
        "minecraft:shulker",
        "minecraft:armor_stand",
        "minecraft:item_frame",
        "minecraft:glow_item_frame",
        "minecraft:painting",
        "minecraft:end_crystal",
        "minecraft:experience_orb"
    );

    public static ConfigAccess instance = new DefaultConfig();

    private static class DefaultConfig implements ConfigAccess {
        @Override
        public int maxDistance() {
            return 24;
        }

        @Override
        public double heightAbove() {
            return 0.5;
        }

        @Override
        public List<String> blacklist() {
            return DEFAULT_BLACKLIST;
        }
    }
}