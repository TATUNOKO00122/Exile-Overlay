package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class QuickLootKeyRegistration {
    private static final String KEY_CATEGORY = "key.categories." + ExampleMod.MOD_ID;
    public static final KeyMapping QUICK_LOOT_KEY = new KeyMapping(
        "key." + ExampleMod.MOD_ID + ".quick_loot",
        InputConstants.Type.KEYSYM,
        org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL,
        KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(QUICK_LOOT_KEY);
        AutoQuickLootHandler.registerKeyMapping(QUICK_LOOT_KEY);
    }
}