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
        public boolean showMinions = true;
        public boolean showMercenary = true;
        public boolean sortByDuration = false;

        public static OverlayFilter createDefaultAll() {
            return new OverlayFilter();
        }

        public static OverlayFilter createDefaultSkillOnly() {
            OverlayFilter f = new OverlayFilter();
            f.showMinions = true;
            f.showMercenary = false;
            f.sortByDuration = false;
            return f;
        }

        public boolean isShowMinions() { return showMinions; }
        public boolean isShowMercenary() { return showMercenary; }
        public boolean isSortByDuration() { return sortByDuration; }

        public void setShowMinions(boolean v) { showMinions = v; }
        public void setShowMercenary(boolean v) { showMercenary = v; }
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
        if (o.has("showMinions")) filter.showMinions = o.get("showMinions").getAsBoolean();
        if (o.has("showMercenary")) filter.showMercenary = o.get("showMercenary").getAsBoolean();
        if (o.has("sortByDuration")) filter.sortByDuration = o.get("sortByDuration").getAsBoolean();
        return filter;
    }

    private static JsonObject serializeFilter(OverlayFilter filter) {
        JsonObject o = new JsonObject();
        o.addProperty("showMinions", filter.showMinions);
        o.addProperty("showMercenary", filter.showMercenary);
        o.addProperty("sortByDuration", filter.sortByDuration);
        return o;
    }

    public OverlayFilter getBuffOverlay() { return buffOverlay; }
    public OverlayFilter getSkillBuffOverlay() { return skillBuffOverlay; }
}
