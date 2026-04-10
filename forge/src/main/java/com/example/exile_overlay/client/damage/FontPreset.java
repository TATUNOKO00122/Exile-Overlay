package com.example.exile_overlay.client.damage;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public enum FontPreset {
    MINECRAFT("Minecraft", null),
    LINESEED("Lineseed", "exile_overlay:damage_font"),
    GAME_POCKET("Game Pocket", "exile_overlay:damage_font_game_pocket"),
    JERSEY_10("Jersey 10", "exile_overlay:damage_font_jersey_10"),
    GOOGLE_SANS_BOLD("Google Sans Bold", "exile_overlay:damage_font_google_sans_bold");

    private static final FontPreset DEFAULT = LINESEED;

    private final String displayName;
    @Nullable
    private final ResourceLocation fontLocation;

    FontPreset(String displayName, @Nullable String fontLocationStr) {
        this.displayName = displayName;
        this.fontLocation = fontLocationStr != null ? ResourceLocation.tryParse(fontLocationStr) : null;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public ResourceLocation getFontLocation() {
        return fontLocation;
    }

    public static FontPreset fromName(String name) {
        if (name == null || name.isEmpty()) {
            return DEFAULT;
        }
        for (FontPreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name)) {
                return preset;
            }
        }
        return DEFAULT;
    }
}
