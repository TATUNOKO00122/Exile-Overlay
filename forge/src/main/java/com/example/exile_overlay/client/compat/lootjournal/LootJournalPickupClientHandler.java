package com.example.exile_overlay.client.compat.lootjournal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public final class LootJournalPickupClientHandler {

    private static boolean initialized = false;
    private static MethodHandle pickupItemHandle = null;

    private LootJournalPickupClientHandler() {
    }

    public static void handlePickup(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!ModList.get().isLoaded("loot_journal")) {
            return;
        }

        initHandle();

        if (pickupItemHandle != null) {
            try {
                pickupItemHandle.invoke((AbstractClientPlayer) mc.player, stack);
            } catch (Throwable ignored) {
            }
        }
    }

    private static synchronized void initHandle() {
        if (initialized) return;
        initialized = true;

        try {
            Class<?> helperClass = Class.forName("dev.obscuria.lootjournal.LootJournalHelper");
            Method method = helperClass.getMethod("pickupItem", AbstractClientPlayer.class, ItemStack.class);
            method.setAccessible(true);
            pickupItemHandle = MethodHandles.lookup().unreflect(method);
        } catch (Throwable ignored) {
        }
    }
}
