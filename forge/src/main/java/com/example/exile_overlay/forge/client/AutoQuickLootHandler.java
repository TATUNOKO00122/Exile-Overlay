package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT)
public class AutoQuickLootHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/AutoQuickLoot");
    private static final long COOLDOWN_MS = 1000;
    private static final Map<BlockPos, Long> cooldownTracker = new ConcurrentHashMap<>();

    private static final Set<String> LOOTR_BLOCKS = Set.of(
        "lootr:lootr_chest",
        "lootr:lootr_trapped_chest",
        "lootr:lootr_barrel",
        "lootr:lootr_shulker"
    );

    // ========== Reflection Cache ==========

    private static boolean reflectionInitialized = false;
    private static boolean reflectionAvailable = false;
    private static Class<?> lootMenuPacketClass;
    private static Class<?> modeClass;
    private static Object lootMode;
    private static Object dropMode;
    private static Method sendToServerMethod;

    // ========== Per-Screen State ==========

    private static BlockPos cachedTargetPos = null;
    private static boolean autoTriggerPending = false;
    private static EquipmentDisplayConfig.QuickLootMode pendingAutoMode = null;
    private static boolean keyTriggerFired = false;

    // ========== Reflection Init ==========

    private static synchronized void ensureReflectionInitialized() {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        try {
            Class<?> packetsClass = Class.forName("com.robertx22.library_of_exile.main.Packets");
            lootMenuPacketClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket");
            modeClass = Class.forName("com.robertx22.mine_and_slash.vanilla_mc.packets.backpack.BackPackLootMenuPacket$Mode");

            for (Object constant : modeClass.getEnumConstants()) {
                String name = constant.toString();
                if ("LOOT".equals(name)) lootMode = constant;
                else if ("DROP".equals(name)) dropMode = constant;
            }

            for (Method m : packetsClass.getMethods()) {
                if ("sendToServer".equals(m.getName()) && m.getParameterCount() == 1) {
                    sendToServerMethod = m;
                    break;
                }
            }

            reflectionAvailable = sendToServerMethod != null && lootMode != null && dropMode != null;
            if (reflectionAvailable) {
                LOGGER.info("Quick Loot: M&S reflection initialized");
            } else {
                LOGGER.warn("Quick Loot: partial init, sendToServer={}, lootMode={}, dropMode={}",
                    sendToServerMethod != null, lootMode != null, dropMode != null);
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Quick Loot: M&S not found, disabled");
        } catch (Exception e) {
            LOGGER.error("Quick Loot: reflection init failed", e);
        }
    }

    // ========== Utility ==========

    private static BlockPos resolveTargetBlock(Minecraft mc) {
        if (mc.hitResult instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos();
        }
        var lookResult = mc.player.pick(mc.player.getBlockReach(), 0, false);
        if (lookResult instanceof BlockHitResult lookHit) {
            return lookHit.getBlockPos();
        }
        return null;
    }

    private static boolean isLootrBlock(Minecraft mc, BlockPos pos) {
        if (mc.level == null || pos == null) return false;
        BlockState state = mc.level.getBlockState(pos);
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return LOOTR_BLOCKS.contains(blockId);
    }

    private static boolean isOnCooldown(BlockPos pos) {
        Long last = cooldownTracker.get(pos);
        return last != null && (System.currentTimeMillis() - last) < COOLDOWN_MS;
    }

    private static void resetState() {
        cachedTargetPos = null;
        autoTriggerPending = false;
        pendingAutoMode = null;
        keyTriggerFired = false;
    }

    // ========== Events ==========

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        resetState();
        if (!LootrHelper.isLoaded()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ensureReflectionInitialized();
        if (!reflectionAvailable) return;

        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        if (!config.isQuickLootEnabled()) return;

        BlockPos pos = resolveTargetBlock(mc);
        if (pos == null || !isLootrBlock(mc, pos)) return;
        if (isOnCooldown(pos)) return;

        cachedTargetPos = pos;
        keyTriggerFired = false;

        if (config.isAutoQuickLootEnabled()) {
            autoTriggerPending = true;
            pendingAutoMode = config.getAutoQuickLootMode();
        }
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        resetState();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (cachedTargetPos == null) return;

        EquipmentDisplayConfig config = EquipmentDisplayConfig.getInstance();
        if (!config.isQuickLootEnabled()) {
            resetState();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (autoTriggerPending && pendingAutoMode != null) {
            autoTriggerPending = false;
            EquipmentDisplayConfig.QuickLootMode mode = pendingAutoMode;
            sendQuickLoot(mc, cachedTargetPos, mode);
            if (mode == EquipmentDisplayConfig.QuickLootMode.DROP) {
                mc.setScreen(null);
            }
            resetState();
            return;
        }

        if (config.isKeyQuickLootEnabled() && !keyTriggerFired && isCtrlPressed(mc)) {
            keyTriggerFired = true;
            EquipmentDisplayConfig.QuickLootMode mode = config.getKeyQuickLootMode();
            sendQuickLoot(mc, cachedTargetPos, mode);
            if (mode == EquipmentDisplayConfig.QuickLootMode.DROP) {
                mc.setScreen(null);
            }
            resetState();
        }
    }

    // ========== Packet Send ==========

    private static void sendQuickLoot(Minecraft mc, BlockPos pos, EquipmentDisplayConfig.QuickLootMode mode) {
        try {
            Object mnsMode = (mode == EquipmentDisplayConfig.QuickLootMode.DROP) ? dropMode : lootMode;
            Object packet = lootMenuPacketClass.getConstructor(modeClass).newInstance(mnsMode);
            sendToServerMethod.invoke(null, packet);
            cooldownTracker.put(pos, System.currentTimeMillis());
            LOGGER.info("Quick Loot: {} at {}", mode, pos);
        } catch (Exception e) {
            LOGGER.error("Quick Loot: send failed", e);
        }
    }

    private static boolean isCtrlPressed(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
    }
}
