package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

/**
 * スキルバフオーバーレイのフィルタ設定
 *
 * - スキルバフ表示の各カテゴリON/OFF管理
 */
public class SkillBuffFilterConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "skill_buff_filter";
    private static final String FILE_NAME = "exile_overlay_skill_buff_filter.json";
    private static volatile SkillBuffFilterConfig instance;
    private static final Object LOCK = new Object();

    private boolean showAura = true;
    private boolean showSelfSkill = true;
    private boolean showFood = false;
    private boolean showCharge = true;
    private boolean showSong = true;
    private boolean showGolem = true;
    private boolean showOther = false;

    private SkillBuffFilterConfig() {
        super(SECTION_ID, FILE_NAME);
    }

    public static SkillBuffFilterConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SkillBuffFilterConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("showAura")) showAura = obj.get("showAura").getAsBoolean();
        if (obj.has("showSelfSkill")) showSelfSkill = obj.get("showSelfSkill").getAsBoolean();
        if (obj.has("showFood")) showFood = obj.get("showFood").getAsBoolean();
        if (obj.has("showCharge")) showCharge = obj.get("showCharge").getAsBoolean();
        if (obj.has("showSong")) showSong = obj.get("showSong").getAsBoolean();
        if (obj.has("showGolem")) showGolem = obj.get("showGolem").getAsBoolean();
        if (obj.has("showOther")) showOther = obj.get("showOther").getAsBoolean();
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("showAura", showAura);
        obj.addProperty("showSelfSkill", showSelfSkill);
        obj.addProperty("showFood", showFood);
        obj.addProperty("showCharge", showCharge);
        obj.addProperty("showSong", showSong);
        obj.addProperty("showGolem", showGolem);
        obj.addProperty("showOther", showOther);
    }

    public boolean isShowAura() { return showAura; }
    public boolean isShowSelfSkill() { return showSelfSkill; }
    public boolean isShowFood() { return showFood; }
    public boolean isShowCharge() { return showCharge; }
    public boolean isShowSong() { return showSong; }
    public boolean isShowGolem() { return showGolem; }
    public boolean isShowOther() { return showOther; }

    public void setShowAura(boolean v) { showAura = v; }
    public void setShowSelfSkill(boolean v) { showSelfSkill = v; }
    public void setShowFood(boolean v) { showFood = v; }
    public void setShowCharge(boolean v) { showCharge = v; }
    public void setShowSong(boolean v) { showSong = v; }
    public void setShowGolem(boolean v) { showGolem = v; }
    public void setShowOther(boolean v) { showOther = v; }
}
