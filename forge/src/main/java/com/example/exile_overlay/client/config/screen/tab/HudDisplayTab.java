package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.BuffOverlayFilterConfig;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import com.example.exile_overlay.client.config.screen.entry.BooleanConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.CycleConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.SectionHeaderEntry;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 「HUD表示 (HUD)」タブ。
 * ターゲット情報、装備耐久度HUD、スキルホットバー、レベル表示モード等を管理する。
 */
public class HudDisplayTab implements IConfigTab {

    @Override
    public Component getTitle() {
        return Component.translatable("exile_overlay.config.tab.hud");
    }

    @Override
    public List<ConfigEntry> buildEntries(ConfigScreen screen) {
        List<ConfigEntry> entries = new ArrayList<>();
        EquipmentDisplayConfig equipConfig = EquipmentDisplayConfig.getInstance();

        // 1. ターゲット情報（M&S利用可能時）
        if (MethodHandlesUtil.isAvailable()) {
            entries.add(new SectionHeaderEntry("section.exile_overlay.target_info"));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.show_target_affix_stats",
                    equipConfig::isShowTargetAffixStats,
                    equipConfig::setShowTargetAffixStats
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.show_target_mob_effects",
                    equipConfig::isShowTargetMobEffects,
                    equipConfig::setShowTargetMobEffects
            ));

            EntityHealthBarConfig hpBarConfig = EntityHealthBarConfig.getInstance();
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.hp_bar_show_poison",
                    hpBarConfig::isShowPoison,
                    hpBarConfig::setShowPoison,
                    Component.translatable("exile_overlay.config.hp_bar_show_poison.tooltip")
                            .append("\n")
                            .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.hp_bar_show_bleed",
                    hpBarConfig::isShowBleed,
                    hpBarConfig::setShowBleed,
                    Component.translatable("exile_overlay.config.hp_bar_show_bleed.tooltip")
                            .append("\n")
                            .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.hp_bar_show_friendly",
                    hpBarConfig::isShowFriendlyColor,
                    hpBarConfig::setShowFriendlyColor
            ));
        }

        // 2. 装備HUD
        entries.add(new SectionHeaderEntry("section.exile_overlay.equipment_hud"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.use_percentage",
                equipConfig::isUsePercentage,
                equipConfig::setUsePercentage
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.equipment_shadow",
                equipConfig::isEnableShadow,
                equipConfig::setEnableShadow
        ));

        // 3. スキルホットバー
        entries.add(new SectionHeaderEntry("section.exile_overlay.skill_hotbar"));

        if (MethodHandlesUtil.isAvailable()) {
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.hotbar_swapping",
                    MethodHandlesUtil::isHotbarSwappingEnabled,
                    MethodHandlesUtil::setHotbarSwappingEnabled
            ));
        }

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.simple_skill_keybind",
                equipConfig::isSimpleSkillKeybindDisplay,
                equipConfig::setSimpleSkillKeybindDisplay
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.simple_skill_charge_summon",
                equipConfig::isSimpleSkillChargeSummonDisplay,
                equipConfig::setSimpleSkillChargeSummonDisplay,
                null,
                Component.translatable("exile_overlay.config.simple_skill_charge_summon.tooltip"),
                screen::rebuildCurrentTab
        ));

        if (equipConfig.isSimpleSkillChargeSummonDisplay()) {
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.simple_skill_charge_max_display",
                    equipConfig::isSimpleSkillChargeMaxDisplay,
                    equipConfig::setSimpleSkillChargeMaxDisplay
            ));
        }

        entries.add(new CycleConfigEntry<>(
                Arrays.asList(EquipmentDisplayConfig.CooldownDisplayType.values()),
                equipConfig::getCooldownDisplayType,
                equipConfig::setCooldownDisplayType,
                type -> {
                    String typeKey = switch (type) {
                        case RADIAL -> "exile_overlay.config.cooldown_display_type.radial";
                        case VERTICAL -> "exile_overlay.config.cooldown_display_type.vertical";
                    };
                    return Component.translatable("exile_overlay.config.cooldown_display_type", Component.translatable(typeKey));
                },
                Component.translatable("exile_overlay.config.cooldown_display_type.tooltip")
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_skill_cooldown_number",
                equipConfig::isShowSkillCooldownNumber,
                equipConfig::setShowSkillCooldownNumber
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_empty_skill_slots",
                equipConfig::isShowEmptySkillSlots,
                equipConfig::setShowEmptySkillSlots
        ));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.show_skill_summon_count",
                equipConfig::isShowSkillSummonCount,
                equipConfig::setShowSkillSummonCount
        ));

        // 4. レベル表示
        entries.add(new SectionHeaderEntry("section.exile_overlay.level_display"));

        List<EquipmentDisplayConfig.LevelDisplayMode> availableModes = MethodHandlesUtil.isAvailable()
                ? Arrays.asList(EquipmentDisplayConfig.LevelDisplayMode.values())
                : List.of(EquipmentDisplayConfig.LevelDisplayMode.BOTH, EquipmentDisplayConfig.LevelDisplayMode.VANILLA_ONLY);

        entries.add(new CycleConfigEntry<>(
                availableModes,
                equipConfig::getLevelDisplayMode,
                equipConfig::setLevelDisplayMode,
                mode -> {
                    String modeKey = switch (mode) {
                        case BOTH -> "exile_overlay.config.level_display.both";
                        case MS_ONLY -> "exile_overlay.config.level_display.ms_only";
                        case VANILLA_ONLY -> "exile_overlay.config.level_display.vanilla_only";
                    };
                    return Component.translatable("exile_overlay.config.level_display_mode", Component.translatable(modeKey));
                },
                Component.translatable("exile_overlay.config.level_display_mode.tooltip")
        ));

        // 5. バフオーバーレイフィルター
        entries.add(new SectionHeaderEntry("section.exile_overlay.buff_overlay_filters"));

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.simple_buff_stack",
                equipConfig::isSimpleBuffStackDisplay,
                equipConfig::setSimpleBuffStackDisplay
        ));

        BuffOverlayFilterConfig.OverlayFilter buffFilter = BuffOverlayFilterConfig.getInstance().getBuffOverlay();

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.buff_filter_sort_by_duration",
                buffFilter::isSortByDuration,
                buffFilter::setSortByDuration
        ));

        if (MethodHandlesUtil.isAvailable()) {
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.buff_filter_minions",
                    buffFilter::isShowMinions,
                    buffFilter::setShowMinions
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.buff_filter_mercenary",
                    buffFilter::isShowMercenary,
                    buffFilter::setShowMercenary
            ));
        }

        return entries;
    }
}
