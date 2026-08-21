package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.entry.BooleanConfigEntry;
// import com.example.exile_overlay.client.config.screen.entry.ColorPresetConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.CycleConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.FloatSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.IntSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.SectionHeaderEntry;
import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.damage.DamagePopupMode;
import com.example.exile_overlay.client.damage.FontPreset;
// import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 「ダメージポップアップ (Damage Popup)」タブ。
 * ダメージポップアップの設定を管理する。
 */
public class DamagePopupTab implements IConfigTab {

    @Override
    public Component getTitle() {
        return Component.translatable("exile_overlay.config.tab.damage_popup");
    }

    @Override
    public List<ConfigEntry> buildEntries(ConfigScreen screen) {
        List<ConfigEntry> entries = new ArrayList<>();
        DamagePopupConfig popupConfig = DamagePopupConfig.getInstance();
        // EntityHealthBarConfig hpConfig = EntityHealthBarConfig.getInstance();

        // 1. ダメージポップアップ表示設定
        entries.add(new SectionHeaderEntry("section.exile_overlay.damage_popup"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_damage",
                popupConfig::isShowDamage,
                popupConfig::setShowDamage
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_healing",
                popupConfig::isShowHealing,
                popupConfig::setShowHealing
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_player_damage",
                popupConfig::isShowPlayerDamage,
                popupConfig::setShowPlayerDamage
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_player_healing",
                popupConfig::isShowPlayerHealing,
                popupConfig::setShowPlayerHealing
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.enable_shadow",
                popupConfig::isEnableShadow,
                popupConfig::setEnableShadow
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.round_damage_numbers",
                popupConfig::isRoundDamageNumbers,
                popupConfig::setRoundDamageNumbers
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.compact_numbers",
                popupConfig::isCompactNumbers,
                popupConfig::setCompactNumbers
        ));

        entries.add(new CycleConfigEntry<>(
                Arrays.asList(FontPreset.values()),
                popupConfig::getFontPreset,
                popupConfig::setFontPreset,
                preset -> Component.translatable("exile_overlay.config.font_preset", preset.getDisplayName()),
                Component.translatable("exile_overlay.config.font_preset.tooltip")
        ));

        entries.add(new CycleConfigEntry<>(
                Arrays.asList(DamagePopupMode.values()),
                popupConfig::getPopupMode,
                popupConfig::setPopupMode,
                mode -> Component.translatable("exile_overlay.config.popup_mode", mode.getDisplayName()),
                Component.translatable("exile_overlay.config.popup_mode.tooltip")
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.base_scale",
                () -> Math.round(popupConfig.getBaseScale() / 0.00036f),
                val -> popupConfig.setBaseScale(val * 0.00036f),
                0, 100
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.critical_scale",
                () -> Math.round(popupConfig.getCriticalScale() / 0.00064f),
                val -> popupConfig.setCriticalScale(val * 0.00064f),
                0, 100
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.display_duration",
                popupConfig::getDisplayDuration,
                popupConfig::setDisplayDuration,
                0, 40
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.fade_in",
                popupConfig::getFadeInDuration,
                popupConfig::setFadeInDuration,
                0, 10
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.fade_out",
                popupConfig::getFadeOutDuration,
                popupConfig::setFadeOutDuration,
                0, 20
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.max_texts",
                popupConfig::getMaxDamageTexts,
                popupConfig::setMaxDamageTexts,
                0, 100,
                val -> val == 0 ? Component.translatable("exile_overlay.config.max_texts.unlimited") : Component.translatable("exile_overlay.config.max_texts", val)
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.popup_height",
                popupConfig::getPopupHeightRatio,
                popupConfig::setPopupHeightRatio,
                0.0f, 1.6f
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.enable_damage_scale",
                popupConfig::isEnableDamageScale,
                popupConfig::setEnableDamageScale
        ));

        /* 3D HPBar無効化中のためコメントアウト
        // 2. エンティティHPバー設定
        entries.add(new SectionHeaderEntry("section.exile_overlay.entity_hp_bar"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.entity_hp_bar_enabled",
                hpConfig::isEnabled,
                hpConfig::setEnabled
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.hp_bar_show_poison",
                hpConfig::isShowPoison,
                hpConfig::setShowPoison
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.hp_bar_show_bleed",
                hpConfig::isShowBleed,
                hpConfig::setShowBleed
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.hp_bar_show_friendly",
                hpConfig::isShowFriendlyColor,
                hpConfig::setShowFriendlyColor
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.max_distance",
                hpConfig::getMaxDistance,
                hpConfig::setMaxDistance,
                8, 64
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.height_above",
                () -> (float) hpConfig.getHeightAbove(),
                val -> hpConfig.setHeightAbove(val.doubleValue()),
                -1.0f, 3.0f
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.bar_width",
                hpConfig::getBarWidth,
                hpConfig::setBarWidth,
                10, 60
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.bar_height",
                hpConfig::getBarHeight,
                hpConfig::setBarHeight,
                1, 8
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.bar_scale",
                hpConfig::getScale,
                hpConfig::setScale,
                0.5f, 3.0f
        ));

        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.hp_bar_display_duration",
                hpConfig::getDisplayDuration,
                hpConfig::setDisplayDuration,
                1, 30
        ));

        entries.add(new ColorPresetConfigEntry(hpConfig));
        */

        return entries;
    }
}
