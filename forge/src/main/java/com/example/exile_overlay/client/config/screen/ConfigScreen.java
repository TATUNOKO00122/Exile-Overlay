package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.BuffOverlayFilterConfig;
import com.example.exile_overlay.client.config.DropSoundConfig;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.ExileOverlayConfigManager;
import com.example.exile_overlay.client.config.HudFontConfig;
import com.example.exile_overlay.client.config.HudFontPreset;
import com.example.exile_overlay.client.config.OrbColorConfig;
import com.example.exile_overlay.client.config.OrbSmoothConfig;
import com.example.exile_overlay.client.config.OrbTextConfig;
import com.example.exile_overlay.client.config.SkillBuffFilterConfig;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.list.ConfigEntryList;
import com.example.exile_overlay.client.config.screen.tab.DamagePopupTab;
import com.example.exile_overlay.client.config.screen.tab.ExtensionsTab;
import com.example.exile_overlay.client.config.screen.tab.GeneralTab;
import com.example.exile_overlay.client.config.screen.tab.HudDisplayTab;
import com.example.exile_overlay.client.config.screen.tab.IConfigTab;
import com.example.exile_overlay.client.config.screen.tab.OrbTab;
import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.FontPreset;
import com.example.exile_overlay.client.render.DayCounterConfig;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MOD全体の総合設定画面。
 * 6つのタブ（一般、HUD表示、オーブ、バフ、戦闘、拡張連携）を備える。
 */
public class ConfigScreen extends Screen {

    private final Screen lastScreen;
    private final List<IConfigTab> tabs = new ArrayList<>();
    private int currentTab = 0;

    private final List<Button> tabButtons = new ArrayList<>();
    private ConfigEntryList entryList;

    public ConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.exile_overlay.config.title"));
        this.lastScreen = lastScreen;
        initTabs();
    }

    private void initTabs() {
        tabs.add(new GeneralTab());
        tabs.add(new HudDisplayTab());
        tabs.add(new OrbTab());
        tabs.add(new DamagePopupTab());
        tabs.add(new ExtensionsTab());
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.tabButtons.clear();

        int leftPanelW = 96;
        int leftPanelX = 16;
        int leftPanelY = 28;
        int leftPanelH = this.height - 36;

        int rightPanelX = leftPanelX + leftPanelW + 8;
        int rightPanelY = 28;
        int rightPanelW = this.width - rightPanelX - 16;
        int rightPanelH = this.height - 36;

        // 1. 左側タブボタン群
        int tabY = leftPanelY + 8;
        for (int i = 0; i < tabs.size(); i++) {
            final int index = i;
            Button tabBtn = Button.builder(tabs.get(i).getTitle(), b -> switchTab(index))
                    .bounds(leftPanelX + 6, tabY, leftPanelW - 12, 20)
                    .build();
            tabBtn.active = (currentTab != i);
            this.tabButtons.add(tabBtn);
            this.addRenderableWidget(tabBtn);
            tabY += 23;
        }

        // 2. 左側下部ボタン（リセット / 完了）
        Button btnReset = Button.builder(Component.translatable("button.exile_overlay.reset"), b -> resetToDefaults())
                .bounds(leftPanelX + 6, leftPanelY + leftPanelH - 48, leftPanelW - 12, 20)
                .tooltip(Tooltip.create(Component.translatable("button.exile_overlay.reset.tooltip")))
                .build();
        this.addRenderableWidget(btnReset);

        Button btnDone = Button.builder(CommonComponents.GUI_DONE, b -> {
            saveConfig();
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(leftPanelX + 6, leftPanelY + leftPanelH - 24, leftPanelW - 12, 20).build();
        this.addRenderableWidget(btnDone);

        // 3. 右側リストウィジェット
        this.entryList = new ConfigEntryList(rightPanelX, rightPanelY, rightPanelW, rightPanelH);
        loadCurrentTabEntries();
    }

    public void switchTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < tabs.size()) {
            this.currentTab = tabIndex;
            for (int i = 0; i < tabButtons.size(); i++) {
                tabButtons.get(i).active = (i != currentTab);
            }
            if (entryList != null) {
                entryList.resetScroll();
            }
            loadCurrentTabEntries();
        }
    }

    public void rebuildCurrentTab() {
        loadCurrentTabEntries();
    }

    private void loadCurrentTabEntries() {
        if (entryList != null && currentTab >= 0 && currentTab < tabs.size()) {
            entryList.setEntries(tabs.get(currentTab).buildEntries(this));
        }
    }

    public void handlePresetButtonClick() {
        ExileOverlayConfigManager mgr = ExileOverlayConfigManager.getInstance();
        if (mgr.hasBackupSnapshot()) {
            mgr.restoreBackupSnapshot();
        } else {
            mgr.createBackupSnapshot();
            applyVanillaMnsDefaultPreset();
        }
        rebuildCurrentTab();
    }

    private void applyVanillaMnsDefaultPreset() {
        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();
        equipConfig.setCancelMnsRpgBars(false);
        equipConfig.setCancelMnsSpellHotbar(false);
        equipConfig.setCancelMnsCastBar(false);
        equipConfig.setCancelMnsStatusEffects(false);
        equipConfig.setCancelMnsExpActionBar(false);
        equipConfig.setDisableMnsHpBar(false);
        MethodHandlesUtil.setNeatHpBarEnabled(true);
        equipConfig.setCancelBotaniaMana(false);
        equipConfig.setCancelDungeonRealmScoreboard(false);
        equipConfig.save();
        ExileOverlayConfigManager.getInstance().saveAll();

        HudPositionManager.getInstance().hideAllElements();

        TrackerConfig trackerConfig = TrackerConfig.getInstance();
        trackerConfig.setEnabled(false);
        trackerConfig.setMaxSkillsShown(20);
        trackerConfig.save();
    }

    private void resetToDefaults() {
        ExileOverlayConfigManager.getInstance().clearBackupSnapshot();

        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        config.resetToDefaults();
        config.save();

        MethodHandlesUtil.setNeatHpBarEnabled(false);

        DamagePopupConfig damageConfig = DamagePopupConfig.getInstance();
        damageConfig.setShowDamage(false);
        damageConfig.setShowHealing(true);
        damageConfig.setShowPlayerDamage(false);
        damageConfig.setShowPlayerHealing(true);
        damageConfig.setEnableShadow(false);
        damageConfig.setFontPreset(FontPreset.GAME_POCKET);
        damageConfig.setBaseScale(0.018f);
        damageConfig.setCriticalScale(0.032f);
        damageConfig.setDisplayDuration(20);
        damageConfig.setFadeInDuration(5);
        damageConfig.setFadeOutDuration(10);
        damageConfig.setMaxDamageTexts(10);
        damageConfig.setPopupHeightRatio(0.8f);
        damageConfig.setRoundDamageNumbers(true);
        damageConfig.setCompactNumbers(false);
        damageConfig.setDecimalThreshold(15.0f);
        damageConfig.setEnableDamageScale(true);
        damageConfig.setNormalDamageColor(0xFFFFFF);
        damageConfig.setCriticalDamageColor(0xFFFF55);
        damageConfig.setPhysicalDamageColor(0xFFAA00);
        damageConfig.setHealingColor(0x55FF55);
        damageConfig.setFireDamageColor(0xFF5555);
        damageConfig.setIceDamageColor(0x55FFFF);
        damageConfig.setLightningDamageColor(0xFFFF55);
        damageConfig.setNatureDamageColor(0xFFFF55);
        damageConfig.setPoisonDamageColor(0x55FF55);
        damageConfig.setMagicDamageColor(0xAA00AA);
        damageConfig.setElementalDamageColor(0xFF77FF);
        damageConfig.setWitherDamageColor(0x2F2F2F);
        damageConfig.save();

        EntityHealthBarConfig hpBarConfig = EntityHealthBarConfig.getInstance();
        hpBarConfig.setEnabled(false);
        hpBarConfig.setShowPoison(false);
        hpBarConfig.setShowBleed(false);
        hpBarConfig.setShowFriendlyColor(true);
        hpBarConfig.setMaxDistance(24);
        hpBarConfig.setHeightAbove(0.5);
        hpBarConfig.setBarWidth(30);
        hpBarConfig.setBarHeight(2);
        hpBarConfig.setScale(1.0f);
        hpBarConfig.setDisplayDuration(5);
        hpBarConfig.setHealthBarColor("B02020");
        hpBarConfig.setPoisonBarColor("246E07");
        hpBarConfig.setBleedBarColor("540606");
        hpBarConfig.setFriendlyBarColor("2D8B2D");
        hpBarConfig.setBlacklist(new ArrayList<>(EntityHealthBarConfig.DEFAULT_BLACKLIST));
        hpBarConfig.save();

        OrbTextConfig orbTextConfig = OrbTextConfig.getInstance();
        orbTextConfig.setShowOrbText(true);
        orbTextConfig.setCompactNumbers(false);
        orbTextConfig.setEnergyCompact(true);
        orbTextConfig.setTextScale(1.97f);
        orbTextConfig.setAboveTextScale(1.99f);
        orbTextConfig.setEnergyTextScale(1.77f);
        orbTextConfig.setEsTextScale(2.22f);
        orbTextConfig.setOrb1EsMode(OrbTextConfig.Orb1EsMode.OVERLAP);
        orbTextConfig.setOrbSwapMode(OrbTextConfig.OrbResourceSwapMode.OFF);
        orbTextConfig.setHideOrb1SmallerValue(false);
        orbTextConfig.setTextPosition(OrbTextConfig.OrbTextPosition.ABOVE);
        orbTextConfig.setAboveOrbOffsetY(2.99f);
        orbTextConfig.setAboveOrbOffsetX(4.99f);
        orbTextConfig.setAboveIndividualOrbOffsetY(5.77f);
        orbTextConfig.setAboveIndividualOrbOffsetX(8.96f);
        orbTextConfig.save();

        OrbColorConfig orbColorConfig = OrbColorConfig.getInstance();
        orbColorConfig.resetToDefaults();
        orbColorConfig.save();

        DayCounterConfig dayCounterConfig = DayCounterConfig.getInstance();
        dayCounterConfig.setSoundVolume(5);
        dayCounterConfig.save();

        TrackerConfig trackerConfig = TrackerConfig.getInstance();
        trackerConfig.setEnabled(false);
        trackerConfig.setMaxSkillsShown(20);
        trackerConfig.save();

        HudFontConfig hudFontConfig = HudFontConfig.getInstance();
        hudFontConfig.setFontPreset(HudFontPreset.GOOGLE_SANS);
        hudFontConfig.setUseCustomFont(false);
        hudFontConfig.save();

        SkillBuffFilterConfig skillBuffFilter = SkillBuffFilterConfig.getInstance();
        skillBuffFilter.setShowAura(true);
        skillBuffFilter.setShowSelfSkill(true);
        skillBuffFilter.setShowFood(false);
        skillBuffFilter.setShowCharge(true);
        skillBuffFilter.setShowSong(true);
        skillBuffFilter.setShowGolem(true);
        skillBuffFilter.setShowOther(false);
        skillBuffFilter.save();

        BuffOverlayFilterConfig buffFilter = BuffOverlayFilterConfig.getInstance();
        BuffOverlayFilterConfig.OverlayFilter defaultAll = BuffOverlayFilterConfig.OverlayFilter.createDefaultAll();
        buffFilter.getBuffOverlay().setShowVanillaBuffs(defaultAll.isShowVanillaBuffs());
        buffFilter.getBuffOverlay().setShowVanillaDebuffs(defaultAll.isShowVanillaDebuffs());
        buffFilter.getBuffOverlay().setShowMnsBuffs(defaultAll.isShowMnsBuffs());
        buffFilter.getBuffOverlay().setShowMnsDebuffs(defaultAll.isShowMnsDebuffs());
        buffFilter.getBuffOverlay().setShowMinions(defaultAll.isShowMinions());
        buffFilter.getBuffOverlay().setSortByDuration(defaultAll.isSortByDuration());
        BuffOverlayFilterConfig.OverlayFilter defaultSkill = BuffOverlayFilterConfig.OverlayFilter.createDefaultSkillOnly();
        buffFilter.getSkillBuffOverlay().setShowVanillaBuffs(defaultSkill.isShowVanillaBuffs());
        buffFilter.getSkillBuffOverlay().setShowVanillaDebuffs(defaultSkill.isShowVanillaDebuffs());
        buffFilter.getSkillBuffOverlay().setShowMnsBuffs(defaultSkill.isShowMnsBuffs());
        buffFilter.getSkillBuffOverlay().setShowMnsDebuffs(defaultSkill.isShowMnsDebuffs());
        buffFilter.getSkillBuffOverlay().setShowMinions(defaultSkill.isShowMinions());
        buffFilter.getSkillBuffOverlay().setSortByDuration(defaultSkill.isSortByDuration());
        buffFilter.save();

        DropSoundConfig dropSoundConfig = DropSoundConfig.getInstance();
        dropSoundConfig.resetToDefaults();
        dropSoundConfig.save();

        rebuildCurrentTab();
    }

    private void saveConfig() {
        ExileOverlayConfigManager.getInstance().saveAll();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int leftPanelW = 96;
        int leftPanelX = 16;
        int leftPanelY = 28;
        int leftPanelH = this.height - 36;

        // 左側背景パネル
        guiGraphics.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelW, leftPanelY + leftPanelH, 0x66000000);

        // タイトル
        guiGraphics.drawString(this.font, this.title, leftPanelX + 6, 10, 0xFFFFFF);

        // リスト描画
        if (entryList != null) {
            entryList.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        // ウィジェット（ボタン）描画
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (entryList != null && entryList.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (entryList != null && entryList.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (entryList != null && entryList.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (entryList != null && entryList.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (entryList != null && entryList.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (entryList != null && entryList.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        saveConfig();
        this.minecraft.setScreen(this.lastScreen);
    }
}