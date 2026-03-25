package com.example.exile_overlay.client.favorite;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class FavoriteKeyBindings {

    private static final Logger LOGGER = LoggerFactory.getLogger(FavoriteKeyBindings.class);

    public static KeyMapping KEY_TOGGLE;

    private static final String CATEGORY = "category.exile_overlay.favorite";

    private FavoriteKeyBindings() {}

    public static void init() {
        KEY_TOGGLE = new KeyMapping(
                "key.exile_overlay.favorite.toggle",
                GLFW.GLFW_KEY_LEFT_ALT,
                CATEGORY
        );
        LOGGER.info("FavoriteKeyBindings initialized: Toggle=LEFT_ALT");
    }

    public static void register(Consumer<KeyMapping> registry) {
        if (KEY_TOGGLE == null) {
            init();
        }
        registry.accept(KEY_TOGGLE);
        LOGGER.info("FavoriteKeyBindings registered");
    }

    public static boolean isToggle() {
        if (KEY_TOGGLE == null) return false;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            InputConstants.Key boundKey = KEY_TOGGLE.getKey();
            if (boundKey.getType() == InputConstants.Type.KEYSYM) {
                long window = mc.getWindow().getWindow();
                return InputConstants.isKeyDown(window, boundKey.getValue());
            }
        }
        return KEY_TOGGLE.isDown();
    }
}