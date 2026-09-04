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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class DropFilterManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String FILTERS_DIR_NAME = "filters";
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
        if (availableFilterFiles.isEmpty()) {
            activeFilter = null;
            return;
        }

        DropSoundConfig config = DropSoundConfig.getInstance();
        String targetFile = config.getActiveFilter();

        if (targetFile == null || targetFile.isEmpty() || !availableFilterFiles.contains(targetFile)) {
            targetFile = availableFilterFiles.get(0);
            config.setActiveFilter(targetFile);
        }

        File file = new File(filtersDir, targetFile);
        if (!file.exists()) {
            activeFilter = null;
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            activeFilter = DropFilter.parse(reader, targetFile);
            LOGGER.info("Loaded drop filter: {} with {} sections", targetFile, activeFilter.getSections().size());
        } catch (Exception e) {
            LOGGER.error("Failed to load drop filter file: {}", targetFile, e);
            activeFilter = null;
        }
    }

    public static void cycleFilter() {
        refreshFilterFileList();
        if (availableFilterFiles.isEmpty()) {
            activeFilter = null;
            return;
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
}
