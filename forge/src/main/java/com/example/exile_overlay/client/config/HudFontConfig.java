package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

/**
 * HUDフォント設定（カスタムフォントの有効無効とプリセット）を管理する設定クラス。
 */
public class HudFontConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "hud_font";
    private static final String FILE_NAME = "exile_overlay_hud_font.json";
    private static volatile HudFontConfig instance;
    private static final Object LOCK = new Object();

    private boolean useCustomFont = false;
    private HudFontPreset fontPreset = HudFontPreset.GOOGLE_SANS;

    private HudFontConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static HudFontConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new HudFontConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("useCustomFont")) {
            useCustomFont = obj.get("useCustomFont").getAsBoolean();
        }
        if (obj.has("fontPreset")) {
            fontPreset = HudFontPreset.fromName(obj.get("fontPreset").getAsString());
        } else if (obj.has("useCustomFont") && !useCustomFont) {
            fontPreset = HudFontPreset.MINECRAFT;
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("useCustomFont", useCustomFont);
        obj.addProperty("fontPreset", fontPreset.name());
    }

    public boolean isUseCustomFont() { return useCustomFont; }
    public void setUseCustomFont(boolean use) { this.useCustomFont = use; }

    public HudFontPreset getFontPreset() { return fontPreset; }
    public void setFontPreset(HudFontPreset preset) {
        this.fontPreset = preset;
        this.useCustomFont = preset.isCustomFont();
    }
}
