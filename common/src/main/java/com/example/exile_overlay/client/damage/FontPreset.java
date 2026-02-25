package com.example.exile_overlay.client.damage;

public enum FontPreset {
    MINECRAFT("Minecraft", null),
    JERSEY20("Jersey 20", "font/Jersey20-Regular.ttf"),
    JERSEY15("Jersey 15", "font/Jersey15-Regular.ttf"),
    JACQUARD12("Jacquard 12", "font/Jacquard12-Regular.ttf"),
    TITAN_ONE("Titan One", "font/TitanOne-Regular.ttf");

    private final String displayName;
    private final String resourcePath;

    FontPreset(String displayName, String resourcePath) {
        this.displayName = displayName;
        this.resourcePath = resourcePath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public boolean isCustomFont() {
        return resourcePath != null;
    }

    public static FontPreset fromName(String name) {
        if (name == null || name.isEmpty()) {
            return JERSEY20;
        }
        for (FontPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return JERSEY20;
    }

    public static FontPreset fromPath(String path) {
        if (path == null || path.isEmpty()) {
            return MINECRAFT;
        }
        for (FontPreset preset : values()) {
            if (path.equals(preset.resourcePath)) {
                return preset;
            }
        }
        if (path.contains("Jersey20") || path.contains("jersey20")) {
            return JERSEY20;
        }
        if (path.contains("Jersey15") || path.contains("jersey15")) {
            return JERSEY15;
        }
        if (path.contains("Jacquard") || path.contains("jacquard")) {
            return JACQUARD12;
        }
        if (path.contains("Titan") || path.contains("titan")) {
            return TITAN_ONE;
        }
        return JERSEY20;
    }
}
