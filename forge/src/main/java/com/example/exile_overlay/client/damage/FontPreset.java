package com.example.exile_overlay.client.damage;

public enum FontPreset {
    MINECRAFT("Minecraft", null),
    LINESEED("Lineseed", "damage_font"),
    TITAN_ONE("Titan One", "font/titan_one_regular.ttf");

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

    public boolean isJsonFont() {
        return "damage_font".equals(resourcePath);
    }

    public static FontPreset fromName(String name) {
        if (name == null || name.isEmpty()) {
            return LINESEED;
        }
        for (FontPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return LINESEED;
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
        if (path.contains("lineseed") || path.contains("Lineseed") || path.contains("damage_font")) {
            return LINESEED;
        }
        if (path.contains("Titan") || path.contains("titan")) {
            return TITAN_ONE;
        }
        return MINECRAFT;
    }
}