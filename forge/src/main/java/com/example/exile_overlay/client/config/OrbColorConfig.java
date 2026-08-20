package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

/**
 * 各オーブ（HP, Shield, Mana, Blood, Energy, Food）の描画色を保持・永続化する設定クラス。
 * 16進数カラーコード（"AARRGGBB" または "RRGGBB"）のパースとシリアライズ。
 */
public class OrbColorConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "orb_colors";
    private static final String FILE_NAME = "exile_overlay_orb_colors.json";
    private static volatile OrbColorConfig instance;
    private static final Object LOCK = new Object();

    // デフォルト色定義
    public static final int DEFAULT_HEALTH_COLOR = 0xFFA03232; // ヘルス
    public static final int DEFAULT_SHIELD_COLOR = 0xFF18BEFF; // シールド
    public static final int DEFAULT_MANA_COLOR   = 0xFF1E1EC8; // マナ
    public static final int DEFAULT_BLOOD_COLOR  = 0xFFA53232; // ブラッド
    public static final int DEFAULT_ENERGY_COLOR = 0xFF289829; // エネルギー
    public static final int DEFAULT_FOOD_COLOR   = 0xFFD59F22; // フード

    private int healthColor = DEFAULT_HEALTH_COLOR;
    private int shieldColor = DEFAULT_SHIELD_COLOR;
    private int manaColor   = DEFAULT_MANA_COLOR;
    private int bloodColor  = DEFAULT_BLOOD_COLOR;
    private int energyColor = DEFAULT_ENERGY_COLOR;
    private int foodColor   = DEFAULT_FOOD_COLOR;

    private OrbColorConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static OrbColorConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new OrbColorConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("healthColor")) healthColor = parseColor(obj.get("healthColor").getAsString(), DEFAULT_HEALTH_COLOR);
        if (obj.has("shieldColor")) shieldColor = parseColor(obj.get("shieldColor").getAsString(), DEFAULT_SHIELD_COLOR);
        if (obj.has("manaColor")) manaColor = parseColor(obj.get("manaColor").getAsString(), DEFAULT_MANA_COLOR);
        if (obj.has("bloodColor")) bloodColor = parseColor(obj.get("bloodColor").getAsString(), DEFAULT_BLOOD_COLOR);
        if (obj.has("energyColor")) energyColor = parseColor(obj.get("energyColor").getAsString(), DEFAULT_ENERGY_COLOR);
        if (obj.has("foodColor")) foodColor = parseColor(obj.get("foodColor").getAsString(), DEFAULT_FOOD_COLOR);
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("healthColor", formatColor(healthColor));
        obj.addProperty("shieldColor", formatColor(shieldColor));
        obj.addProperty("manaColor", formatColor(manaColor));
        obj.addProperty("bloodColor", formatColor(bloodColor));
        obj.addProperty("energyColor", formatColor(energyColor));
        obj.addProperty("foodColor", formatColor(foodColor));
    }

    private static int parseColor(String hexStr, int defaultColor) {
        if (hexStr == null || hexStr.isBlank()) return defaultColor;
        try {
            String clean = hexStr.startsWith("#") ? hexStr.substring(1) : hexStr;
            if (clean.length() == 6) {
                return (int) (0xFF000000L | Long.parseLong(clean, 16));
            } else if (clean.length() == 8) {
                return (int) Long.parseLong(clean, 16);
            }
        } catch (NumberFormatException ignored) {}
        return defaultColor;
    }

    private static String formatColor(int color) {
        return String.format("%08X", color);
    }

    public int getHealthColor() { return healthColor; }
    public void setHealthColor(int color) { this.healthColor = color; }

    public int getShieldColor() { return shieldColor; }
    public void setShieldColor(int color) { this.shieldColor = color; }

    public int getManaColor() { return manaColor; }
    public void setManaColor(int color) { this.manaColor = color; }

    public int getBloodColor() { return bloodColor; }
    public void setBloodColor(int color) { this.bloodColor = color; }

    public int getEnergyColor() { return energyColor; }
    public void setEnergyColor(int color) { this.energyColor = color; }

    public int getFoodColor() { return foodColor; }
    public void setFoodColor(int color) { this.foodColor = color; }

    public void resetToDefaults() {
        this.healthColor = DEFAULT_HEALTH_COLOR;
        this.shieldColor = DEFAULT_SHIELD_COLOR;
        this.manaColor = DEFAULT_MANA_COLOR;
        this.bloodColor = DEFAULT_BLOOD_COLOR;
        this.energyColor = DEFAULT_ENERGY_COLOR;
        this.foodColor = DEFAULT_FOOD_COLOR;
    }
}
