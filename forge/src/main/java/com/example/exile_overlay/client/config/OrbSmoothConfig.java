package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

public class OrbSmoothConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "orb_smooth";
    private static final String FILE_NAME = "exile_overlay_orb_smooth.json";
    private static volatile OrbSmoothConfig instance;
    private static final Object LOCK = new Object();

    // デフォルト値：増加時（回復）はスムージングON(3.0f)、減少時（被ダメ）はOFF（即時反映）
    private boolean smoothIncrease = true;
    private boolean smoothDecrease = false;
    private float increaseSpeed = 3.0f;
    private float decreaseSpeed = 3.0f;

    private OrbSmoothConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static OrbSmoothConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new OrbSmoothConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("smoothIncrease")) smoothIncrease = obj.get("smoothIncrease").getAsBoolean();
        if (obj.has("smoothDecrease")) smoothDecrease = obj.get("smoothDecrease").getAsBoolean();
        if (obj.has("increaseSpeed")) increaseSpeed = obj.get("increaseSpeed").getAsFloat();
        if (obj.has("decreaseSpeed")) decreaseSpeed = obj.get("decreaseSpeed").getAsFloat();
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("smoothIncrease", smoothIncrease);
        obj.addProperty("smoothDecrease", smoothDecrease);
        obj.addProperty("increaseSpeed", increaseSpeed);
        obj.addProperty("decreaseSpeed", decreaseSpeed);
    }

    public boolean isSmoothIncrease() {
        return smoothIncrease;
    }

    public void setSmoothIncrease(boolean smoothIncrease) {
        this.smoothIncrease = smoothIncrease;
    }

    public boolean isSmoothDecrease() {
        return smoothDecrease;
    }

    public void setSmoothDecrease(boolean smoothDecrease) {
        this.smoothDecrease = smoothDecrease;
    }

    public float getIncreaseSpeed() {
        return increaseSpeed;
    }

    public void setIncreaseSpeed(float increaseSpeed) {
        this.increaseSpeed = increaseSpeed;
    }

    public float getDecreaseSpeed() {
        return decreaseSpeed;
    }

    public void setDecreaseSpeed(float decreaseSpeed) {
        this.decreaseSpeed = decreaseSpeed;
    }
}
