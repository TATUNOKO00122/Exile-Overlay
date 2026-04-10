package com.example.exile_overlay.client.config;

import net.minecraft.resources.ResourceLocation;

public enum HudFontPreset {
    MINECRAFT("Minecraft", null),
    GOOGLE_SANS("Google Sans", "exile_overlay:hud_font"),
    JERSEY_10("Jersey 10", "exile_overlay:hud_font_jersey_10"),
    GAME_POCKET("GamePocket", "exile_overlay:hud_font_game_pocket"),
    M6X11("m6x11", "exile_overlay:hud_font_m6x11");

    private final String displayName;
    private final ResourceLocation fontLocation;

    HudFontPreset(String displayName, String fontLocationStr) {
        this.displayName = displayName;
        this.fontLocation = fontLocationStr != null
                ? ResourceLocation.tryParse(fontLocationStr)
                : null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceLocation getFontLocation() {
        return fontLocation;
    }

    public boolean isCustomFont() {
        return fontLocation != null;
    }

    public static HudFontPreset fromName(String name) {
        if (name == null || name.isEmpty()) {
            return GOOGLE_SANS;
        }
        for (HudFontPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return GOOGLE_SANS;
    }
}
