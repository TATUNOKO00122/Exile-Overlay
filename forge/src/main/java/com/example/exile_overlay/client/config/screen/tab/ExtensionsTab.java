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
import com.example.exile_overlay.client.sound.ExileAudioPlayer;
import com.example.exile_overlay.compat.BotaniaCompat;
import com.example.exile_overlay.util.InventorySorterHelper;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 「拡張・連携 (Extensions)」タブ。
 * 他MOD互換（M&S, Botania, Lootr, Inventory Sorter）およびドロップサウンド設定を管理する。
 */
public class ExtensionsTab implements IConfigTab {

    private boolean dropSoundCollapsed = true;

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
        entries.add(new SectionHeaderEntry("section.exile_overlay.drop_sound"));

        DropSoundConfig dropSoundConfig = DropSoundConfig.getInstance();
        entries.add(new BooleanConfigEntry(
                "exile_overlay.config.drop_sound_enabled",
                dropSoundConfig::isEnabled,
                dropSoundConfig::setEnabled,
                Component.translatable("exile_overlay.config.drop_sound_enabled.tooltip")
                        .append("\n")
                        .append(Component.translatable("exile_overlay.config.experimental").withStyle(s -> s.withColor(0xFFAA00)))
        ));

        Component dropToggleText = Component.literal(dropSoundCollapsed ? "\u25B6 " : "\u25BC ")
                .append(Component.translatable("exile_overlay.config.drop_sound.collapse"));
        entries.add(new ActionConfigEntry(
                dropToggleText,
                Component.translatable("exile_overlay.config.drop_sound.collapse.tooltip"),
                btn -> {
                    dropSoundCollapsed = !dropSoundCollapsed;
                    screen.rebuildCurrentTab();
                }
        ));

        if (!dropSoundCollapsed) {
            String[] rarities = {"unique", "mythic", "legendary"};
            for (String rarity : rarities) {
                DropSoundConfig.RaritySound raritySound = dropSoundConfig.getRaritySound(rarity);
                if (raritySound == null) continue;

                String rKey = "exile_overlay.config.rarity." + rarity;
                int rColor = getRarityColor(rarity);

            // レアリティ有効/無効
            entries.add(new BooleanConfigEntry(
                    rKey,
                    raritySound::isEnabled,
                    raritySound::setEnabled,
                    enabled -> {
                        Component onOff = Component.translatable(enabled ? "exile_overlay.config.on" : "exile_overlay.config.off");
                        return Component.translatable(rKey).withStyle(s -> s.withColor(rColor))
                                .append(Component.literal(": ").withStyle(s -> s.withColor(0xFFFFFF)))
                                .append(onOff.copy().withStyle(s -> s.withColor(rColor)));
                    },
                    null,
                    null
            ));

            // サウンド選択
            entries.add(new ActionConfigEntry(
                    Component.translatable("exile_overlay.config.drop_sound_select", formatSoundName(raritySound.getSound())),
                    Component.translatable("exile_overlay.config.drop_sound_select.tooltip"),
                    btn -> {
                        cycleDropSound(raritySound);
                        btn.setMessage(Component.translatable("exile_overlay.config.drop_sound_select", formatSoundName(raritySound.getSound())));
                        playPreviewSound(raritySound);
                    }
            ));

            // 音量スライダー（0〜200%）
            entries.add(new FloatSliderConfigEntry(
                    "exile_overlay.config.drop_sound_volume",
                    raritySound::getVolume,
                    raritySound::setVolume,
                    0.0f, 2.0f,
                    val -> Component.translatable("exile_overlay.config.drop_sound_volume", Math.round(val * 100) + "%")
            ));
        }
        }

        return entries;
    }

    private static int getRarityColor(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "legendary" -> 0xFFAA00;   // GOLD
            case "mythic" -> 0xAA00AA;      // DARK_PURPLE
            case "unique" -> 0xFF5555;      // RED
            default -> 0xFFFFFF;
        };
    }

    private static String formatSoundName(String soundLoc) {
        if (soundLoc == null || soundLoc.isEmpty()) {
            return Component.translatable("exile_overlay.config.none").getString();
        }
        if (soundLoc.startsWith("exile_overlay:")) {
            // 識別子から表示名を生成（例: "exile_overlay:name.mp3" → "name.mp3"）
            return soundLoc.substring(14);
        }
        return soundLoc;
    }

    private static void cycleDropSound(DropSoundConfig.RaritySound config) {
        List<String> options = new ArrayList<>();
        List<String> customSounds = CustomSoundManager.getAvailableCustomSounds();
        for (String custom : customSounds) {
            options.add("exile_overlay:" + custom);
        }

        if (options.isEmpty()) {
            config.setSound("");
            return;
        }

        String current = config.getSound();
        int index = options.indexOf(current);
        if (index == -1 || index >= options.size() - 1) {
            config.setSound(options.get(0));
        } else {
            config.setSound(options.get(index + 1));
        }
    }

    private static void playPreviewSound(DropSoundConfig.RaritySound raritySound) {
        String soundName = raritySound.getSound();
        if (soundName == null || soundName.isEmpty()) return;

        // カスタム音（OGG / MP3）は ExileAudioPlayer で PCM 増幅再生（0〜2000%対応）
        java.io.File customSoundFile = CustomSoundManager.getCustomSoundFile(soundName);
        if (customSoundFile != null) {
            ExileAudioPlayer.playCustomSound(customSoundFile, raritySound.getVolume());
            return;
        }

        ResourceLocation soundLoc = CustomSoundManager.getSafeSoundLocation(soundName);
        if (soundLoc != null) {
            Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                    SoundEvent.createVariableRangeEvent(soundLoc).getLocation(),
                    SoundSource.MASTER,
                    raritySound.getVolume(), 1.0F,
                    RandomSource.create(),
                    false, 0,
                    SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0, true));
        }
    }
}
