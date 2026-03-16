package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.util.LootrHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class AutoQuickLootHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/AutoQuickLoot");
    private static final long COOLDOWN_MS = 3000;
    private static final Map<BlockPos, Long> cooldownTracker = new ConcurrentHashMap<>();
    private static final Set<String> LOOTR_BLOCKS = Set.of(
        "lootr:lootr_chest",
        "lootr:lootr_trapped_chest",
        "lootr:lootr_barrel",
        "lootr:lootr_shulker"
    );

    private static boolean initialized = false;
    private static Class<?> lootMenuPacketClass = null;
    private static Class<?> modeClass = null;
    private static Object dropMode = null;
    private static Method sendToServerMethod = null;
    private static Field renderablesField = null;

    private static KeyMapping quickLootKey;

    public static void registerKeyMapping(KeyMapping mapping) {
        quickLootKey = mapping;
    }

    public static KeyMapping getKeyMapping() {
        return quickLootKey;
    }

    private static void initializeReflection() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> packetsClass = Class.forName("com.robertx22.library_of_exile.main.Packets");
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

            renderablesField = findRenderablesField();

            if (sendToServerMethod != null && renderablesField != null && dropMode != null) {
                LOGGER.info("Auto Quick Loot initialized successfully");
            } else {
                LOGGER.warn("Auto Quick Loot partial init: sendToServer={}, renderables={}, dropMode={}",
                    sendToServerMethod != null, renderablesField != null, dropMode != null);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Mine and Slash not found, Auto Quick Loot disabled");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize Auto Quick Loot", e);
        }
    }

    private static Field findRenderablesField() {
        for (Field f : Screen.class.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return f;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!LootrHelper.isLoaded()) return;

        cleanupExpiredCooldowns();

        if (!EquipmentDisplayConfig.getInstance().isAutoQuickLootEnabled()) return;

        initializeReflection();
        if (!isReflectionReady()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen == null) return;

        if (!isKeyPressed(mc)) return;

        BlockPos targetPos = getTargetBlockPos(mc);
        if (targetPos == null || isOnCooldown(targetPos)) return;

        BlockState state = mc.level.getBlockState(targetPos);
        Block block = state.getBlock();
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        if (!LOOTR_BLOCKS.contains(blockId)) return;

        if (hasQuickLootButton(mc.screen)) {
            executeQuickLoot(mc, targetPos, blockId);
        }
    }

    private static void cleanupExpiredCooldowns() {
        long now = System.currentTimeMillis();
        cooldownTracker.entrySet().removeIf(entry -> (now - entry.getValue()) > COOLDOWN_MS * 2);
    }

    private static boolean isReflectionReady() {
        return lootMenuPacketClass != null && modeClass != null &&
               dropMode != null && sendToServerMethod != null && renderablesField != null;
    }

    private static boolean isKeyPressed(Minecraft mc) {
        if (quickLootKey != null) {
            return quickLootKey.isDown();
        }
        long window = mc.getWindow().getWindow();
        return org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
    }

    private static BlockPos getTargetBlockPos(Minecraft mc) {
        if (!(mc.hitResult instanceof BlockHitResult blockHit)) return null;
        return blockHit.getBlockPos();
    }

    private static boolean isOnCooldown(BlockPos pos) {
        Long lastTrigger = cooldownTracker.get(pos);
        if (lastTrigger == null) return false;
        return (System.currentTimeMillis() - lastTrigger) < COOLDOWN_MS;
    }

    private static boolean hasQuickLootButton(Screen screen) {
        if (renderablesField == null) return false;

        try {
            List<?> renderables = (List<?>) renderablesField.get(screen);
            if (renderables == null) return false;

            for (Object obj : renderables) {
                String className = obj.getClass().getName();
                if (className.contains("BackpackQuickLootButton")) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to check screen buttons", e);
        }
        return false;
    }

    private static void executeQuickLoot(Minecraft mc, BlockPos pos, String blockId) {
        try {
            Object packet = lootMenuPacketClass.getConstructor(modeClass).newInstance(dropMode);
            sendToServerMethod.invoke(null, packet);

            mc.setScreen(null);
            cooldownTracker.put(pos, System.currentTimeMillis());
            LOGGER.info("Quick loot executed for {}", blockId);
        } catch (Exception e) {
            LOGGER.error("Failed to execute quick loot", e);
        }
    }
}