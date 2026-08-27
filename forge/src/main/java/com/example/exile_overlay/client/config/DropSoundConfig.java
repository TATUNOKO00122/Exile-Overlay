package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DropSoundConfig extends AbstractConfigSection {
    private static final String SECTION_ID = "drop_sound";
    private static volatile DropSoundConfig instance;

    public static class RaritySound {
        private boolean enabled;
        private String sound;
        private float volume;

        public RaritySound(boolean enabled, String sound, float volume) {
            this.enabled = enabled;
            this.sound = sound;
            this.volume = volume;
        }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSound() { return sound; }
        public void setSound(String sound) { this.sound = sound; }
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = Math.max(0.0f, Math.min(2.0f, volume)); }
    }

    private boolean enabled = false;
    private final Map<String, RaritySound> raritySounds = new HashMap<>();

    private DropSoundConfig() {
        super(SECTION_ID, "drop_sound.json");
        resetToDefaults();
        load();
    }

    public static DropSoundConfig getInstance() {
        if (instance == null) {
            synchronized (DropSoundConfig.class) {
                if (instance == null) {
                    instance = new DropSoundConfig();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject json) {
        if (json.has("enabled")) {
            this.enabled = json.get("enabled").getAsBoolean();
        }
        if (json.has("rarities")) {
            JsonObject raritiesJson = json.getAsJsonObject("rarities");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : raritiesJson.entrySet()) {
                String rarity = entry.getKey();
                if (raritySounds.containsKey(rarity)) {
                    JsonObject rJson = entry.getValue().getAsJsonObject();
                    RaritySound rSound = raritySounds.get(rarity);
                    if (rJson.has("enabled")) rSound.setEnabled(rJson.get("enabled").getAsBoolean());
                    if (rJson.has("sound")) {
                        String loadedSound = rJson.get("sound").getAsString();
                        // バニラのデフォルト音（minecraft:...）が残っている場合は空文字にリセット
                        if (loadedSound != null && loadedSound.startsWith("minecraft:")) {
                            rSound.setSound("");
                        } else {
                            rSound.setSound(loadedSound);
                        }
                    }
                    if (rJson.has("volume")) rSound.setVolume(rJson.get("volume").getAsFloat());
                }
            }
        }
    }

    @Override
    protected void serialize(JsonObject json) {
        json.addProperty("enabled", this.enabled);
        JsonObject raritiesJson = new JsonObject();
        for (Map.Entry<String, RaritySound> entry : raritySounds.entrySet()) {
            JsonObject rJson = new JsonObject();
            rJson.addProperty("enabled", entry.getValue().isEnabled());
            rJson.addProperty("sound", entry.getValue().getSound());
            rJson.addProperty("volume", entry.getValue().getVolume());
            raritiesJson.add(entry.getKey(), rJson);
        }
        json.add("rarities", raritiesJson);
    }

    public void resetToDefaults() {
        this.enabled = false;
        this.raritySounds.clear();
        this.raritySounds.put("legendary", new RaritySound(false, "", 1.0f));
        this.raritySounds.put("mythic", new RaritySound(false, "", 1.0f));
        this.raritySounds.put("unique", new RaritySound(false, "", 1.0f));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RaritySound getRaritySound(String rarity) {
        if (rarity == null) return null;
        return raritySounds.get(rarity.toLowerCase(Locale.ROOT));
    }

    public Map<String, RaritySound> getRaritySounds() {
        return raritySounds;
    }
}
