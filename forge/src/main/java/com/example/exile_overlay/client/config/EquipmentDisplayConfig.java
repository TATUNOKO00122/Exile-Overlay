package com.example.exile_overlay.client.config;

import com.google.gson.JsonObject;

/**
 * 装備表示設定（アイコンサイズ、パーセント表示等）を管理する設定クラス。
 */
public class EquipmentDisplayConfig extends AbstractConfigSection {

    private static final String SECTION_ID = "equipment_display";
    private static final String FILE_NAME = "exile_overlay_equipment_display.json";
    private static volatile EquipmentDisplayConfig instance;
    private static final Object LOCK = new Object();

    public enum QuickLootMode {
        LOOT,
        DROP
    }

    /**
     * レベル数値の表示モード。
     * BOTH: バニラ（緑） / MS（黄）の両方表示、MS_ONLY: MS Levelのみ、VANILLA_ONLY: バニラLevelのみ。
     */
    public enum LevelDisplayMode {
        BOTH,
        MS_ONLY,
        VANILLA_ONLY
    }

    /**
     * スキルクールダウンの表示モード。
     * RADIAL: 時計回りに開く扇形（ラジアルスイープ）、VERTICAL: 上から下に水平線が下がっていく垂直降下型。
     */
    public enum CooldownDisplayType {
        RADIAL,
        VERTICAL
    }

    private boolean usePercentage = true;
    private boolean enableShadow = true;
    private boolean quickLootEnabled = false;
    private boolean autoQuickLootEnabled = false;
    private QuickLootMode autoQuickLootMode = QuickLootMode.LOOT;
    private boolean keyQuickLootEnabled = true;
    private QuickLootMode keyQuickLootMode = QuickLootMode.DROP;
    private boolean disableMnsHpBar = false;
    private boolean cancelMnsRpgBars = true;
    private boolean cancelMnsSpellHotbar = true;
    private boolean cancelMnsCastBar = true;
    private boolean cancelMnsStatusEffects = true;
    private boolean cancelMnsExpActionBar = false;
    private boolean cancelBotaniaMana = false;
    private boolean cancelDungeonRealmScoreboard = false;
    private boolean autoSortLootrChest = true;
    private LevelDisplayMode levelDisplayMode = LevelDisplayMode.BOTH;
    private boolean showTargetAffixStats = false;
    private boolean showTargetMobEffects = true;
    private boolean showBossMobEffects = true;
    private boolean enableBossPortalMarker = false;
    private boolean showEmptySkillSlots = false;
    private boolean showSkillCooldownNumber = true;
    private boolean showSkillSummonCount = true;
    private boolean enableOrbNoise = true;
    private boolean enableLiquidShadow = false;
    private boolean enableOrbInnerShadow = true;
    private boolean simpleSkillKeybindDisplay = false;
    private boolean simpleSkillChargeMaxDisplay = false;
    private boolean simpleBuffStackDisplay = false;
    private CooldownDisplayType cooldownDisplayType = CooldownDisplayType.VERTICAL;

    private EquipmentDisplayConfig() {
        super(SECTION_ID, FILE_NAME, true);
    }

    public static EquipmentDisplayConfig getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new EquipmentDisplayConfig();
                    instance.load();
                }
            }
        }
        return instance;
    }

    @Override
    protected void deserialize(JsonObject obj) {
        if (obj.has("usePercentage")) usePercentage = obj.get("usePercentage").getAsBoolean();
        if (obj.has("enableShadow")) enableShadow = obj.get("enableShadow").getAsBoolean();
        if (obj.has("quickLootEnabled")) quickLootEnabled = obj.get("quickLootEnabled").getAsBoolean();
        if (obj.has("autoQuickLootEnabled")) autoQuickLootEnabled = obj.get("autoQuickLootEnabled").getAsBoolean();
        if (obj.has("autoQuickLootMode")) {
            try {
                autoQuickLootMode = QuickLootMode.valueOf(obj.get("autoQuickLootMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                autoQuickLootMode = QuickLootMode.LOOT;
            }
        }
        if (obj.has("keyQuickLootEnabled")) keyQuickLootEnabled = obj.get("keyQuickLootEnabled").getAsBoolean();
        if (obj.has("keyQuickLootMode")) {
            try {
                keyQuickLootMode = QuickLootMode.valueOf(obj.get("keyQuickLootMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                keyQuickLootMode = QuickLootMode.DROP;
            }
        }
        if (obj.has("disableMnsHpBar")) disableMnsHpBar = obj.get("disableMnsHpBar").getAsBoolean();
        if (obj.has("cancelMnsRpgBars")) cancelMnsRpgBars = obj.get("cancelMnsRpgBars").getAsBoolean();
        if (obj.has("cancelMnsSpellHotbar")) cancelMnsSpellHotbar = obj.get("cancelMnsSpellHotbar").getAsBoolean();
        if (obj.has("cancelMnsCastBar")) cancelMnsCastBar = obj.get("cancelMnsCastBar").getAsBoolean();
        if (obj.has("cancelMnsStatusEffects")) cancelMnsStatusEffects = obj.get("cancelMnsStatusEffects").getAsBoolean();
        if (obj.has("cancelMnsExpActionBar")) cancelMnsExpActionBar = obj.get("cancelMnsExpActionBar").getAsBoolean();
        if (obj.has("cancelBotaniaMana")) cancelBotaniaMana = obj.get("cancelBotaniaMana").getAsBoolean();
        if (obj.has("cancelDungeonRealmScoreboard")) cancelDungeonRealmScoreboard = obj.get("cancelDungeonRealmScoreboard").getAsBoolean();
        if (obj.has("autoSortLootrChest")) autoSortLootrChest = obj.get("autoSortLootrChest").getAsBoolean();
        if (obj.has("levelDisplayMode")) {
            try {
                levelDisplayMode = LevelDisplayMode.valueOf(obj.get("levelDisplayMode").getAsString());
            } catch (IllegalArgumentException ignored) {
                levelDisplayMode = LevelDisplayMode.BOTH;
            }
        }
        if (obj.has("showTargetAffixStats")) showTargetAffixStats = obj.get("showTargetAffixStats").getAsBoolean();
        if (obj.has("showTargetMobEffects")) showTargetMobEffects = obj.get("showTargetMobEffects").getAsBoolean();
        if (obj.has("showBossMobEffects")) showBossMobEffects = obj.get("showBossMobEffects").getAsBoolean();
        if (obj.has("enableBossPortalMarker")) enableBossPortalMarker = obj.get("enableBossPortalMarker").getAsBoolean();
        if (obj.has("showEmptySkillSlots")) showEmptySkillSlots = obj.get("showEmptySkillSlots").getAsBoolean();
        if (obj.has("showSkillCooldownNumber")) showSkillCooldownNumber = obj.get("showSkillCooldownNumber").getAsBoolean();
        if (obj.has("showSkillSummonCount")) showSkillSummonCount = obj.get("showSkillSummonCount").getAsBoolean();
        if (obj.has("enableOrbNoise")) enableOrbNoise = obj.get("enableOrbNoise").getAsBoolean();
        if (obj.has("enableLiquidShadow")) enableLiquidShadow = obj.get("enableLiquidShadow").getAsBoolean();
        if (obj.has("enableOrbInnerShadow")) enableOrbInnerShadow = obj.get("enableOrbInnerShadow").getAsBoolean();
        if (obj.has("simpleSkillKeybindDisplay")) simpleSkillKeybindDisplay = obj.get("simpleSkillKeybindDisplay").getAsBoolean();
        if (obj.has("simpleSkillChargeMaxDisplay")) simpleSkillChargeMaxDisplay = obj.get("simpleSkillChargeMaxDisplay").getAsBoolean();
        if (obj.has("simpleBuffStackDisplay")) simpleBuffStackDisplay = obj.get("simpleBuffStackDisplay").getAsBoolean();
        if (obj.has("cooldownDisplayType")) {
            try {
                cooldownDisplayType = CooldownDisplayType.valueOf(obj.get("cooldownDisplayType").getAsString());
            } catch (IllegalArgumentException ignored) {
                cooldownDisplayType = CooldownDisplayType.VERTICAL;
            }
        }
    }

    @Override
    protected void serialize(JsonObject obj) {
        obj.addProperty("usePercentage", usePercentage);
        obj.addProperty("enableShadow", enableShadow);
        obj.addProperty("quickLootEnabled", quickLootEnabled);
        obj.addProperty("autoQuickLootEnabled", autoQuickLootEnabled);
        obj.addProperty("autoQuickLootMode", autoQuickLootMode.name());
        obj.addProperty("keyQuickLootEnabled", keyQuickLootEnabled);
        obj.addProperty("keyQuickLootMode", keyQuickLootMode.name());
        obj.addProperty("disableMnsHpBar", disableMnsHpBar);
        obj.addProperty("cancelMnsRpgBars", cancelMnsRpgBars);
        obj.addProperty("cancelMnsSpellHotbar", cancelMnsSpellHotbar);
        obj.addProperty("cancelMnsCastBar", cancelMnsCastBar);
        obj.addProperty("cancelMnsStatusEffects", cancelMnsStatusEffects);
        obj.addProperty("cancelMnsExpActionBar", cancelMnsExpActionBar);
        obj.addProperty("cancelBotaniaMana", cancelBotaniaMana);
        obj.addProperty("cancelDungeonRealmScoreboard", cancelDungeonRealmScoreboard);
        obj.addProperty("autoSortLootrChest", autoSortLootrChest);
        obj.addProperty("levelDisplayMode", levelDisplayMode.name());
        obj.addProperty("showTargetAffixStats", showTargetAffixStats);
        obj.addProperty("showTargetMobEffects", showTargetMobEffects);
        obj.addProperty("showBossMobEffects", showBossMobEffects);
        obj.addProperty("enableBossPortalMarker", enableBossPortalMarker);
        obj.addProperty("showEmptySkillSlots", showEmptySkillSlots);
        obj.addProperty("showSkillCooldownNumber", showSkillCooldownNumber);
        obj.addProperty("showSkillSummonCount", showSkillSummonCount);
        obj.addProperty("enableOrbNoise", enableOrbNoise);
        obj.addProperty("enableLiquidShadow", enableLiquidShadow);
        obj.addProperty("enableOrbInnerShadow", enableOrbInnerShadow);
        obj.addProperty("simpleSkillKeybindDisplay", simpleSkillKeybindDisplay);
        obj.addProperty("simpleSkillChargeMaxDisplay", simpleSkillChargeMaxDisplay);
        obj.addProperty("simpleBuffStackDisplay", simpleBuffStackDisplay);
        obj.addProperty("cooldownDisplayType", cooldownDisplayType.name());
    }

    public boolean isUsePercentage() { return usePercentage; }
    public void setUsePercentage(boolean use) { this.usePercentage = use; }

    public boolean isEnableShadow() { return enableShadow; }
    public void setEnableShadow(boolean enable) { this.enableShadow = enable; }

    public boolean isQuickLootEnabled() { return quickLootEnabled; }
    public void setQuickLootEnabled(boolean enabled) { this.quickLootEnabled = enabled; }

    public boolean isAutoQuickLootEnabled() { return autoQuickLootEnabled; }
    public void setAutoQuickLootEnabled(boolean enabled) { this.autoQuickLootEnabled = enabled; }

    public QuickLootMode getAutoQuickLootMode() { return autoQuickLootMode; }
    public void setAutoQuickLootMode(QuickLootMode mode) { this.autoQuickLootMode = mode; }

    public boolean isKeyQuickLootEnabled() { return keyQuickLootEnabled; }
    public void setKeyQuickLootEnabled(boolean enabled) { this.keyQuickLootEnabled = enabled; }

    public QuickLootMode getKeyQuickLootMode() { return keyQuickLootMode; }
    public void setKeyQuickLootMode(QuickLootMode mode) { this.keyQuickLootMode = mode; }

    public boolean isDisableMnsHpBar() { return disableMnsHpBar; }
    public void setDisableMnsHpBar(boolean disable) { this.disableMnsHpBar = disable; }

    public boolean isCancelMnsRpgBars() { return cancelMnsRpgBars; }
    public void setCancelMnsRpgBars(boolean cancel) { this.cancelMnsRpgBars = cancel; }

    public boolean isCancelMnsSpellHotbar() { return cancelMnsSpellHotbar; }
    public void setCancelMnsSpellHotbar(boolean cancel) { this.cancelMnsSpellHotbar = cancel; }

    public boolean isCancelMnsCastBar() { return cancelMnsCastBar; }
    public void setCancelMnsCastBar(boolean cancel) { this.cancelMnsCastBar = cancel; }

    public boolean isCancelMnsStatusEffects() { return cancelMnsStatusEffects; }
    public void setCancelMnsStatusEffects(boolean cancel) { this.cancelMnsStatusEffects = cancel; }

    public boolean isCancelMnsExpActionBar() { return cancelMnsExpActionBar; }
    public void setCancelMnsExpActionBar(boolean cancel) { this.cancelMnsExpActionBar = cancel; }

    public boolean isCancelBotaniaMana() { return cancelBotaniaMana; }
    public void setCancelBotaniaMana(boolean cancel) { this.cancelBotaniaMana = cancel; }

    public boolean isCancelDungeonRealmScoreboard() { return cancelDungeonRealmScoreboard; }
    public void setCancelDungeonRealmScoreboard(boolean cancel) { this.cancelDungeonRealmScoreboard = cancel; }

    public boolean isAutoSortLootrChest() { return autoSortLootrChest; }
    public void setAutoSortLootrChest(boolean enabled) { this.autoSortLootrChest = enabled; }

    public LevelDisplayMode getLevelDisplayMode() { return levelDisplayMode; }
    public void setLevelDisplayMode(LevelDisplayMode mode) { this.levelDisplayMode = mode; }

    public boolean isShowTargetAffixStats() { return showTargetAffixStats; }
    public void setShowTargetAffixStats(boolean show) { this.showTargetAffixStats = show; }

    public boolean isShowTargetMobEffects() { return showTargetMobEffects; }
    public void setShowTargetMobEffects(boolean show) { this.showTargetMobEffects = show; }

    public boolean isShowBossMobEffects() { return showBossMobEffects; }
    public void setShowBossMobEffects(boolean show) { this.showBossMobEffects = show; }

    public boolean isEnableBossPortalMarker() { return enableBossPortalMarker; }
    public void setEnableBossPortalMarker(boolean enable) { this.enableBossPortalMarker = enable; }

    public boolean isShowEmptySkillSlots() { return showEmptySkillSlots; }
    public void setShowEmptySkillSlots(boolean show) { this.showEmptySkillSlots = show; }

    public boolean isShowSkillCooldownNumber() { return showSkillCooldownNumber; }
    public void setShowSkillCooldownNumber(boolean show) { this.showSkillCooldownNumber = show; }

    public boolean isShowSkillSummonCount() { return showSkillSummonCount; }
    public void setShowSkillSummonCount(boolean show) { this.showSkillSummonCount = show; }

    public boolean isEnableOrbNoise() { return enableOrbNoise; }
    public void setEnableOrbNoise(boolean enable) { this.enableOrbNoise = enable; }

    public boolean isEnableLiquidShadow() { return enableLiquidShadow; }
    public void setEnableLiquidShadow(boolean enable) { this.enableLiquidShadow = enable; }

    public boolean isEnableOrbInnerShadow() { return enableOrbInnerShadow; }
    public void setEnableOrbInnerShadow(boolean enable) { this.enableOrbInnerShadow = enable; }

    public boolean isSimpleSkillKeybindDisplay() { return simpleSkillKeybindDisplay; }
    public void setSimpleSkillKeybindDisplay(boolean simple) { this.simpleSkillKeybindDisplay = simple; }

    public boolean isSimpleSkillChargeMaxDisplay() { return simpleSkillChargeMaxDisplay; }
    public void setSimpleSkillChargeMaxDisplay(boolean show) { this.simpleSkillChargeMaxDisplay = show; }

    public boolean isSimpleBuffStackDisplay() { return simpleBuffStackDisplay; }
    public void setSimpleBuffStackDisplay(boolean simple) { this.simpleBuffStackDisplay = simple; }

    public CooldownDisplayType getCooldownDisplayType() { return cooldownDisplayType; }
    public void setCooldownDisplayType(CooldownDisplayType type) {
        if (type != null) {
            this.cooldownDisplayType = type;
        }
    }

    public void resetToDefaults() {
        this.usePercentage = true;
        this.enableShadow = true;
        this.quickLootEnabled = false;
        this.autoQuickLootEnabled = false;
        this.autoQuickLootMode = QuickLootMode.LOOT;
        this.keyQuickLootEnabled = true;
        this.keyQuickLootMode = QuickLootMode.DROP;
        this.disableMnsHpBar = false;
        this.cancelMnsRpgBars = true;
        this.cancelMnsSpellHotbar = true;
        this.cancelMnsCastBar = true;
        this.cancelMnsStatusEffects = true;
        this.cancelMnsExpActionBar = false;
        this.cancelBotaniaMana = false;
        this.cancelDungeonRealmScoreboard = false;
        this.autoSortLootrChest = true;
        this.levelDisplayMode = LevelDisplayMode.BOTH;
        this.showTargetAffixStats = false;
        this.showTargetMobEffects = true;
        this.showBossMobEffects = true;
        this.enableBossPortalMarker = false;
        this.showEmptySkillSlots = false;
        this.showSkillCooldownNumber = true;
        this.showSkillSummonCount = true;
        this.enableOrbNoise = true;
        this.enableLiquidShadow = false;
        this.enableOrbInnerShadow = true;
        this.simpleSkillKeybindDisplay = false;
        this.simpleSkillChargeMaxDisplay = false;
        this.simpleBuffStackDisplay = false;
        this.cooldownDisplayType = CooldownDisplayType.VERTICAL;
    }
}
