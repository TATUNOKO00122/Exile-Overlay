package com.example.exile_overlay.client.render.entity;

import com.example.exile_overlay.client.config.AbstractConfigSection;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class EntityHealthBarConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "entity_healthbar";
    private static final String FILE_NAME = "exile_overlay_entity_healthbar.json";
    private static volatile EntityHealthBarConfig instance;
    private static final Object LOCK = new Object();

    private boolean enabled = false;
    private boolean showPoison = false;
    private boolean showBleed = false;
    private boolean showFriendlyColor = true;
    private int maxDistance = 24;
    private double heightAbove = 0.5;
    private int barWidth = 30;
    private int barHeight = 2;
    private float scale = 1.0f;
    private int displayDuration = 5;
    private String healthBarColor = "B02020";
    private String poisonBarColor = "246E07";
    private String bleedBarColor = "540606";
    private String friendlyBarColor = "2D8B2D";
    private List<String> blacklist = new ArrayList<>(DEFAULT_BLACKLIST);

    public static final List<String> DEFAULT_BLACKLIST = List.of(
        "minecraft:shulker",
        "minecraft:armor_stand",
        "minecraft:item_frame",
        "minecraft:glow_item_frame",
        "minecraft:painting",
        "minecraft:end_crystal",
        "minecraft:experience_orb"
    );

    public enum ColorPreset {
        DARK_RED("8B0000", "exile_overlay.config.hp_color.dark_red"),
        BRICK_RED("B22222", "exile_overlay.config.hp_color.brick_red"),
        PINK_RED("FF4040", "exile_overlay.config.hp_color.pink_red"),
        CRIMSON("DC143C", "exile_overlay.config.hp_color.crimson"),
        PURE_RED("FF0000", "exile_overlay.config.hp_color.pure_red");

        private final String hex;
        private final String translationKey;

        ColorPreset(String hex, String translationKey) {
            this.hex = hex;
            this.translationKey = translationKey;
        }

        public String getHex() { return hex; }
        public String getTranslationKey() { return translationKey; }
        public int getColorValue() { return 0xFF000000 | Integer.parseInt(hex, 16); }

        public static ColorPreset fromHex(String hex) {
            for (ColorPreset preset : values()) {
                if (preset.hex.equalsIgnoreCase(hex)) {
                    return preset;
                }
            }
            return DARK_RED;
        }
    }

    private EntityHealthBarConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static EntityHealthBarConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EntityHealthBarConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
        if (obj.has("showPoison")) showPoison = obj.get("showPoison").getAsBoolean();
        if (obj.has("showBleed")) showBleed = obj.get("showBleed").getAsBoolean();
        if (obj.has("showFriendlyColor")) showFriendlyColor = obj.get("showFriendlyColor").getAsBoolean();
        if (obj.has("maxDistance")) maxDistance = obj.get("maxDistance").getAsInt();
        if (obj.has("heightAbove")) heightAbove = obj.get("heightAbove").getAsDouble();
        if (obj.has("barWidth")) barWidth = obj.get("barWidth").getAsInt();
        if (obj.has("barHeight")) barHeight = obj.get("barHeight").getAsInt();
        if (obj.has("scale")) scale = obj.get("scale").getAsFloat();
        if (obj.has("displayDuration")) displayDuration = obj.get("displayDuration").getAsInt();
        if (obj.has("healthBarColor")) healthBarColor = obj.get("healthBarColor").getAsString();
        if (obj.has("poisonBarColor")) poisonBarColor = obj.get("poisonBarColor").getAsString();
        if (obj.has("bleedBarColor")) bleedBarColor = obj.get("bleedBarColor").getAsString();
        if (obj.has("friendlyBarColor")) friendlyBarColor = obj.get("friendlyBarColor").getAsString();

        if (obj.has("blacklist")) {
            blacklist.clear();
            for (var element : obj.getAsJsonArray("blacklist")) {
                blacklist.add(element.getAsString());
            }
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("enabled", enabled);
        obj.addProperty("showPoison", showPoison);
        obj.addProperty("showBleed", showBleed);
        obj.addProperty("showFriendlyColor", showFriendlyColor);
        obj.addProperty("maxDistance", maxDistance);
        obj.addProperty("heightAbove", heightAbove);
        obj.addProperty("barWidth", barWidth);
        obj.addProperty("barHeight", barHeight);
        obj.addProperty("scale", scale);
        obj.addProperty("displayDuration", displayDuration);
        obj.addProperty("healthBarColor", healthBarColor);
        obj.addProperty("poisonBarColor", poisonBarColor);
        obj.addProperty("bleedBarColor", bleedBarColor);
        obj.addProperty("friendlyBarColor", friendlyBarColor);

        JsonArray blacklistArray = new JsonArray();
        for (String entry : blacklist) {
            blacklistArray.add(entry);
        }
        obj.add("blacklist", blacklistArray);
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isShowPoison() { return showPoison; }
    public void setShowPoison(boolean showPoison) { this.showPoison = showPoison; }

    public boolean isShowBleed() { return showBleed; }
    public void setShowBleed(boolean showBleed) { this.showBleed = showBleed; }

    public boolean isShowFriendlyColor() { return showFriendlyColor; }
    public void setShowFriendlyColor(boolean showFriendlyColor) { this.showFriendlyColor = showFriendlyColor; }

    public int getMaxDistance() { return maxDistance; }
    public void setMaxDistance(int maxDistance) { this.maxDistance = maxDistance; }

    public double getHeightAbove() { return heightAbove; }
    public void setHeightAbove(double heightAbove) { this.heightAbove = heightAbove; }

    public int getBarWidth() { return barWidth; }
    public void setBarWidth(int barWidth) { this.barWidth = barWidth; }

    public int getBarHeight() { return barHeight; }
    public void setBarHeight(int barHeight) { this.barHeight = barHeight; }

    public float getScale() { return scale; }
    public void setScale(float scale) { this.scale = scale; }

    public int getDisplayDuration() { return displayDuration; }
    public void setDisplayDuration(int displayDuration) { this.displayDuration = displayDuration; }

    public String getHealthBarColor() { return healthBarColor; }
    public void setHealthBarColor(String healthBarColor) { this.healthBarColor = healthBarColor; }

    public String getPoisonBarColor() { return poisonBarColor; }
    public void setPoisonBarColor(String poisonBarColor) { this.poisonBarColor = poisonBarColor; }

    public String getBleedBarColor() { return bleedBarColor; }
    public void setBleedBarColor(String bleedBarColor) { this.bleedBarColor = bleedBarColor; }

    public String getFriendlyBarColor() { return friendlyBarColor; }
    public void setFriendlyBarColor(String friendlyBarColor) { this.friendlyBarColor = friendlyBarColor; }

    public int getHealthBarColorHex() {
        return getHealthBarColorHex(0xFF);
    }

    public int getHealthBarColorHex(int alpha) {
        return parseColorHex(healthBarColor, 0xB02020, alpha);
    }

    public int getHostileBarColorHex() {
        return getHealthBarColorHex(0xFF);
    }

    public int getHostileBarColorHex(int alpha) {
        return getHealthBarColorHex(alpha);
    }

    public int getPoisonBarColorHex() {
        return getPoisonBarColorHex(0xFF);
    }

    public int getPoisonBarColorHex(int alpha) {
        return parseColorHex(poisonBarColor, 0x246E07, alpha);
    }

    public int getBleedBarColorHex() {
        return getBleedBarColorHex(0xFF);
    }

    public int getBleedBarColorHex(int alpha) {
        return parseColorHex(bleedBarColor, 0x540606, alpha);
    }

    public int getFriendlyBarColorHex() {
        return getFriendlyBarColorHex(0xFF);
    }

    public int getFriendlyBarColorHex(int alpha) {
        return parseColorHex(friendlyBarColor, 0x2D8B2D, alpha);
    }

    private static int parseColorHex(String hex, int defaultRgb, int alpha) {
        if (hex == null || hex.isEmpty()) {
            return ((alpha & 0xFF) << 24) | (defaultRgb & 0xFFFFFF);
        }
        try {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            int rgb = (int) Long.parseLong(cleanHex, 16) & 0xFFFFFF;
            return ((alpha & 0xFF) << 24) | rgb;
        } catch (Exception e) {
            return ((alpha & 0xFF) << 24) | (defaultRgb & 0xFFFFFF);
        }
    }

    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> blacklist) { this.blacklist = new ArrayList<>(blacklist); }
}
