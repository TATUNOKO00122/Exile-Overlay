package com.example.exile_overlay.client.sound;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.client.config.DropSoundConfig;
import com.example.exile_overlay.util.DropItemResolver;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DropFilterManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FILTERS_DIR_NAME = "filters";
    public static final String DEFAULT_FILTER_FILE = "default.filter";

    private static File filtersDir;
    private static final List<String> availableFilterFiles = new ArrayList<>();
    private static DropFilter activeFilter;

    public static void init() {
        File configDir = FMLPaths.CONFIGDIR.get().resolve(ExileOverlayMod.MOD_ID).toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        filtersDir = new File(configDir, FILTERS_DIR_NAME);
        if (!filtersDir.exists()) {
            filtersDir.mkdirs();
            LOGGER.info("Created drop filter directory at: {}", filtersDir.getAbsolutePath());
        }

        refreshFilterFileList();

        if (availableFilterFiles.isEmpty()) {
            createDefaultFilterFile();
            refreshFilterFileList();
        }

        loadActiveFilter();
    }

    public static File getFiltersDir() {
        return filtersDir;
    }

    public static List<String> getAvailableFilterFiles() {
        return Collections.unmodifiableList(availableFilterFiles);
    }

    public static DropFilter getActiveFilter() {
        return activeFilter;
    }

    public static void refreshFilterFileList() {
        availableFilterFiles.clear();
        if (filtersDir == null || !filtersDir.exists() || !filtersDir.isDirectory()) {
            return;
        }

        File[] files = filtersDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase(Locale.ROOT);
            return lower.endsWith(".filter") || lower.endsWith(".txt");
        });

        if (files != null) {
            for (File file : files) {
                availableFilterFiles.add(file.getName());
            }
        }
        Collections.sort(availableFilterFiles);
    }

    public static void loadActiveFilter() {
        DropSoundConfig config = DropSoundConfig.getInstance();
        String targetFile = config.getActiveFilter();

        if (targetFile == null || targetFile.isEmpty() || !availableFilterFiles.contains(targetFile)) {
            if (availableFilterFiles.contains(DEFAULT_FILTER_FILE)) {
                targetFile = DEFAULT_FILTER_FILE;
            } else if (!availableFilterFiles.isEmpty()) {
                targetFile = availableFilterFiles.get(0);
            } else {
                targetFile = DEFAULT_FILTER_FILE;
            }
            config.setActiveFilter(targetFile);
        }

        File file = new File(filtersDir, targetFile);
        if (!file.exists()) {
            activeFilter = createFallbackFilter();
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            activeFilter = DropFilter.parse(reader, targetFile);
            LOGGER.info("Loaded drop filter: {} with {} sections", targetFile, activeFilter.getSections().size());
        } catch (Exception e) {
            LOGGER.error("Failed to load drop filter file: {}", targetFile, e);
            activeFilter = createFallbackFilter();
        }
    }

    public static void cycleFilter() {
        refreshFilterFileList();
        if (availableFilterFiles.isEmpty()) {
            createDefaultFilterFile();
            refreshFilterFileList();
        }

        DropSoundConfig config = DropSoundConfig.getInstance();
        String current = config.getActiveFilter();
        int currentIndex = availableFilterFiles.indexOf(current);
        int nextIndex = (currentIndex + 1) % availableFilterFiles.size();
        String nextFile = availableFilterFiles.get(nextIndex);

        config.setActiveFilter(nextFile);
        config.save();
        loadActiveFilter();
    }

    public static void reload() {
        refreshFilterFileList();
        loadActiveFilter();
    }

    public static DropFilter.SoundRule match(ItemStack stack) {
        if (activeFilter == null) {
            return null;
        }
        DropItemResolver.ItemInfo info = DropItemResolver.resolve(stack);
        return activeFilter.match(info);
    }

    private static DropFilter createFallbackFilter() {
        DropFilter filter = new DropFilter("Fallback Filter");
        DropFilter.FilterSection section = new DropFilter.FilterSection("default", 1.0f);
        section.addTarget("rarity:unique");
        section.addTarget("rarity:mythic");
        filter.getSections().add(section);
        return filter;
    }

    private static void createDefaultFilterFile() {
        File defaultFile = new File(filtersDir, DEFAULT_FILTER_FILE);
        String defaultContent = """
                # ==============================================================================
                # Exile Overlay Drop Sound Filter
                # ==============================================================================
                # 構文:
                #   [サウンド名: 音量(0.0〜2.0)]
                #   アイテムID または rarity:レアリティ名
                #   アイテムID@レアリティ名 (例: mmorpg:omen@legendary, mmorpg:omen@mythic)
                #
                # ※上のセクションから順に評価され、最初に一致したサウンドが鳴ります。
                # ※[none] または [mute] の下に書いたアイテムは消音（除外）されます。
                # ※行の先頭に ! を付けると、そのセクションから除外されます。
                # ※ゲーム内でアイテムにカーソルを乗せて Ctrl + C を押すと、IDをコピーできます。
                # ==============================================================================

                # 1. 消音（除外したいアイテム）
                [none]
                # mmorpg:currency/portal_scroll

                # 2. 最高レア・神ドロップ（超大音量）
                [god_tier_alert: 2.0]
                mmorpg:currency/mirror
                mmorpg:currency/divine
                minecraft:netherite_block

                # 3. 高価値カレンシー・アイテム
                [high_tier_alert: 1.5]
                mmorpg:currency/exalted
                mmorpg:currency/chaos
                minecraft:netherite_ingot

                # 4. ユニーク・ミシック装備（一般）
                [normal_unique: 1.0]
                rarity:unique
                rarity:mythic

                # 5. レジェンダリー装備
                [legendary_drop: 0.8]
                rarity:legendary
                """;

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(defaultFile), StandardCharsets.UTF_8)) {
            writer.write(defaultContent);
            LOGGER.info("Generated default drop filter file at: {}", defaultFile.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to generate default drop filter file", e);
        }
    }
}
