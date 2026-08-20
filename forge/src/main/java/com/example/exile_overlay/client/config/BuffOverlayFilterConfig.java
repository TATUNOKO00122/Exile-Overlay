package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

/**
 * バフオーバーレイの表示内容フィルタ設定。
 * buff_overlay / skill_buff_overlay それぞれの表示内容を個別に管理する。
 */
public class BuffOverlayFilterConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "buff_filter";
    private static final String FILE_NAME = "exile_overlay_buff_filter.json";
    private static volatile BuffOverlayFilterConfig instance;
    private static final Object LOCK = new Object();

    private OverlayFilter buffOverlay = OverlayFilter.createDefaultAll();
    private OverlayFilter skillBuffOverlay = OverlayFilter.createDefaultSkillOnly();

    public static class OverlayFilter {
        public boolean showVanillaBuffs = true;
        public boolean showVanillaDebuffs = true;
        public boolean showMnsBuffs = true;
        public boolean showMnsDebuffs = true;
        public boolean sortByDuration = false;

        public static OverlayFilter createDefaultAll() {
            return new OverlayFilter();
        }

        public static OverlayFilter createDefaultSkillOnly() {
            OverlayFilter f = new OverlayFilter();
            f.showVanillaBuffs = false;
            f.showVanillaDebuffs = false;
            f.showMnsBuffs = true;
            f.showMnsDebuffs = false;
            f.sortByDuration = false;
            return f;
        }

        public boolean isShowVanillaBuffs() { return showVanillaBuffs; }
        public boolean isShowVanillaDebuffs() { return showVanillaDebuffs; }
        public boolean isShowMnsBuffs() { return showMnsBuffs; }
        public boolean isShowMnsDebuffs() { return showMnsDebuffs; }
        public boolean isSortByDuration() { return sortByDuration; }

        public void setShowVanillaBuffs(boolean v) { showVanillaBuffs = v; }
        public void setShowVanillaDebuffs(boolean v) { showVanillaDebuffs = v; }
        public void setShowMnsBuffs(boolean v) { showMnsBuffs = v; }
        public void setShowMnsDebuffs(boolean v) { showMnsDebuffs = v; }
        public void setSortByDuration(boolean v) { sortByDuration = v; }
    }

    private BuffOverlayFilterConfig() {
        super(SECTION_ID, FILE_NAME);
    }

    public static BuffOverlayFilterConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new BuffOverlayFilterConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    public OverlayFilter getFilter(String overlayId) {
        if ("skill_buff_overlay".equals(overlayId)) {
            return skillBuffOverlay;
        }
        return buffOverlay;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("buffOverlay") && obj.get("buffOverlay").isJsonObject()) {
            buffOverlay = deserializeFilter(obj.getAsJsonObject("buffOverlay"));
        }
        if (obj.has("skillBuffOverlay") && obj.get("skillBuffOverlay").isJsonObject()) {
            skillBuffOverlay = deserializeFilter(obj.getAsJsonObject("skillBuffOverlay"));
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.add("buffOverlay", serializeFilter(buffOverlay));
        obj.add("skillBuffOverlay", serializeFilter(skillBuffOverlay));
    }

    private static OverlayFilter deserializeFilter(JsonObject o) {
        OverlayFilter filter = new OverlayFilter();
        if (o.has("showVanillaBuffs")) filter.showVanillaBuffs = o.get("showVanillaBuffs").getAsBoolean();
        if (o.has("showVanillaDebuffs")) filter.showVanillaDebuffs = o.get("showVanillaDebuffs").getAsBoolean();
        if (o.has("showMnsBuffs")) filter.showMnsBuffs = o.get("showMnsBuffs").getAsBoolean();
        if (o.has("showMnsDebuffs")) filter.showMnsDebuffs = o.get("showMnsDebuffs").getAsBoolean();
        if (o.has("sortByDuration")) filter.sortByDuration = o.get("sortByDuration").getAsBoolean();
        return filter;
    }

    private static JsonObject serializeFilter(OverlayFilter filter) {
        JsonObject o = new JsonObject();
        o.addProperty("showVanillaBuffs", filter.showVanillaBuffs);
        o.addProperty("showVanillaDebuffs", filter.showVanillaDebuffs);
        o.addProperty("showMnsBuffs", filter.showMnsBuffs);
        o.addProperty("showMnsDebuffs", filter.showMnsDebuffs);
        o.addProperty("sortByDuration", filter.sortByDuration);
        return o;
    }

    public OverlayFilter getBuffOverlay() { return buffOverlay; }
    public OverlayFilter getSkillBuffOverlay() { return skillBuffOverlay; }
}
