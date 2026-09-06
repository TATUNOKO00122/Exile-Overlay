package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.ExileOverlayConfigManager;
import com.example.exile_overlay.client.config.position.HudPosition;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.DraggableHudConfigScreen;
import com.example.exile_overlay.client.config.screen.entry.ActionConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.BooleanConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.FloatSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.IntSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.SectionHeaderEntry;
import com.example.exile_overlay.client.render.DayCounterConfig;
import com.example.exile_overlay.dmgtracker.config.TrackerConfig;
import com.example.exile_overlay.dmgtracker.network.TrackerSyncS2C;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 「一般 (General)」タブ。
 * プリセット操作、HUDエディタ、デイリーカウンター等の設定を管理する。
 */
public class GeneralTab implements IConfigTab {

    private static final String DAY_COUNTER_KEY = "day_counter";

    @Override
    public Component getTitle() {
        return Component.translatable("exile_overlay.config.tab.general");
    }

    @Override
    public List<ConfigEntry> buildEntries(ConfigScreen screen) {
        List<ConfigEntry> entries = new ArrayList<>();

        // 1. プリセットセクション
        entries.add(new SectionHeaderEntry("section.exile_overlay.presets"));

        boolean hasUndo = ExileOverlayConfigManager.getInstance().hasBackupSnapshot();
        String presetKey = hasUndo ? "exile_overlay.config.preset_undo" : "exile_overlay.config.preset_vanilla_mns_default";

        entries.add(new ActionConfigEntry(
                Component.translatable(presetKey),
                Component.translatable(presetKey + ".tooltip"),
                btn -> {
                    screen.handlePresetButtonClick();
                }
        ));

        // 2. HUD位置エディタ
        entries.add(new SectionHeaderEntry("section.exile_overlay.hud_position"));

        entries.add(new ActionConfigEntry(
                Component.translatable("exile_overlay.config.open_hud_editor"),
                Component.translatable("exile_overlay.config.open_hud_editor.tooltip"),
                btn -> Minecraft.getInstance().setScreen(new DraggableHudConfigScreen(screen))
        ));

        // 3. デイリーカウンター
        entries.add(new SectionHeaderEntry("section.exile_overlay.day_counter"));

        HudPositionManager posMgr = HudPositionManager.getInstance();
        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.day_counter_enabled",
                () -> posMgr.getPosition(DAY_COUNTER_KEY).isVisible(),
                val -> {
                    HudPosition pos = posMgr.getPosition(DAY_COUNTER_KEY);
                    posMgr.setPosition(DAY_COUNTER_KEY, pos.withVisible(val));
                }
        ));

        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.day_counter_scale",
                () -> posMgr.getPosition(DAY_COUNTER_KEY).getScale(),
                val -> {
                    HudPosition pos = posMgr.getPosition(DAY_COUNTER_KEY);
                    posMgr.setPosition(DAY_COUNTER_KEY, pos.withScale(val));
                },
                0.5f, 3.0f
        ));

        DayCounterConfig dayCounterConfig = DayCounterConfig.getInstance();
        entries.add(new IntSliderConfigEntry(
                "exile_overlay.config.day_counter_volume",
                dayCounterConfig::getSoundVolume,
                val -> {
                    dayCounterConfig.setSoundVolume(val);
                    dayCounterConfig.save();
                },
                0, 10
        ));

        // 4. ダメージトラッカー（サーバー側導入時）
        if (TrackerSyncS2C.ClientTrackerData.serverHasMod()) {
            entries.add(new SectionHeaderEntry("section.exile_overlay.damage_tracker"));

            TrackerConfig trackerConfig = TrackerConfig.getInstance();
            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.damage_tracker_enabled",
                    trackerConfig::isEnabled,
                    trackerConfig::setEnabled,
                    Component.translatable("exile_overlay.config.damage_tracker_enabled.tooltip")
                            .append("\n")
                            .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
            ));

            entries.add(new IntSliderConfigEntry(
                    "exile_overlay.config.damage_tracker_max_skills",
                    TrackerConfig::getMaxSkillsShown,
                    trackerConfig::setMaxSkillsShown,
                    1, 20,
                    val -> Component.translatable("exile_overlay.config.damage_tracker_max_skills", val)
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.damage_tracker_show_individual_dps",
                    TrackerConfig::isShowIndividualDps,
                    trackerConfig::setShowIndividualDps
            ));

            if (MethodHandlesUtil.isMercenarySupported()) {
                entries.add(new BooleanConfigEntry(
                        "exile_overlay.config.damage_tracker_exclude_mercenary",
                        TrackerConfig::isExcludeMercenaryDamage,
                        trackerConfig::setExcludeMercenaryDamage,
                        Component.translatable("exile_overlay.config.damage_tracker_exclude_mercenary.tooltip")
                ));
            }
        }

        return entries;
    }
}
