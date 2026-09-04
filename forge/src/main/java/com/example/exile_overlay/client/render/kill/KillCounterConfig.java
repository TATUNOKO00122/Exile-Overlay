package com.example.exile_overlay.client.render.kill;

import com.example.exile_overlay.client.config.AbstractConfigSection;
import com.google.gson.JsonObject;

/**
 * キルカウンターの設定管理クラス
 */
public class KillCounterConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "kill_counter";
    private static final String FILE_NAME = "exile_overlay_kill_counter.json";
    private static volatile KillCounterConfig instance;
    private static final Object LOCK = new Object();

    private int timeoutSeconds = 15;

    private KillCounterConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static KillCounterConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new KillCounterConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("timeoutSeconds")) {
            timeoutSeconds = obj.get("timeoutSeconds").getAsInt();
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("timeoutSeconds", timeoutSeconds);
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
