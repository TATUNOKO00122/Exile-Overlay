package com.example.exile_overlay.client.config.screen;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.OrbTextConfig;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.damage.DamageFontRenderer;
import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.FontPreset;
import com.example.exile_overlay.client.render.DayCounterConfig;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import com.example.exile_overlay.util.InventorySorterHelper;
import com.example.exile_overlay.util.LootrHelper;
import com.example.exile_overlay.util.MineAndSlashHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private static final String DAY_COUNTER_KEY = "day_counter";
    
    private final Screen lastScreen;
    private int currentTab = 0;

    private double scrollOffset = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;
    private boolean isDraggingScrollbar = false;

    private final List<net.minecraft.client.gui.components.AbstractWidget> leftWidgets = new ArrayList<>();
    private final List<net.minecraft.client.gui.components.AbstractWidget> rightWidgets = new ArrayList<>();
    private final List<SectionHeader> sectionHeaders = new ArrayList<>();

    public ConfigScreen(Screen lastScreen) {
        super(Component.translatable("screen.exile_overlay.config.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        leftWidgets.clear();
        rightWidgets.clear();
        sectionHeaders.clear();
        this.clearWidgets();

        int leftPanelW = 90;
        int leftPanelX = 20;
        int leftPanelY = 30;
        int leftPanelH = this.height - 40;

        int rightPanelX = leftPanelX + leftPanelW + 10;
        int rightPanelY = 30;
        int rightPanelW = this.width - rightPanelX - 20;
        int rightPanelH = this.height - 40;

        int tabY = leftPanelY + 10;
        for (int i = 0; i < 4; i++) {
            final int index = i;
            Button btn = Button.builder(getTabComponent(i), b -> switchTab(index))
                    .bounds(leftPanelX + 10, tabY, leftPanelW - 20, 20)
                    .build();
            btn.active = (currentTab != i);
            leftWidgets.add(btn);
            this.addRenderableWidget(btn);
            tabY += 24;
        }

        Button btnDone = Button.builder(CommonComponents.GUI_DONE, b -> {
            saveConfig();
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(leftPanelX + 10, leftPanelY + leftPanelH - 30, leftPanelW - 20, 20).build();

        Button btnReset = Button.builder(Component.translatable("button.exile_overlay.reset"), b -> resetToDefaults())
                .bounds(leftPanelX + 10, leftPanelY + leftPanelH - 54, leftPanelW - 20, 20)
                .tooltip(Tooltip.create(Component.translatable("button.exile_overlay.reset.tooltip")))
                .build();

        leftWidgets.add(btnDone);
        this.addRenderableWidget(btnDone);
        leftWidgets.add(btnReset);
        this.addRenderableWidget(btnReset);

        int colW = Math.max(100, Math.min(260, rightPanelW - 20));
        int colX = rightPanelX + rightPanelW / 2 - colW / 2;
        int startY = rightPanelY + 10 - (int) scrollOffset;

        buildTabContent(currentTab, colX, startY, colW, 20, 24, rightPanelX + 20);

        updateMaxScroll(rightPanelH);
        clampScrollOffset();
    }

    private Component getTabComponent(int index) {
        return switch (index) {
            case 0 -> Component.translatable("exile_overlay.config.tab.general");
            case 1 -> Component.translatable("exile_overlay.config.tab.damage_popup");
            case 2 -> Component.translatable("exile_overlay.config.tab.hp_bar");
            case 3 -> Component.translatable("exile_overlay.config.tab.equipment_orb");
            default -> Component.empty();
        };
    }

    private void addRightWidget(net.minecraft.client.gui.components.AbstractWidget widget) {
        rightWidgets.add(widget);
        this.addWidget(widget);
    }

    private void buildTabContent(int tab, int colX, int y, int colW, int btnH, int spacing, int titleX) {
        switch (tab) {
            case 0 -> buildGeneralTab(colX, y, colW, btnH, spacing, titleX);
            case 1 -> buildDamagePopupTab(colX, y, colW, btnH, spacing, titleX);
            case 2 -> buildEntityHealthBarTab(colX, y, colW, btnH, spacing, titleX);
            case 3 -> buildEquipmentOrbTab(colX, y, colW, btnH, spacing, titleX);
        }
    }

    private int addSection(int y, String titleKey, int titleX) {
        y += 10;
        sectionHeaders.add(new SectionHeader(titleX, y, Component.translatable(titleKey)));
        return y + 16;
    }

    private Component getOnOffComponent(String key, boolean enabled) {
        return Component.translatable(key,
                Component.translatable(enabled ? "exile_overlay.config.on" : "exile_overlay.config.off"));
    }

    private Component getModeOnlyComponent(EquipmentDisplayConfig.QuickLootMode mode) {
        String modeKey = mode == EquipmentDisplayConfig.QuickLootMode.LOOT 
                ? "exile_overlay.config.mode.loot" 
                : "exile_overlay.config.mode.drop";
        return Component.translatable(modeKey);
    }

    private Component getQuickLootModeComponent(String key, EquipmentDisplayConfig.QuickLootMode mode) {
        String modeKey = mode == EquipmentDisplayConfig.QuickLootMode.LOOT 
                ? "exile_overlay.config.mode.loot" 
                : "exile_overlay.config.mode.drop";
        return Component.translatable(key, Component.translatable(modeKey));
    }

    private void buildGeneralTab(int x, int y, int w, int h, int sp, int tx) {
        y = addSection(y, "section.exile_overlay.hud_position", tx);

        addRightWidget(Button.builder(
                Component.translatable("exile_overlay.config.open_hud_editor"),
                btn -> {
                    Minecraft.getInstance().setScreen(new DraggableHudConfigScreen(this));
                })
                .bounds(x, y, w, h)
                .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.open_hud_editor.tooltip")))
                .build());
        y += sp;

        y = addSection(y, "section.exile_overlay.day_counter", tx);

        HudPosition dayCounterPos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.day_counter_enabled", dayCounterPos.isVisible()), btn -> {
                    HudPosition pos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);
                    HudPositionManager.getInstance().setPosition(DAY_COUNTER_KEY, pos.withVisible(!pos.isVisible()));
                    btn.setMessage(getOnOffComponent("exile_overlay.config.day_counter_enabled", HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY).isVisible()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.day_counter_enabled.tooltip")))
                        .build());
        y += sp;

        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.day_counter_scale",
                dayCounterPos.getScale(), 0.5f, 3.0f, val -> {
                    HudPosition pos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);
                    HudPositionManager.getInstance().setPosition(DAY_COUNTER_KEY, pos.withScale(val));
                }));
        y += sp;

        DayCounterConfig dayCounterConfig = DayCounterConfig.getInstance();
        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.day_counter_volume",
                dayCounterConfig.getSoundVolume(), 0, 100, val -> {
                    dayCounterConfig.setSoundVolume(val);
                    dayCounterConfig.save();
                }));
        y += sp;

        y = buildCompatibilitySection(x, y, w, h, sp, tx);

        contentHeight = y - (30 - (int) scrollOffset) + sp;
    }

    private void buildDamagePopupTab(int x, int y, int w, int h, int sp, int tx) {
        DamagePopupConfig config = DamagePopupConfig.getInstance();
        
        y = addSection(y, "section.exile_overlay.display_settings", tx);
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.show_damage", config.isShowDamage()), btn -> {
                    config.setShowDamage(!config.isShowDamage());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.show_damage", config.isShowDamage()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.show_damage.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.show_healing", config.isShowHealing()), btn -> {
                    config.setShowHealing(!config.isShowHealing());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.show_healing", config.isShowHealing()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.show_healing.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.show_player_damage", config.isShowPlayerDamage()), btn -> {
                    config.setShowPlayerDamage(!config.isShowPlayerDamage());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.show_player_damage", config.isShowPlayerDamage()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.show_player_damage.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.show_player_healing", config.isShowPlayerHealing()), btn -> {
                    config.setShowPlayerHealing(!config.isShowPlayerHealing());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.show_player_healing", config.isShowPlayerHealing()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.show_player_healing.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.enable_shadow", config.isEnableShadow()), btn -> {
                    config.setEnableShadow(!config.isEnableShadow());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.enable_shadow", config.isEnableShadow()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.enable_shadow.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.round_damage_numbers", config.isRoundDamageNumbers()), btn -> {
                    config.setRoundDamageNumbers(!config.isRoundDamageNumbers());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.round_damage_numbers", config.isRoundDamageNumbers()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.round_damage_numbers.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.compact_numbers", config.isCompactNumbers()), btn -> {
                    config.setCompactNumbers(!config.isCompactNumbers());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.compact_numbers", config.isCompactNumbers()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.compact_numbers.tooltip")))
                        .build());
        y += sp;
        
        addRightWidget(Button.builder(getFontPresetComponent(config.getFontPreset()), btn -> {
            FontPreset[] presets = FontPreset.values();
            int currentIndex = config.getFontPreset().ordinal();
            int nextIndex = (currentIndex + 1) % presets.length;
            config.setFontPreset(presets[nextIndex]);
            btn.setMessage(getFontPresetComponent(config.getFontPreset()));
        }).bounds(x, y, w, h)
                .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.font_preset.tooltip")))
                .build());
        y += sp;
        
        y = addSection(y, "section.exile_overlay.numeric_settings", tx);
        
        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.base_scale",
                config.getBaseScale(), 0.01f, 0.3f, val -> config.setBaseScale(val)));
        y += sp;
        
        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.critical_scale",
                config.getCriticalScale(), 0.01f, 0.5f, val -> config.setCriticalScale(val)));
        y += sp;
        
        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.display_duration",
                config.getDisplayDuration(), 20, 200, val -> config.setDisplayDuration(val)));
        y += sp;
        
        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.fade_in",
                config.getFadeInDuration(), 0, 20, val -> config.setFadeInDuration(val)));
        y += sp;
        
        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.fade_out",
                config.getFadeOutDuration(), 5, 60, val -> config.setFadeOutDuration(val)));
        y += sp;
        
        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.max_texts",
                config.getMaxDamageTexts(), 5, 50, val -> config.setMaxDamageTexts(val)));
        y += sp;
        
        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.popup_height",
                config.getPopupHeightRatio(), 0.0f, 1.0f, val -> config.setPopupHeightRatio(val)));
        
        contentHeight = y - (30 - (int) scrollOffset) + sp;
    }

    private void buildEquipmentOrbTab(int x, int y, int w, int h, int sp, int tx) {
        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();

        y = addSection(y, "section.exile_overlay.equipment_hud", tx);

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.use_percentage", equipConfig.isUsePercentage()), btn -> {
                    equipConfig.setUsePercentage(!equipConfig.isUsePercentage());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.use_percentage", equipConfig.isUsePercentage()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.use_percentage.tooltip")))
                        .build());
        y += sp;

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.enable_shadow", equipConfig.isEnableShadow()), btn -> {
                    equipConfig.setEnableShadow(!equipConfig.isEnableShadow());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.enable_shadow", equipConfig.isEnableShadow()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.enable_shadow.tooltip")))
                        .build());
        y += sp;

        y = addSection(y, "section.exile_overlay.orb_text", tx);

        OrbTextConfig orbConfig = OrbTextConfig.getInstance();

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.show_orb_text", orbConfig.isShowOrbText()), btn -> {
                    orbConfig.setShowOrbText(!orbConfig.isShowOrbText());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.show_orb_text", orbConfig.isShowOrbText()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.show_orb_text.tooltip")))
                        .build());
        y += sp;

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.compact_numbers_orb", orbConfig.isCompactNumbers()), btn -> {
                    orbConfig.setCompactNumbers(!orbConfig.isCompactNumbers());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.compact_numbers_orb", orbConfig.isCompactNumbers()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.compact_numbers_orb.tooltip")))
                        .build());
        y += sp;

        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.text_scale",
                orbConfig.getTextScale(), 0.5f, 2.0f, "%.0f%%", orbConfig::setTextScale));
        y += sp;

        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.energy_text_scale",
                orbConfig.getEnergyTextScale(), 0.5f, 2.0f, "%.0f%%", orbConfig::setEnergyTextScale));
        y += sp;

        contentHeight = y - (30 - (int) scrollOffset) + sp;
    }

    private int buildCompatibilitySection(int x, int y, int w, int h, int sp, int tx) {
        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        boolean hasAnyCompat = false;

        if (LootrHelper.isLoaded()) {
            hasAnyCompat = true;
            y = addSection(y, "section.exile_overlay.quick_loot", tx);

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.quick_loot_enabled", config.isQuickLootEnabled()), btn -> {
                        config.setQuickLootEnabled(!config.isQuickLootEnabled());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.quick_loot_enabled", config.isQuickLootEnabled()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.quick_loot_enabled.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.auto_execute", config.isAutoQuickLootEnabled()), btn -> {
                        config.setAutoQuickLootEnabled(!config.isAutoQuickLootEnabled());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.auto_execute", config.isAutoQuickLootEnabled()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.auto_execute.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getModeOnlyComponent(config.getAutoQuickLootMode()), btn -> {
                        EquipmentDisplayConfig.QuickLootMode next = config.getAutoQuickLootMode() == EquipmentDisplayConfig.QuickLootMode.LOOT
                                ? EquipmentDisplayConfig.QuickLootMode.DROP
                                : EquipmentDisplayConfig.QuickLootMode.LOOT;
                        config.setAutoQuickLootMode(next);
                        btn.setMessage(getModeOnlyComponent(next));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.auto_execute_mode.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.key_execute", config.isKeyQuickLootEnabled()), btn -> {
                        config.setKeyQuickLootEnabled(!config.isKeyQuickLootEnabled());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.key_execute", config.isKeyQuickLootEnabled()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.key_execute.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getModeOnlyComponent(config.getKeyQuickLootMode()), btn -> {
                        EquipmentDisplayConfig.QuickLootMode next = config.getKeyQuickLootMode() == EquipmentDisplayConfig.QuickLootMode.LOOT
                                ? EquipmentDisplayConfig.QuickLootMode.DROP
                                : EquipmentDisplayConfig.QuickLootMode.LOOT;
                        config.setKeyQuickLootMode(next);
                        btn.setMessage(getModeOnlyComponent(next));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.key_execute_mode.tooltip")))
                            .build());
            y += sp;
        }

        if (MineAndSlashHelper.isLoaded()) {
            hasAnyCompat = true;
            y = addSection(y, "section.exile_overlay.mns_compat", tx);

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.cancel_mns_rpg_bars", config.isCancelMnsRpgBars()), btn -> {
                        config.setCancelMnsRpgBars(!config.isCancelMnsRpgBars());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.cancel_mns_rpg_bars", config.isCancelMnsRpgBars()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.cancel_mns_rpg_bars.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.cancel_mns_spell_hotbar", config.isCancelMnsSpellHotbar()), btn -> {
                        config.setCancelMnsSpellHotbar(!config.isCancelMnsSpellHotbar());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.cancel_mns_spell_hotbar", config.isCancelMnsSpellHotbar()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.cancel_mns_spell_hotbar.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.cancel_mns_cast_bar", config.isCancelMnsCastBar()), btn -> {
                        config.setCancelMnsCastBar(!config.isCancelMnsCastBar());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.cancel_mns_cast_bar", config.isCancelMnsCastBar()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.cancel_mns_cast_bar.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.cancel_mns_status_effects", config.isCancelMnsStatusEffects()), btn -> {
                        config.setCancelMnsStatusEffects(!config.isCancelMnsStatusEffects());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.cancel_mns_status_effects", config.isCancelMnsStatusEffects()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.cancel_mns_status_effects.tooltip")))
                            .build());
            y += sp;

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.disable_mns_hpbar", config.isDisableMnsHpBar()), btn -> {
                        config.setDisableMnsHpBar(!config.isDisableMnsHpBar());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.disable_mns_hpbar", config.isDisableMnsHpBar()));
                        MineAndSlashHelper.setNeatHpBarEnabled(!config.isDisableMnsHpBar());
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.disable_mns_hpbar.tooltip")))
                            .build());
            y += sp;
        }

        if (com.example.exile_overlay.api.DungeonRealmReflection.isAvailable()) {
            hasAnyCompat = true;
            y = addSection(y, "section.exile_overlay.dungeon_realm_compat", tx);

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.cancel_dungeon_scoreboard", config.isCancelDungeonRealmScoreboard()), btn -> {
                        config.setCancelDungeonRealmScoreboard(!config.isCancelDungeonRealmScoreboard());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.cancel_dungeon_scoreboard", config.isCancelDungeonRealmScoreboard()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.cancel_dungeon_scoreboard.tooltip")))
                            .build());
            y += sp;
        }

        if (LootrHelper.isLoaded() && InventorySorterHelper.isLoaded()) {
            hasAnyCompat = true;
            y = addSection(y, "section.exile_overlay.inventory_sorter", tx);

            addRightWidget(
                    Button.builder(getOnOffComponent("exile_overlay.config.auto_sort_lootr_chest", config.isAutoSortLootrChest()), btn -> {
                        config.setAutoSortLootrChest(!config.isAutoSortLootrChest());
                        btn.setMessage(getOnOffComponent("exile_overlay.config.auto_sort_lootr_chest", config.isAutoSortLootrChest()));
                    }).bounds(x, y, w, h)
                            .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.auto_sort_lootr_chest.tooltip")))
                            .build());
            y += sp;
        }

        return y;
    }

    private void buildEntityHealthBarTab(int x, int y, int w, int h, int sp, int tx) {
        EntityHealthBarConfig config = EntityHealthBarConfig.getInstance();

        y = addSection(y, "section.exile_overlay.display_settings", tx);

        addRightWidget(
                Button.builder(getOnOffComponent("exile_overlay.config.entity_hp_bar_enabled", config.isEnabled()), btn -> {
                    config.setEnabled(!config.isEnabled());
                    btn.setMessage(getOnOffComponent("exile_overlay.config.entity_hp_bar_enabled", config.isEnabled()));
                }).bounds(x, y, w, h)
                        .tooltip(Tooltip.create(Component.translatable("exile_overlay.config.entity_hp_bar_enabled.tooltip")))
                        .build());
        y += sp;

        y = addSection(y, "section.exile_overlay.numeric_settings", tx);

        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.max_distance",
                config.getMaxDistance(), 8, 64, val -> config.setMaxDistance(val)));
        y += sp;

        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.height_above",
                (float) config.getHeightAbove(), -1.0f, 3.0f, val -> config.setHeightAbove(val)));
        y += sp;

        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.bar_width",
                config.getBarWidth(), 10, 60, val -> config.setBarWidth(val)));
        y += sp;

        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.bar_height",
                config.getBarHeight(), 1, 8, val -> config.setBarHeight(val)));
        y += sp;

        addRightWidget(new FloatConfigSlider(x, y, w, h, "exile_overlay.config.bar_scale",
                config.getScale(), 0.5f, 3.0f, val -> config.setScale(val)));
        y += sp;

        addRightWidget(new IntConfigSlider(x, y, w, h, "exile_overlay.config.hp_bar_display_duration",
                config.getDisplayDuration(), 1, 30, val -> config.setDisplayDuration(val)));
        y += sp;

        y = addSection(y, "section.exile_overlay.color_settings", tx);

        addRightWidget(new ColorPresetButton(x, y, w, h, config));

        contentHeight = y - (30 - (int) scrollOffset) + sp;
    }

    private Component getFontPresetComponent(FontPreset preset) {
        return Component.translatable("exile_overlay.config.font_preset", preset.getDisplayName());
    }

    // TODO: フォントプリセット選択再有効化時に使用
    // private Component getHudFontPresetComponent(HudFontPreset preset) {
    //     return Component.translatable("exile_overlay.config.hud_font_preset", preset.getDisplayName());
    // }

    private record SectionHeader(int x, int y, Component label) {
    }

    private void updateMaxScroll(int visibleHeight) {
        maxScroll = Math.max(0, contentHeight - visibleHeight);
    }

    private void clampScrollOffset() {
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void switchTab(int tab) {
        this.currentTab = tab;
        this.scrollOffset = 0;
        this.init();
    }

    private void saveConfig() {
        EquipmentDisplayConfig.getInstance().save();
        DamagePopupConfig.getInstance().save();
        EntityHealthBarConfig.getInstance().save();
        OrbTextConfig.getInstance().save();
        HudPositionManager.getInstance().saveToFile();
        DamageFontRenderer.reloadCustomFont();
    }

    private void resetToDefaults() {
        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        config.setUsePercentage(false);
        config.setEnableShadow(true);
        config.setQuickLootEnabled(true);
        config.setAutoQuickLootEnabled(false);
        config.setAutoQuickLootMode(EquipmentDisplayConfig.QuickLootMode.LOOT);
        config.setKeyQuickLootEnabled(true);
        config.setKeyQuickLootMode(EquipmentDisplayConfig.QuickLootMode.DROP);
        config.setDisableMnsHpBar(true);
        config.setCancelMnsRpgBars(true);
        config.setCancelMnsSpellHotbar(true);
        config.setCancelMnsCastBar(true);
        config.setCancelMnsStatusEffects(true);
        config.setCancelDungeonRealmScoreboard(true);
        config.setAutoSortLootrChest(true);
        config.save();
        
        MineAndSlashHelper.setNeatHpBarEnabled(false);
        
        DamagePopupConfig damageConfig = DamagePopupConfig.getInstance();
        damageConfig.setShowDamage(true);
        damageConfig.setShowHealing(true);
        damageConfig.setShowPlayerDamage(false);
        damageConfig.setShowPlayerHealing(true);
        damageConfig.setEnableShadow(true);
        damageConfig.setFontPreset(FontPreset.LINESEED);
        damageConfig.setBaseScale(0.03f);
        damageConfig.setCriticalScale(0.04f);
        damageConfig.setDisplayDuration(30);
        damageConfig.setFadeInDuration(5);
        damageConfig.setFadeOutDuration(10);
        damageConfig.setMaxDamageTexts(20);
        damageConfig.setPopupHeightRatio(0.8f);
        damageConfig.save();
        
        HudPosition dayCounterPos = HudPositionManager.getInstance().getPosition(DAY_COUNTER_KEY);
        HudPositionManager.getInstance().setPosition(DAY_COUNTER_KEY, 
            dayCounterPos.withScale(1.0f).withVisible(true));
        HudPositionManager.getInstance().saveToFile();

        EntityHealthBarConfig hpBarConfig = EntityHealthBarConfig.getInstance();
        hpBarConfig.setEnabled(true);
        hpBarConfig.setMaxDistance(24);
        hpBarConfig.setHeightAbove(0.5);
        hpBarConfig.setBarWidth(30);
        hpBarConfig.setBarHeight(2);
        hpBarConfig.setScale(1.0f);
        hpBarConfig.save();

        OrbTextConfig orbTextConfig = OrbTextConfig.getInstance();
        orbTextConfig.setShowOrbText(true);
        orbTextConfig.setCompactNumbers(false);
        orbTextConfig.setTextScale(1.0f);
        orbTextConfig.setEnergyTextScale(1.0f);
        orbTextConfig.save();

        DamageFontRenderer.reloadCustomFont();
        this.init();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (maxScroll > 0) {
            scrollOffset -= delta * 20;
            clampScrollOffset();
            this.init();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0) {
            int leftPanelW = 90;
            int rightPanelX = 20 + leftPanelW + 10;
            int rightPanelW = this.width - rightPanelX - 20;
            int rightPanelH = this.height - 40;
            int scrollBarX = rightPanelX + rightPanelW - 6;

            if (mouseX >= scrollBarX - 4 && mouseX <= scrollBarX + 8 && mouseY >= 30 && mouseY <= 30 + rightPanelH) {
                isDraggingScrollbar = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && isDraggingScrollbar) {
            isDraggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScrollbar && maxScroll > 0) {
            int rightPanelH = this.height - 40;
            int visibleHeight = rightPanelH;
            int scrollBarHeight = Math.max(20, (int) ((double) visibleHeight / (double) contentHeight * visibleHeight));
            double scrollFactor = (double) maxScroll / (visibleHeight - scrollBarHeight);
            scrollOffset += dragY * scrollFactor;
            clampScrollOffset();
            this.init();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int leftPanelW = 90;
        int leftPanelX = 20;
        int leftPanelY = 30;
        int leftPanelH = this.height - 40;

        int rightPanelX = leftPanelX + leftPanelW + 10;
        int rightPanelY = 30;
        int rightPanelW = this.width - rightPanelX - 20;
        int rightPanelH = this.height - 40;

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        guiGraphics.fill(leftPanelX, leftPanelY, leftPanelX + leftPanelW, leftPanelY + leftPanelH, 0x66000000);
        guiGraphics.fill(rightPanelX, rightPanelY, rightPanelX + rightPanelW, rightPanelY + rightPanelH, 0x44000000);

        guiGraphics.enableScissor(rightPanelX, rightPanelY, rightPanelX + rightPanelW, rightPanelY + rightPanelH);

        for (var header : sectionHeaders) {
            guiGraphics.drawString(this.font, header.label(), header.x(), header.y(), 0xFFFFAA);
            guiGraphics.fill(header.x(), header.y() + 11, rightPanelX + rightPanelW - 20, header.y() + 12, 0x22FFFFFF);
        }

        for (var widget : rightWidgets) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        guiGraphics.disableScissor();

        renderScrollBar(guiGraphics, rightPanelX + rightPanelW - 6, rightPanelY, rightPanelH);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderScrollBar(GuiGraphics guiGraphics, int x, int y, int height) {
        if (maxScroll <= 0)
            return;

        int visibleHeight = height;
        int scrollBarHeight = Math.max(20, (int) ((double) visibleHeight / contentHeight * visibleHeight));
        int scrollBarY = y + (int) ((double) scrollOffset / maxScroll * (visibleHeight - scrollBarHeight));

        guiGraphics.fill(x, y, x + 6, y + height, 0x88000000);
        guiGraphics.fill(x, scrollBarY, x + 6, scrollBarY + scrollBarHeight,
                isDraggingScrollbar ? 0xFFFFFFFF : 0xFFAAAAAA);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    private static class FloatConfigSlider extends AbstractSliderButton {
        private final String translationKey;
        private final float min;
        private final float max;
        private final String format;
        private final java.util.function.Consumer<Float> setter;

        public FloatConfigSlider(int x, int y, int width, int height, String translationKey, float current, float min,
                float max, java.util.function.Consumer<Float> setter) {
            this(x, y, width, height, translationKey, current, min, max, "%.2f", setter);
        }

        public FloatConfigSlider(int x, int y, int width, int height, String translationKey, float current, float min,
                float max, String format, java.util.function.Consumer<Float> setter) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.translationKey = translationKey;
            this.min = min;
            this.max = max;
            this.format = format;
            this.setter = setter;
            this.setTooltip(Tooltip.create(Component.translatable(translationKey + ".tooltip")));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            float value = min + (max - min) * (float) this.value;
            this.setMessage(Component.translatable(translationKey, String.format(format, value)));
        }

        @Override
        protected void applyValue() {
            float newValue = min + (max - min) * (float) this.value;
            newValue = Math.round(newValue * 100.0f) / 100.0f;
            setter.accept(newValue);
        }
    }

    private static class IntConfigSlider extends AbstractSliderButton {
        private final String translationKey;
        private final int min;
        private final int max;
        private final java.util.function.Consumer<Integer> setter;

        public IntConfigSlider(int x, int y, int width, int height, String translationKey, int current, int min,
                int max, java.util.function.Consumer<Integer> setter) {
            super(x, y, width, height, Component.empty(), (double) (current - min) / (max - min));
            this.translationKey = translationKey;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.setTooltip(Tooltip.create(Component.translatable(translationKey + ".tooltip")));
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            int value = min + (int) Math.round((max - min) * this.value);
            this.setMessage(Component.translatable(translationKey, value));
        }

        @Override
        protected void applyValue() {
            int newValue = min + (int) Math.round((max - min) * this.value);
            setter.accept(newValue);
        }
    }

    private class ColorPresetButton extends Button {
        private final EntityHealthBarConfig config;

        ColorPresetButton(int x, int y, int width, int height, EntityHealthBarConfig config) {
            super(createBuilder(config).bounds(x, y, width, height));
            this.config = config;
        }

        private static Button.Builder createBuilder(EntityHealthBarConfig config) {
            return Button.builder(Component.empty(), btn -> {
                EntityHealthBarConfig.ColorPreset current = EntityHealthBarConfig.ColorPreset.fromHex(config.getHealthBarColor());
                EntityHealthBarConfig.ColorPreset[] presets = EntityHealthBarConfig.ColorPreset.values();
                int nextIndex = (current.ordinal() + 1) % presets.length;
                config.setHealthBarColor(presets[nextIndex].getHex());
            }).tooltip(Tooltip.create(Component.translatable("exile_overlay.config.hp_color.tooltip")));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);

            EntityHealthBarConfig.ColorPreset preset = EntityHealthBarConfig.ColorPreset.fromHex(config.getHealthBarColor());
            int color = preset.getColorValue();

            int boxSize = 12;
            int boxX = this.getX() + (this.getWidth() - boxSize) / 2;
            int boxY = this.getY() + (this.getHeight() - boxSize) / 2;

            guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, 0xFF000000);
            guiGraphics.fill(boxX + 1, boxY + 1, boxX + boxSize - 1, boxY + boxSize - 1, color);
        }
    }
}