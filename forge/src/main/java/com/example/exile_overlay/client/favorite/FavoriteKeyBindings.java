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
    public static KeyMapping KEY_BYPASS;

    private static final String CATEGORY = "category.exile_overlay.favorite";

    private FavoriteKeyBindings() {}

    public static void init() {
        KEY_TOGGLE = new KeyMapping(
                "key.exile_overlay.favorite.toggle",
                GLFW.GLFW_KEY_LEFT_ALT,
                CATEGORY
        );
        KEY_BYPASS = new KeyMapping(
                "key.exile_overlay.favorite.bypass",
                GLFW.GLFW_KEY_V,
                CATEGORY
        );
        LOGGER.info("FavoriteKeyBindings initialized: Toggle=LEFT_ALT, Bypass=V");
    }

    public static void register(Consumer<KeyMapping> registry) {
        if (KEY_TOGGLE == null || KEY_BYPASS == null) {
            init();
        }
        registry.accept(KEY_TOGGLE);
        registry.accept(KEY_BYPASS);
        LOGGER.info("FavoriteKeyBindings registered");
    }

    public static boolean isToggle() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_ALT);
    }

    public static boolean isBypass() {
        return isKeyDown(GLFW.GLFW_KEY_V);
    }

    private static boolean isKeyDown(int glfwKeyCode) {
        try {
            long window = Minecraft.getInstance().getWindow().getWindow();
            return GLFW.glfwGetKey(window, glfwKeyCode) == GLFW.GLFW_PRESS;
        } catch (Exception e) {
            return false;
        }
    }
}