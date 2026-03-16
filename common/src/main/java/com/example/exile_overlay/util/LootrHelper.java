package com.example.exile_overlay.util;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public class LootrHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Boolean loaded = null;

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                Class.forName("noobanidus.mods.lootr.api.LootrAPI");
                loaded = true;
                LOGGER.info("Lootr detected, enabling Auto Quick Loot integration.");
            } catch (ClassNotFoundException e) {
                loaded = false;
                LOGGER.debug("Lootr not found, Auto Quick Loot feature hidden.");
            }
        }
        return loaded;
    }
}