package com.example.exile_overlay.client.config.screen.tab;

import com.example.exile_overlay.api.MethodHandlesUtil;
import com.example.exile_overlay.client.config.DropSoundConfig;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.screen.ConfigScreen;
import com.example.exile_overlay.client.config.screen.entry.ActionConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.BooleanConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.ConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.CycleConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.FloatSliderConfigEntry;
import com.example.exile_overlay.client.config.screen.entry.SectionHeaderEntry;
import com.example.exile_overlay.client.sound.CustomSoundManager;
import com.example.exile_overlay.client.sound.DropFilterManager;
import com.example.exile_overlay.compat.BotaniaCompat;
import com.example.exile_overlay.util.InventorySorterHelper;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 「拡張・連携 (Extensions)」タブ。
 * 他MOD互換（M&S, Botania, Lootr, Inventory Sorter）およびドロップサウンド設定を管理する。
 */
public class ExtensionsTab implements IConfigTab {

    @Override
    public Component getTitle() {
        return Component.translatable("exile_overlay.config.tab.extensions");
    }

    @Override
    public List<ConfigEntry> buildEntries(ConfigScreen screen) {
        List<ConfigEntry> entries = new ArrayList<>();
        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        boolean hasAnyCompat = false;

        // 1. Mine and Slash 連携
        if (MethodHandlesUtil.isAvailable()) {
            hasAnyCompat = true;
            entries.add(new SectionHeaderEntry("section.exile_overlay.mns"));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_mns_rpg_bars",
                    config::isCancelMnsRpgBars,
                    config::setCancelMnsRpgBars
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_mns_spell_hotbar",
                    config::isCancelMnsSpellHotbar,
                    config::setCancelMnsSpellHotbar
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_mns_cast_bar",
                    config::isCancelMnsCastBar,
                    config::setCancelMnsCastBar
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_mns_status_effects",
                    config::isCancelMnsStatusEffects,
                    config::setCancelMnsStatusEffects
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_mns_exp_action_bar",
                    config::isCancelMnsExpActionBar,
                    config::setCancelMnsExpActionBar
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.disable_mns_hpbar",
                    config::isDisableMnsHpBar,
                    val -> {
                        config.setDisableMnsHpBar(val);
                        MethodHandlesUtil.setNeatHpBarEnabled(!val);
                    }
            ));

            if (ModList.get().isLoaded("mnsdummy")) {
                entries.add(new BooleanConfigEntry(
                        "exile_overlay.config.exclude_dummy_boss_bar",
                        config::isExcludeDummyBossBar,
                        config::setExcludeDummyBossBar
                ));
            }
        }

        // 2. Botania 連携
        if (BotaniaCompat.isBotaniaLoaded()) {
            hasAnyCompat = true;
            entries.add(new SectionHeaderEntry("section.exile_overlay.botania"));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.cancel_botania_mana",
                    config::isCancelBotaniaMana,
                    config::setCancelBotaniaMana
            ));
        }

        // 3. Lootr 連携
        if (LootrHelper.isLoaded()) {
            hasAnyCompat = true;
            entries.add(new SectionHeaderEntry("section.exile_overlay.quick_loot"));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.quick_loot_enabled",
                    config::isQuickLootEnabled,
                    config::setQuickLootEnabled
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.auto_execute",
                    config::isAutoQuickLootEnabled,
                    config::setAutoQuickLootEnabled
            ));

            entries.add(new CycleConfigEntry<>(
                    List.of(EquipmentDisplayConfig.QuickLootMode.LOOT, EquipmentDisplayConfig.QuickLootMode.DROP),
                    config::getAutoQuickLootMode,
                    config::setAutoQuickLootMode,
                    mode -> Component.translatable(mode == EquipmentDisplayConfig.QuickLootMode.LOOT
                            ? "exile_overlay.config.mode.loot"
                            : "exile_overlay.config.mode.drop"),
                    Component.translatable("exile_overlay.config.auto_execute_mode.tooltip")
            ));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.key_execute",
                    config::isKeyQuickLootEnabled,
                    config::setKeyQuickLootEnabled
            ));

            entries.add(new CycleConfigEntry<>(
                    List.of(EquipmentDisplayConfig.QuickLootMode.LOOT, EquipmentDisplayConfig.QuickLootMode.DROP),
                    config::getKeyQuickLootMode,
                    config::setKeyQuickLootMode,
                    mode -> Component.translatable(mode == EquipmentDisplayConfig.QuickLootMode.LOOT
                            ? "exile_overlay.config.mode.loot"
                            : "exile_overlay.config.mode.drop"),
                    Component.translatable("exile_overlay.config.key_execute_mode.tooltip")
            ));
        }

        // 4. Inventory Sorter 連携
        if (LootrHelper.isLoaded() && InventorySorterHelper.isLoaded()) {
            hasAnyCompat = true;
            entries.add(new SectionHeaderEntry("section.exile_overlay.inventory_sorter"));

            entries.add(new BooleanConfigEntry(
                    "exile_overlay.config.auto_sort_lootr_chest",
                    config::isAutoSortLootrChest,
                    config::setAutoSortLootrChest
            ));
        }

        if (!hasAnyCompat) {
            entries.add(new SectionHeaderEntry("section.exile_overlay.no_mods"));
        }

        // 5. ドロップサウンド設定
        entries.add(new SectionHeaderEntry(
                Component.translatable("section.exile_overlay.drop_sound")
                        .append(" ")
                        .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
        ));

        DropSoundConfig dropSoundConfig = DropSoundConfig.getInstance();
        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.drop_sound_enabled",
                dropSoundConfig::isEnabled,
                dropSoundConfig::setEnabled,
                Component.translatable("exile_overlay.config.drop_sound_enabled.tooltip")
                        .append("\n")
                        .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
        ));

        // 全体音量スライダー（0〜200%）
        entries.add(new FloatSliderConfigEntry(
                "exile_overlay.config.drop_sound_master_volume",
                dropSoundConfig::getMasterVolume,
                dropSoundConfig::setMasterVolume,
                0.0f,
                2.0f,
                val -> Component.translatable("exile_overlay.config.drop_sound_master_volume", Math.round(val * 100) + "%")
        ));

        // フィルター選択
        entries.add(new ActionConfigEntry(
                Component.translatable("exile_overlay.config.drop_sound_filter_select", dropSoundConfig.getActiveFilter()),
                Component.translatable("exile_overlay.config.drop_sound_filter_select.tooltip"),
                btn -> {
                    DropFilterManager.cycleFilter();
                    btn.setMessage(Component.translatable("exile_overlay.config.drop_sound_filter_select", dropSoundConfig.getActiveFilter()));
                }
        ));

        // フィルターフォルダを開く
        entries.add(new ActionConfigEntry(
                Component.translatable("exile_overlay.config.drop_sound_open_filters"),
                Component.translatable("exile_overlay.config.drop_sound_open_filters.tooltip"),
                btn -> {
                    File dir = DropFilterManager.getFiltersDir();
                    if (dir != null && dir.exists()) {
                        Util.getPlatform().openFile(dir);
                    }
                }
        ));

        // サウンドフォルダを開く
        entries.add(new ActionConfigEntry(
                Component.translatable("exile_overlay.config.drop_sound_open_sounds"),
                Component.translatable("exile_overlay.config.drop_sound_open_sounds.tooltip"),
                btn -> {
                    File dir = CustomSoundManager.getSoundDir();
                    if (dir != null && dir.exists()) {
                        Util.getPlatform().openFile(dir);
                    }
                }
        ));

        // フィルター・音声リロード
        entries.add(new ActionConfigEntry(
                Component.translatable("exile_overlay.config.drop_sound_reload"),
                Component.translatable("exile_overlay.config.drop_sound_reload.tooltip"),
                btn -> {
                    DropFilterManager.reload();
                    CustomSoundManager.reloadSounds();
                    screen.rebuildCurrentTab();
                }
        ));

        return entries;
    }
}
