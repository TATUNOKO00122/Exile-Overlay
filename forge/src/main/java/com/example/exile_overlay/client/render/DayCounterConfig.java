package com.example.exile_overlay.client.render;

import com.example.exile_overlay.client.config.AbstractConfigSection;
import com.google.gson.JsonObject;

public class DayCounterConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "day_counter";
    private static final String FILE_NAME = "exile_overlay_day_counter.json";
    private static volatile DayCounterConfig instance;
    private static final Object LOCK = new Object();

    private int soundVolume = 5;

    private DayCounterConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static DayCounterConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new DayCounterConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("soundVolume")) {
            soundVolume = obj.get("soundVolume").getAsInt();
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("soundVolume", soundVolume);
    }

    public int getSoundVolume() { return soundVolume; }
    public void setSoundVolume(int volume) { this.soundVolume = Math.max(0, Math.min(10, volume)); }
    public float getSoundVolumeFloat() { return soundVolume / 100.0f; }
}
