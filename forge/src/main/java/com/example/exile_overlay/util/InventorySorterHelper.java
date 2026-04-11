package com.example.exile_overlay.util;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class InventorySorterHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static Boolean loaded = null;
    private static boolean initialized = false;
    
    private static Object sortAction = null;
    private static SimpleChannel networkChannel = null;
    private static Method messageMethod = null;
    
    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                Class.forName("cpw.mods.inventorysorter.InventorySorter");
                loaded = true;
                LOGGER.info("Inventory Sorter detected, enabling auto-sort integration.");
            } catch (ClassNotFoundException e) {
                loaded = false;
                LOGGER.debug("Inventory Sorter not found, auto-sort feature hidden.");
            }
        }
        return loaded;
    }
    
    private static void initialize() {
        if (initialized || !isLoaded()) return;
        initialized = true;
        
        try {
            Class<?> actionClass = Class.forName("cpw.mods.inventorysorter.Action");
            Object[] actions = actionClass.getEnumConstants();
            for (Object action : actions) {
                if (action.toString().equals("SORT")) {
                    sortAction = action;
                    break;
                }
            }
            
            if (sortAction == null) {
                LOGGER.error("Failed to find SORT action in Inventory Sorter");
                return;
            }
            
            Class<?> networkClass = Class.forName("cpw.mods.inventorysorter.Network");
            Field channelField = networkClass.getDeclaredField("channel");
            channelField.setAccessible(true);
            networkChannel = (SimpleChannel) channelField.get(null);
            
            messageMethod = actionClass.getMethod("message", Slot.class);
            
            LOGGER.info("Inventory Sorter integration initialized successfully");
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Inventory Sorter classes not found");
        } catch (NoSuchFieldException e) {
            LOGGER.error("Failed to find channel field in Network class", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to access Network channel", e);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Failed to find message method in Action class", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error initializing Inventory Sorter integration", e);
        }
    }
    
    public static boolean isAvailable() {
        if (!isLoaded()) return false;
        initialize();
        return sortAction != null && networkChannel != null && messageMethod != null;
    }
    
    public static void sendSortPacket(Slot slot) {
        if (!isAvailable()) {
            LOGGER.debug("Inventory Sorter not available, cannot send sort packet");
            return;
        }
        
        if (slot == null) {
            LOGGER.debug("Slot is null, cannot send sort packet");
            return;
        }
        
        try {
            Object message = messageMethod.invoke(sortAction, slot);
            networkChannel.sendToServer(message);
            LOGGER.debug("Sent sort packet for slot index {}", slot.index);
        } catch (Exception e) {
            LOGGER.error("Failed to send sort packet", e);
        }
    }
    
    public static void sortCurrentContainer() {
        if (!isAvailable()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen<?> containerScreen) {
            Slot slot = containerScreen.getMenu().getSlot(0);
            if (slot != null) {
                sendSortPacket(slot);
            }
        }
    }
}