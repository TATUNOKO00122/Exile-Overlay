package com.example.exile_overlay.compat.jei;

import com.example.exile_overlay.client.config.AbstractConfigSection;
import com.google.gson.JsonObject;

/**
 * Just Enough Items (JEI) 連携の有効/無効設定を管理する設定クラス。
 */
public class JeiConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "jei";
    private static final String FILE_NAME = "jei.json";
    private static volatile JeiConfig instance;
    private static final Object LOCK = new Object();

    private boolean enabled = false;

    private JeiConfig() {
        super(SECTION_ID, FILE_NAME);
    }

    public static JeiConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new JeiConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("enabled")) {
            this.enabled = obj.get("enabled").getAsBoolean();
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("enabled", this.enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
