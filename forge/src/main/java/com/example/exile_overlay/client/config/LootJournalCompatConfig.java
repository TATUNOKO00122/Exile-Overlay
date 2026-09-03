package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

public class LootJournalCompatConfig extends AbstractConfigSection {
    private static final String SECTION_ID = "loot_journal_compat";
    private static volatile LootJournalCompatConfig instance;

    private boolean enableCompat = true;
    private boolean onlyMsItems = true;
    private boolean showFullAffixName = true;
    private boolean autoScaleByWeight = true;

    private float mirrorScale = 2.0f;
    private float megaUberScale = 1.7f;
    private float uberScale = 1.4f;
    private float rareScale = 1.2f;

    private LootJournalCompatConfig() {
        super(SECTION_ID, "loot_journal_compat.json");
        resetToDefaults();
        load();
    }

    public static LootJournalCompatConfig getInstance() {
        if (instance == null) {
            synchronized (LootJournalCompatConfig.class) {
                if (instance == null) {
                    instance = new LootJournalCompatConfig();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject json) {
        if (json.has("enable_compat")) {
            this.enableCompat = json.get("enable_compat").getAsBoolean();
        }
        if (json.has("only_ms_items")) {
            this.onlyMsItems = json.get("only_ms_items").getAsBoolean();
        }
        if (json.has("show_full_affix_name")) {
            this.showFullAffixName = json.get("show_full_affix_name").getAsBoolean();
        }
        if (json.has("auto_scale_by_weight")) {
            this.autoScaleByWeight = json.get("auto_scale_by_weight").getAsBoolean();
        }
        if (json.has("mirror_scale")) {
            this.mirrorScale = Math.max(1.0f, Math.min(3.0f, json.get("mirror_scale").getAsFloat()));
        }
        if (json.has("mega_uber_scale")) {
            this.megaUberScale = Math.max(1.0f, Math.min(3.0f, json.get("mega_uber_scale").getAsFloat()));
        }
        if (json.has("uber_scale")) {
            this.uberScale = Math.max(1.0f, Math.min(3.0f, json.get("uber_scale").getAsFloat()));
        }
        if (json.has("rare_scale")) {
            this.rareScale = Math.max(1.0f, Math.min(3.0f, json.get("rare_scale").getAsFloat()));
        }
    }

    @Override
    protected void serialize(JsonObject json) {
        json.addProperty("enable_compat", enableCompat);
        json.addProperty("only_ms_items", onlyMsItems);
        json.addProperty("show_full_affix_name", showFullAffixName);
        json.addProperty("auto_scale_by_weight", autoScaleByWeight);
        json.addProperty("mirror_scale", mirrorScale);
        json.addProperty("mega_uber_scale", megaUberScale);
        json.addProperty("uber_scale", uberScale);
        json.addProperty("rare_scale", rareScale);
    }

    public void resetToDefaults() {
        this.enableCompat = true;
        this.onlyMsItems = true;
        this.showFullAffixName = true;
        this.autoScaleByWeight = true;
        this.mirrorScale = 2.0f;
        this.megaUberScale = 1.7f;
        this.uberScale = 1.4f;
        this.rareScale = 1.2f;
    }

    public boolean isEnableCompat() { return enableCompat; }
    public void setEnableCompat(boolean enableCompat) { this.enableCompat = enableCompat; }

    public boolean isOnlyMsItems() { return onlyMsItems; }
    public void setOnlyMsItems(boolean onlyMsItems) { this.onlyMsItems = onlyMsItems; }

    public boolean isShowFullAffixName() { return showFullAffixName; }
    public void setShowFullAffixName(boolean showFullAffixName) { this.showFullAffixName = showFullAffixName; }

    public boolean isAutoScaleByWeight() { return autoScaleByWeight; }
    public void setAutoScaleByWeight(boolean autoScaleByWeight) { this.autoScaleByWeight = autoScaleByWeight; }

    public float getMirrorScale() { return mirrorScale; }
    public void setMirrorScale(float mirrorScale) { this.mirrorScale = mirrorScale; }

    public float getMegaUberScale() { return megaUberScale; }
    public void setMegaUberScale(float megaUberScale) { this.megaUberScale = megaUberScale; }

    public float getUberScale() { return uberScale; }
    public void setUberScale(float uberScale) { this.uberScale = uberScale; }

    public float getRareScale() { return rareScale; }
    public void setRareScale(float rareScale) { this.rareScale = rareScale; }
}
