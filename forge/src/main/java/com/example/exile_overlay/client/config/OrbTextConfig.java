package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

public class OrbTextConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "orb_text";
    private static final String FILE_NAME = "exile_overlay_orb_text.json";
    private static volatile OrbTextConfig instance;
    private static final Object LOCK = new Object();

    private static final float[] SCALE_OPTIONS = {0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};

    /**
     * オーブテキストの表示位置。CENTER（中央）とABOVE（上部）は排他切替。
     */
    public enum OrbTextPosition {
        CENTER,
        ABOVE,
        ABOVE_INTEGRATED
    }

    /**
     * Orb1のHP/MS表示モード
     */
    public enum Orb1MsMode {
        OVERLAP,  // 重ね合わせ（比率に応じた動的幅）
        SPLIT     // 分割表示
    }

    /**
     * マナオーブ（ORB_2）とエネルギーオーブ（ORB_3）の入れ替えモード
     */
    public enum OrbResourceSwapMode {
        OFF,        // マナがORB_2（メイン）、エネルギーがORB_3（サブ）
        SWAPPED,    // エネルギーがORB_2（メイン）、マナがORB_3（サブ）
        AUTO,       // エネルギー > マナ の場合に自動スワップ（最大値/現在値比較）
        SKILL_COST  // ホットバースキルのエネルギーコスト合計 > マナコスト合計 の場合に自動スワップ
    }

    private boolean showOrbText = true;
    private boolean compactNumbers = false;
    private boolean energyCompact = false;
    private float textScale = 1.97f;
    private float aboveTextScale = 1.99f;
    private float energyTextScale = 1.77f;
    private float msTextScale = 2.22f;
    private Orb1MsMode orb1MsMode = Orb1MsMode.OVERLAP;
    private OrbResourceSwapMode orbSwapMode = OrbResourceSwapMode.OFF;
    private boolean hideOrb1SmallerValue = false;
    private boolean hideLowerHpMsGaugeOrb1 = false;
    private OrbTextPosition textPosition = OrbTextPosition.ABOVE;
    private float aboveOrbOffsetY = 2.99f;
    private float aboveOrbOffsetX = 4.99f;
    private float aboveIndividualOrbOffsetY = 5.77f;
    private float aboveIndividualOrbOffsetX = 8.96f;
    private boolean orbTextShadow = true;

    private OrbTextConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static float[] getScaleOptions() {
        return SCALE_OPTIONS;
    }

    public static String getScaleLabel(float scale) {
        return ((int) (scale * 100)) + "%";
    }

    public static OrbTextConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new OrbTextConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("showOrbText")) showOrbText = obj.get("showOrbText").getAsBoolean();
        if (obj.has("compactNumbers")) compactNumbers = obj.get("compactNumbers").getAsBoolean();
        if (obj.has("roundEnergyOrb")) energyCompact = obj.get("roundEnergyOrb").getAsBoolean();
        if (obj.has("energyCompact")) energyCompact = obj.get("energyCompact").getAsBoolean();
        if (obj.has("textScale")) textScale = obj.get("textScale").getAsFloat();
        if (obj.has("aboveTextScale")) aboveTextScale = obj.get("aboveTextScale").getAsFloat();
        if (obj.has("energyTextScale")) energyTextScale = obj.get("energyTextScale").getAsFloat();
        if (obj.has("msTextScale")) {
            msTextScale = obj.get("msTextScale").getAsFloat();
        } else if (obj.has("esTextScale")) {
            msTextScale = obj.get("esTextScale").getAsFloat();
        }
        if (obj.has("orb1MsMode")) {
            try {
                orb1MsMode = Orb1MsMode.valueOf(obj.get("orb1MsMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                orb1MsMode = Orb1MsMode.OVERLAP;
            }
        } else if (obj.has("orb1EsMode")) {
            try {
                orb1MsMode = Orb1MsMode.valueOf(obj.get("orb1EsMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                orb1MsMode = Orb1MsMode.OVERLAP;
            }
        } else if (obj.has("splitOrb1")) {
            orb1MsMode = obj.get("splitOrb1").getAsBoolean() ? Orb1MsMode.SPLIT : Orb1MsMode.OVERLAP;
        }
        if (obj.has("orbSwapMode")) {
            try {
                orbSwapMode = OrbResourceSwapMode.valueOf(obj.get("orbSwapMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                orbSwapMode = OrbResourceSwapMode.OFF;
            }
        }
        if (obj.has("hideOrb1SmallerValue")) {
            hideOrb1SmallerValue = obj.get("hideOrb1SmallerValue").getAsBoolean();
        } else if (obj.has("showOrb1SmallerValue")) {
            hideOrb1SmallerValue = !obj.get("showOrb1SmallerValue").getAsBoolean();
        }
        if (obj.has("hideLowerHpMsGaugeOrb1")) {
            hideLowerHpMsGaugeOrb1 = obj.get("hideLowerHpMsGaugeOrb1").getAsBoolean();
        } else if (obj.has("hideLowerHpEsGaugeOrb1")) {
            hideLowerHpMsGaugeOrb1 = obj.get("hideLowerHpEsGaugeOrb1").getAsBoolean();
        }
        if (obj.has("textPosition")) {
            try {
                textPosition = OrbTextPosition.valueOf(obj.get("textPosition").getAsString());
            } catch (IllegalArgumentException ignored) {
                textPosition = OrbTextPosition.CENTER;
            }
        }
        if (obj.has("aboveOrbOffsetY")) aboveOrbOffsetY = obj.get("aboveOrbOffsetY").getAsFloat();
        if (obj.has("aboveOrbOffsetX")) aboveOrbOffsetX = obj.get("aboveOrbOffsetX").getAsFloat();
        if (obj.has("aboveIndividualOrbOffsetY")) aboveIndividualOrbOffsetY = obj.get("aboveIndividualOrbOffsetY").getAsFloat();
        if (obj.has("aboveIndividualOrbOffsetX")) aboveIndividualOrbOffsetX = obj.get("aboveIndividualOrbOffsetX").getAsFloat();
        if (obj.has("orbTextShadow")) {
            orbTextShadow = obj.get("orbTextShadow").getAsBoolean();
        } else if (obj.has("aboveTextShadow")) {
            orbTextShadow = obj.get("aboveTextShadow").getAsBoolean();
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("showOrbText", showOrbText);
        obj.addProperty("compactNumbers", compactNumbers);
        obj.addProperty("energyCompact", energyCompact);
        obj.addProperty("textScale", textScale);
        obj.addProperty("aboveTextScale", aboveTextScale);
        obj.addProperty("energyTextScale", energyTextScale);
        obj.addProperty("msTextScale", msTextScale);
        obj.addProperty("orb1MsMode", orb1MsMode.name());
        obj.addProperty("orbSwapMode", orbSwapMode.name());
        obj.addProperty("hideOrb1SmallerValue", hideOrb1SmallerValue);
        obj.addProperty("hideLowerHpMsGaugeOrb1", hideLowerHpMsGaugeOrb1);
        obj.addProperty("textPosition", textPosition.name());
        obj.addProperty("aboveOrbOffsetY", aboveOrbOffsetY);
        obj.addProperty("aboveOrbOffsetX", aboveOrbOffsetX);
        obj.addProperty("aboveIndividualOrbOffsetY", aboveIndividualOrbOffsetY);
        obj.addProperty("aboveIndividualOrbOffsetX", aboveIndividualOrbOffsetX);
        obj.addProperty("orbTextShadow", orbTextShadow);
    }

    public boolean isShowOrbText() { return showOrbText; }
    public void setShowOrbText(boolean show) { this.showOrbText = show; }

    public boolean isCompactNumbers() { return compactNumbers; }
    public void setCompactNumbers(boolean compact) { this.compactNumbers = compact; }

    public float getTextScale() { return textScale; }
    public void setTextScale(float scale) { this.textScale = scale; }

    public float getAboveTextScale() { return aboveTextScale; }
    public void setAboveTextScale(float scale) { this.aboveTextScale = scale; }

    public float cycleTextScale() {
        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            if (Float.compare(SCALE_OPTIONS[i], textScale) == 0) {
                textScale = SCALE_OPTIONS[(i + 1) % SCALE_OPTIONS.length];
                return textScale;
            }
        }
        textScale = 1.0f;
        return textScale;
    }

    public float getEnergyTextScale() { return energyTextScale; }
    public void setEnergyTextScale(float scale) { this.energyTextScale = scale; }

    public float cycleEnergyTextScale() {
        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            if (Float.compare(SCALE_OPTIONS[i], energyTextScale) == 0) {
                energyTextScale = SCALE_OPTIONS[(i + 1) % SCALE_OPTIONS.length];
                return energyTextScale;
            }
        }
        energyTextScale = 1.0f;
        return energyTextScale;
    }

    public float getMsTextScale() { return msTextScale; }
    public void setMsTextScale(float scale) { this.msTextScale = scale; }

    public float cycleMsTextScale() {
        for (int i = 0; i < SCALE_OPTIONS.length; i++) {
            if (Float.compare(SCALE_OPTIONS[i], msTextScale) == 0) {
                msTextScale = SCALE_OPTIONS[(i + 1) % SCALE_OPTIONS.length];
                return msTextScale;
            }
        }
        msTextScale = 1.0f;
        return msTextScale;
    }

    @Deprecated
    public float getEsTextScale() { return getMsTextScale(); }
    @Deprecated
    public void setEsTextScale(float scale) { setMsTextScale(scale); }
    @Deprecated
    public float cycleEsTextScale() { return cycleMsTextScale(); }

    public boolean isEnergyCompact() { return energyCompact; }
    public void setEnergyCompact(boolean compact) { this.energyCompact = compact; }

    public Orb1MsMode getOrb1MsMode() { return orb1MsMode; }
    public void setOrb1MsMode(Orb1MsMode mode) { this.orb1MsMode = mode; }
    public boolean isSplitOrb1() { return orb1MsMode == Orb1MsMode.SPLIT; }
    public void setSplitOrb1(boolean split) { this.orb1MsMode = split ? Orb1MsMode.SPLIT : Orb1MsMode.OVERLAP; }
    public boolean isOverlapHpMsOrb1() { return orb1MsMode == Orb1MsMode.OVERLAP; }

    public Orb1MsMode cycleOrb1MsMode() {
        Orb1MsMode[] values = Orb1MsMode.values();
        orb1MsMode = values[(orb1MsMode.ordinal() + 1) % values.length];
        return orb1MsMode;
    }

    @Deprecated
    public boolean isOverlapHpEsOrb1() { return isOverlapHpMsOrb1(); }

    public OrbResourceSwapMode getOrbSwapMode() { return orbSwapMode; }
    public void setOrbSwapMode(OrbResourceSwapMode mode) { this.orbSwapMode = mode; }

    public OrbResourceSwapMode cycleOrbSwapMode() {
        OrbResourceSwapMode[] values = OrbResourceSwapMode.values();
        orbSwapMode = values[(orbSwapMode.ordinal() + 1) % values.length];
        return orbSwapMode;
    }

    public boolean isHideOrb1SmallerValue() { return hideOrb1SmallerValue; }
    public void setHideOrb1SmallerValue(boolean hide) { this.hideOrb1SmallerValue = hide; }

    public boolean isHideLowerHpMsGaugeOrb1() { return hideLowerHpMsGaugeOrb1; }
    public void setHideLowerHpMsGaugeOrb1(boolean hide) { this.hideLowerHpMsGaugeOrb1 = hide; }

    @Deprecated
    public boolean isHideLowerHpEsGaugeOrb1() { return isHideLowerHpMsGaugeOrb1(); }
    @Deprecated
    public void setHideLowerHpEsGaugeOrb1(boolean hide) { setHideLowerHpMsGaugeOrb1(hide); }

    public OrbTextPosition getTextPosition() { return textPosition; }
    public void setTextPosition(OrbTextPosition position) {
        this.textPosition = position;
    }

    /**
     * ABOVEモード時のOrb上辺からのYオフセット（ピクセル、テクスチャ座標系）
     */
    public float getAboveOrbOffsetY() {
        if (textPosition == OrbTextPosition.ABOVE) {
            return aboveIndividualOrbOffsetY;
        }
        return aboveOrbOffsetY;
    }
    
    public void setAboveOrbOffsetY(float offset) {
        if (textPosition == OrbTextPosition.ABOVE) {
            this.aboveIndividualOrbOffsetY = offset;
        } else {
            this.aboveOrbOffsetY = offset;
        }
    }

    /**
     * ABOVEモード時の左右オフセット（正: 左は左へ・右は右へ拡げる）
     */
    public float getAboveOrbOffsetX() {
        if (textPosition == OrbTextPosition.ABOVE) {
            return aboveIndividualOrbOffsetX;
        }
        return aboveOrbOffsetX;
    }
    
    public void setAboveOrbOffsetX(float offset) {
        if (textPosition == OrbTextPosition.ABOVE) {
            this.aboveIndividualOrbOffsetX = offset;
        } else {
            this.aboveOrbOffsetX = offset;
        }
    }

    public float getAboveIndividualOrbOffsetY() { return aboveIndividualOrbOffsetY; }
    public void setAboveIndividualOrbOffsetY(float offset) { this.aboveIndividualOrbOffsetY = offset; }

    public float getAboveIndividualOrbOffsetX() { return aboveIndividualOrbOffsetX; }
    public void setAboveIndividualOrbOffsetX(float offset) { this.aboveIndividualOrbOffsetX = offset; }

    public boolean isOrbTextShadow() { return orbTextShadow; }
    public void setOrbTextShadow(boolean shadow) { this.orbTextShadow = shadow; }

    /**
     * 表示位置をサイクル切替（CENTER → ABOVE → CENTER）
     */
    public OrbTextPosition cycleTextPosition() {
        OrbTextPosition[] values = OrbTextPosition.values();
        textPosition = values[(textPosition.ordinal() + 1) % values.length];
        return textPosition;
    }
}
