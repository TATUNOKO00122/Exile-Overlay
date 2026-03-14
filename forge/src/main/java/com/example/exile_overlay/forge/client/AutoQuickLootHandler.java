package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class AutoQuickLootHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/AutoQuickLoot");
    private static final WeakHashMap<Screen, Boolean> triggered = new WeakHashMap<>();

    private static boolean initialized = false;
    private static Class<?> packetsClass = null;
    private static Class<?> myPacketClass = null;
    private static Class<?> lootMenuPacketClass = null;
    private static Class<?> modeClass = null;
    private static Object dropMode = null;
    private static Method sendToServerMethod = null;
    private static Field drawableListField = null;

    private static void initializeReflection() {
        if (initialized) return;
        initialized = true;

        try {
            packetsClass = Class.forName("com.robertx22.library_of_exile.main.Packets");
            myPacketClass = Class.forName("com.robertx22.library_of_exile.main.MyPacket");
            lootMenuPacketClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket");
            modeClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket$Mode");

            for (Object constant : modeClass.getEnumConstants()) {
                if (constant.toString().equals("DROP")) {
                    dropMode = constant;
                    break;
                }
            }

            for (Method m : packetsClass.getMethods()) {
                if (m.getName().equals("sendToServer") && m.getParameterCount() == 1) {
                    sendToServerMethod = m;
                    break;
                }
            }

            drawableListField = findDrawableListField();
            
            if (sendToServerMethod != null && drawableListField != null) {
                LOGGER.info("Auto Quick Loot initialized successfully");
            } else {
                LOGGER.warn("Auto Quick Loot: sendToServerMethod={}, drawableListField={}", 
                    sendToServerMethod != null, drawableListField != null);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Mine and Slash not found, Auto Quick Loot will be disabled");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Auto Quick Loot reflection", e);
        }
    }

    private static Field findDrawableListField() {
        for (Field f : Screen.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (!EquipmentDisplayConfig.getInstance().isAutoQuickLootEnabled()) {
            return;
        }

        initializeReflection();
        if (packetsClass == null || lootMenuPacketClass == null || modeClass == null || 
            dropMode == null || sendToServerMethod == null) {
            return;
        }

        Screen screen = event.getScreen();
        if (screen == null) {
            return;
        }

        long window = Minecraft.getInstance().getWindow().getWindow();
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }

        BlockPos pos = blockHit.getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        Block block = state.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        if (!blockId.equals("lootr:lootr_chest")) {
            return;
        }

        if (triggered.getOrDefault(screen, false)) {
            return;
        }

        boolean foundButton = findButtonInScreen(screen);

        if (foundButton) {
            try {
                Object packet = lootMenuPacketClass.getConstructor(modeClass).newInstance(dropMode);
                sendToServerMethod.invoke(null, packet);

                mc.setScreen(null);
                triggered.put(screen, true);
                LOGGER.info("Auto Quick Loot triggered for lootr:lootr_chest");
            } catch (Exception e) {
                LOGGER.error("Failed to send BackPackLootMenuPacket", e);
            }
        }
    }

    private static boolean findButtonInScreen(Screen screen) {
        if (drawableListField == null) {
            return false;
        }

        try {
            List<?> drawables = (List<?>) drawableListField.get(screen);
            if (drawables == null) {
                return false;
            }

            for (Object obj : drawables) {
                String className = obj.getClass().getName();
                if (className.contains("BackpackQuickLootButton")) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to access drawable list", e);
        }

        return false;
    }
}