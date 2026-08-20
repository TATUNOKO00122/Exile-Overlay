package com.example.exile_overlay.client.event;

import com.example.exile_overlay.ExileOverlayMod;
import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.util.LootrHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ExileOverlayMod.MOD_ID, value = Dist.CLIENT)
public class AutoQuickLootHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/AutoQuickLoot");
    private static final EquipmentDisplayConfig EQUIP_CONFIG = EquipmentDisplayConfig.getInstance();
    private static final long COOLDOWN_MS = 1000;
    private static final Map<BlockPos, Long> cooldownTracker = new ConcurrentHashMap<>();

    // ========== Reflection Cache ==========

    private static boolean reflectionInitialized = false;
    private static boolean reflectionAvailable = false;
    private static Class<?> lootMenuPacketClass;
    private static Class<?> modeClass;
    private static Object lootMode;
    private static Object dropMode;
    private static Method sendToServerMethod;

    // ========== Per-Screen State ==========

    private static BlockPos lastClickedLootrPos = null;
    private static Integer lastClickedLootrEntityId = null;
    private static long lastClickedLootrTime = 0;

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
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (LootrHelper.isLootrContainerAt(event.getLevel(), event.getPos())) {
            lastClickedLootrPos = event.getPos();
            lastClickedLootrEntityId = null;
            lastClickedLootrTime = System.currentTimeMillis();
            LOGGER.debug("Quick Loot: Clicked Lootr block at {}", lastClickedLootrPos);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (LootrHelper.isLootrEntity(event.getTarget())) {
            lastClickedLootrPos = event.getTarget().blockPosition();
            lastClickedLootrEntityId = event.getTarget().getId();
            lastClickedLootrTime = System.currentTimeMillis();
            LOGGER.debug("Quick Loot: Clicked Lootr entity {} at {}", event.getTarget().getType(), lastClickedLootrPos);
        }
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        resetState();
        // 期限切れのクールダウンエントリをパージする（無制限成長を防ぐ）
        purgeCooldownTracker();
        if (!LootrHelper.isLoaded()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        ensureReflectionInitialized();
        if (!reflectionAvailable) return;

        EquipmentDisplayConfig config = EQUIP_CONFIG;
        if (!config.isQuickLootEnabled()) return;

        // Ensure the screen is a container screen (not pause menu, etc.)
        if (!(event.getScreen() instanceof AbstractContainerScreen)) {
            return;
        }

        // Prevent triggering on player inventory, crafting tables, furnaces, or modded backpacks
        if (LootrHelper.isExcludedScreen(event.getScreen())) {
            return;
        }

        if (lastClickedLootrPos == null || System.currentTimeMillis() - lastClickedLootrTime >= 2000) {
            return; // No recent click
        }

        BlockPos pos = lastClickedLootrPos;
        Integer entityId = lastClickedLootrEntityId;
        lastClickedLootrPos = null; // consume
        lastClickedLootrEntityId = null;
        lastClickedLootrTime = 0;

        // 対象の実体（Block または Entity）が依然として Lootr であることを再検証（誤爆防止）
        boolean verified = false;
        if (entityId != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && entity.isAlive() && LootrHelper.isLootrEntity(entity)) {
                verified = true;
            }
        }
        if (!verified && LootrHelper.isLootrContainerAt(mc.level, pos)) {
            verified = true;
        }

        if (!verified) {
            LOGGER.debug("Quick Loot: Target validation failed at pos {}", pos);
            return;
        }

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

    /** 期限切れのクールダウンエントリを削除する */
    private static void purgeCooldownTracker() {
        long now = System.currentTimeMillis();
        cooldownTracker.entrySet().removeIf(e -> (now - e.getValue()) >= COOLDOWN_MS);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (cachedTargetPos == null) return;

        EquipmentDisplayConfig config = EQUIP_CONFIG;
        if (!config.isQuickLootEnabled()) {
            resetState();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (autoTriggerPending && pendingAutoMode != null) {
            autoTriggerPending = false;
            EquipmentDisplayConfig.QuickLootMode mode = pendingAutoMode;
            pendingAutoMode = null;
            sendQuickLoot(mc, cachedTargetPos, mode);
            if (mode == EquipmentDisplayConfig.QuickLootMode.DROP) {
                mc.setScreen(null);
                resetState();
                return;
            }
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
