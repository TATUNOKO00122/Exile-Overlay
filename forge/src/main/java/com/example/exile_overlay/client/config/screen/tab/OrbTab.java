package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.OrbSmoothConfig;
import com.example.exile_overlay.client.config.OrbTextConfig;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.OrbColorConfigScreen;
import com.example.exile_overlay.client.config.screen.entry.ActionConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.BooleanConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.CycleConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.FloatSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.SectionHeaderEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 「オーブ (Orbs)」タブ。
 * オーブの色、テキスト位置、サイズ、オフセット、MS/入れ替えモード、ノイズ、スムージング等を管理する。
 */
public class OrbTab implements IConfigTab {

    @Override
    public Component getTitle() {
        return Component.translatable("exile_overlay.config.tab.orb");
    }

    @Override
    public List<ConfigEntry> buildEntries(ConfigScreen screen) {
        List<ConfigEntry> entries = new ArrayList<>();
        OrbTextConfig orbConfig = OrbTextConfig.getInstance();
        OrbSmoothConfig smoothConfig = OrbSmoothConfig.getInstance();
        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();

        // 1. オーブ色設定
        entries.add(new SectionHeaderEntry("section.exile_overlay.orb_color"));
        entries.add(new ActionConfigEntry(
                Component.translatableWithFallback("exile_overlay.config.open_orb_color_editor", "オーブカラーの変更"),
                Component.translatableWithFallback("exile_overlay.config.open_orb_color_editor.tooltip", "実際のHUDを見ながらオーブの色を調整します"),
                btn -> Minecraft.getInstance().setScreen(new OrbColorConfigScreen(screen))
        ));

        // 2. オーブテキスト基本設定
        entries.add(new SectionHeaderEntry("section.exile_overlay.orb_text"));
        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_orb_text",
                orbConfig::isShowOrbText,
                orbConfig::setShowOrbText
        ));

        entries.add(new CycleConfigEntry<>(
                Arrays.asList(OrbTextConfig.OrbTextPosition.values()),
                orbConfig::getTextPosition,
                orbConfig::setTextPosition,
                pos -> {
                    String key = switch (pos) {
                        case CENTER -> "exile_overlay.config.orb_text_position.center";
                        case ABOVE -> "exile_overlay.config.orb_text_position.above";
                        case ABOVE_INTEGRATED -> "exile_overlay.config.orb_text_position.above_integrated";
                    };
                    return Component.translatable("exile_overlay.config.orb_text_position", Component.translatable(key));
                },
                Component.translatable("exile_overlay.config.orb_text_position.tooltip"),
                screen::rebuildCurrentTab
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.compact_numbers",
                orbConfig::isCompactNumbers,
                orbConfig::setCompactNumbers
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.energy_compact",
                orbConfig::isEnergyCompact,
                orbConfig::setEnergyCompact
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.orb_text_shadow",
                orbConfig::isOrbTextShadow,
                orbConfig::setOrbTextShadow
        ));

        // 3. テキストサイズ・位置調整スライダー
        boolean aboveMode = orbConfig.getTextPosition() != OrbTextConfig.OrbTextPosition.CENTER;
        if (aboveMode) {
            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.above_text_scale",
                    orbConfig::getAboveTextScale,
                    orbConfig::setAboveTextScale,
                    0.5f, 4.0f
            ));

            if (orbConfig.getTextPosition() == OrbTextConfig.OrbTextPosition.ABOVE) {
                entries.add(new FloatSliderConfigEntry(
                        "exile_overlay.config.energy_text_scale",
                        orbConfig::getEnergyTextScale,
                        orbConfig::setEnergyTextScale,
                        0.5f, 4.0f
                ));
            }

            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.orb_text_above_offset_x",
                    orbConfig::getAboveOrbOffsetX,
                    orbConfig::setAboveOrbOffsetX,
                    -250.0f, 250.0f,
                    "%.1f"
            ));

            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.orb_text_above_offset",
                    orbConfig::getAboveOrbOffsetY,
                    orbConfig::setAboveOrbOffsetY,
                    -150.0f, 150.0f,
                    "%.1f"
            ));
        } else {
            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.text_scale",
                    orbConfig::getTextScale,
                    orbConfig::setTextScale,
                    0.5f, 4.0f
            ));

            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.energy_text_scale",
                    orbConfig::getEnergyTextScale,
                    orbConfig::setEnergyTextScale,
                    0.5f, 4.0f
            ));

            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.ms_text_scale",
                    orbConfig::getMsTextScale,
                    orbConfig::setMsTextScale,
                    0.5f, 4.0f
            ));
        }

        // 4. オーブ詳細・ゲージ設定
        entries.add(new CycleConfigEntry<>(
                Arrays.asList(OrbTextConfig.OrbResourceSwapMode.values()),
                orbConfig::getOrbSwapMode,
                orbConfig::setOrbSwapMode,
                mode -> {
                    String key = switch (mode) {
                        case OFF -> "exile_overlay.config.orb_swap_mode.off";
                        case SWAPPED -> "exile_overlay.config.orb_swap_mode.swapped";
                        case AUTO -> "exile_overlay.config.orb_swap_mode.auto";
                        case SKILL_COST -> "exile_overlay.config.orb_swap_mode.skill_cost";
                    };
                    return Component.translatable("exile_overlay.config.orb_swap_mode", Component.translatable(key));
                },
                Component.translatable("exile_overlay.config.orb_swap_mode.tooltip")
        ));

        entries.add(new CycleConfigEntry<>(
                Arrays.asList(OrbTextConfig.Orb1MsMode.values()),
                orbConfig::getOrb1MsMode,
                orbConfig::setOrb1MsMode,
                mode -> {
                    String key = switch (mode) {
                        case SPLIT -> "exile_overlay.config.orb1_ms_mode.split";
                        case OVERLAP -> "exile_overlay.config.orb1_ms_mode.overlap";
                    };
                    return Component.translatable("exile_overlay.config.orb1_ms_mode", Component.translatable(key));
                },
                Component.translatable("exile_overlay.config.orb1_ms_mode.tooltip")
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.hide_orb1_smaller_value",
                orbConfig::isHideOrb1SmallerValue,
                orbConfig::setHideOrb1SmallerValue
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.hide_lower_hp_ms_gauge_orb1",
                orbConfig::isHideLowerHpMsGaugeOrb1,
                orbConfig::setHideLowerHpMsGaugeOrb1
        ));

        // 5. ビジュアルエフェクト
        entries.add(new SectionHeaderEntry("section.exile_overlay.orb_visual"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.enable_orb_noise",
                equipConfig::isEnableOrbNoise,
                equipConfig::setEnableOrbNoise
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.enable_liquid_shadow",
                equipConfig::isEnableLiquidShadow,
                equipConfig::setEnableLiquidShadow
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.enable_orb_inner_shadow",
                equipConfig::isEnableOrbInnerShadow,
                equipConfig::setEnableOrbInnerShadow
        ));

        // 6. スムージング設定
        entries.add(new SectionHeaderEntry("section.exile_overlay.orb_smooth"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.smooth_increase",
                smoothConfig::isSmoothIncrease,
                smoothConfig::setSmoothIncrease
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.increase_speed",
                smoothConfig::getIncreaseSpeed,
                smoothConfig::setIncreaseSpeed,
                0.1f, 10.0f
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.smooth_decrease",
                smoothConfig::isSmoothDecrease,
                smoothConfig::setSmoothDecrease
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.decrease_speed",
                smoothConfig::getDecreaseSpeed,
                smoothConfig::setDecreaseSpeed,
                0.1f, 10.0f
        ));

        return entries;
    }
}
