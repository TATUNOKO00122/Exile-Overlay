package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.BuffOverlayFilterConfig;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.render.entity.EntityHealthBarConfig;
import com.example.exile_overlay.client.config.screen.entry.ActionConfigEntry;
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

    private boolean buffFilterCollapsed = true;

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
                    hpBarConfig::setShowPoison
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.hp_bar_show_bleed",
                    hpBarConfig::isShowBleed,
                    hpBarConfig::setShowBleed
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
                "exile_overlay.config.enable_shadow",
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
                equipConfig::setSimpleSkillKeybindDisplay,
                null,
                Component.translatable("exile_overlay.config.simple_skill_keybind.tooltip"),
                screen::rebuildCurrentTab
        ));

        if (equipConfig.isSimpleSkillKeybindDisplay()) {
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.simple_skill_charge_max_display",
                    equipConfig::isSimpleSkillChargeMaxDisplay,
                    equipConfig::setSimpleSkillChargeMaxDisplay
            ));
        }

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

        BuffOverlayFilterConfig.OverlayFilter buffFilter = BuffOverlayFilterConfig.getInstance().getBuffOverlay();

        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.buff_filter_sort_by_duration",
                buffFilter::isSortByDuration,
                buffFilter::setSortByDuration
        ));

        Component buffToggleText = Component.literal(buffFilterCollapsed ? "\u25B6 " : "\u25BC ")
                .append(Component.translatable("exile_overlay.config.filter_settings"));
        entries.add(new ActionConfigEntry(
                buffToggleText,
                Component.translatable("exile_overlay.config.filter_settings.tooltip"),
                btn -> {
                    buffFilterCollapsed = !buffFilterCollapsed;
                    screen.rebuildCurrentTab();
                }
        ));

        if (!buffFilterCollapsed) {
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.buff_filter_vanilla_buffs",
                    buffFilter::isShowVanillaBuffs,
                    buffFilter::setShowVanillaBuffs
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.buff_filter_vanilla_debuffs",
                    buffFilter::isShowVanillaDebuffs,
                    buffFilter::setShowVanillaDebuffs
            ));

            if (MethodHandlesUtil.isAvailable()) {
                entries.add(new BooleanConfigEntry(
                        "exile_overlay.config.buff_filter_mns_buffs",
                        buffFilter::isShowMnsBuffs,
                        buffFilter::setShowMnsBuffs
                ));

                entries.add(new BooleanConfigEntry(
                        "exile_overlay.config.buff_filter_mns_debuffs",
                        buffFilter::isShowMnsDebuffs,
                        buffFilter::setShowMnsDebuffs
                ));
            }
        }

        return entries;
    }
}
