package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

public class LootJournalCompatConfig extends AbstractConfigSection {
    private static final String SECTION_ID = "loot_journal_compat";
    private static volatile LootJournalCompatConfig instance;

    private boolean enableCompat = true;
    private boolean onlyMsItems = true;
    private boolean showFullAffixName = true;

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
    }

    @Override
    protected void serialize(JsonObject json) {
        json.addProperty("enable_compat", enableCompat);
        json.addProperty("only_ms_items", onlyMsItems);
        json.addProperty("show_full_affix_name", showFullAffixName);
    }

    public void resetToDefaults() {
        this.enableCompat = true;
        this.onlyMsItems = true;
        this.showFullAffixName = true;
    }

    public boolean isEnableCompat() { return enableCompat; }
    public void setEnableCompat(boolean enableCompat) { this.enableCompat = enableCompat; }

    public boolean isOnlyMsItems() { return onlyMsItems; }
    public void setOnlyMsItems(boolean onlyMsItems) { this.onlyMsItems = onlyMsItems; }

    public boolean isShowFullAffixName() { return showFullAffixName; }
    public void setShowFullAffixName(boolean showFullAffixName) { this.showFullAffixName = showFullAffixName; }
}

