package com.example.exile_overlay.dmgtracker.config;

import com.example.exile_overlay.client.config.AbstractConfigSection;
import com.google.gson.JsonObject;

public class TrackerConfig extends AbstractConfigSection {

    private static final TrackerConfig INSTANCE = new TrackerConfig();

    private boolean enabled = false;
    private boolean showOverlay = true;
    private int maxSkillsShown = 20;
    private int overlayPosX = -1;
    private int overlayPosY = -1;

    public TrackerConfig() {
        super("damage_tracker", "exile_overlay_damage_tracker.json", true);
    }

    public static TrackerConfig getInstance() {
        return INSTANCE;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean val) { enabled = val; save(); }
    public static boolean showOverlay() { return INSTANCE.enabled && INSTANCE.showOverlay; }
    public static void setShowOverlay(boolean show) {
        INSTANCE.enabled = show;
        INSTANCE.showOverlay = show;
        INSTANCE.save();
    }
    public static void toggleOverlay() {
        INSTANCE.enabled = !INSTANCE.enabled;
        if (INSTANCE.enabled) {
            INSTANCE.showOverlay = true;
        }
        INSTANCE.save();
    }
    public static int getMaxSkillsShown() { return INSTANCE.maxSkillsShown; }
    public void setMaxSkillsShown(int maxSkillsShown) {
        this.maxSkillsShown = maxSkillsShown;
        this.save();
    }
    public static int getOverlayPosX() { return INSTANCE.overlayPosX; }
    public static int getOverlayPosY() { return INSTANCE.overlayPosY; }
    public static void setOverlayPos(int x, int y) { INSTANCE.overlayPosX = x; INSTANCE.overlayPosY = y; INSTANCE.save(); }
    public static void resetOverlayPos() { INSTANCE.overlayPosX = -1; INSTANCE.overlayPosY = -1; INSTANCE.save(); }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("enabled")) enabled = obj.get("enabled").getAsBoolean();
        if (obj.has("showOverlay")) showOverlay = obj.get("showOverlay").getAsBoolean();
        if (obj.has("maxSkillsShown")) maxSkillsShown = obj.get("maxSkillsShown").getAsInt();
        if (obj.has("overlayPosX")) overlayPosX = obj.get("overlayPosX").getAsInt();
        if (obj.has("overlayPosY")) overlayPosY = obj.get("overlayPosY").getAsInt();
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("enabled", enabled);
        obj.addProperty("showOverlay", showOverlay);
        obj.addProperty("maxSkillsShown", maxSkillsShown);
        obj.addProperty("overlayPosX", overlayPosX);
        obj.addProperty("overlayPosY", overlayPosY);
    }
}
