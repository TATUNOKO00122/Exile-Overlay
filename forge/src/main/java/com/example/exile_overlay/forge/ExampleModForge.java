package com.example.exile_overlay.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

import com.example.exile_overlay.ExampleMod;

@Mod(ExampleMod.MOD_ID)
public final class ExampleModForge {
    public ExampleModForge() {
        ExampleMod.init();
        MinecraftForge.EVENT_BUS.register(com.example.exile_overlay.forge.client.ExileOverlayGui.class);
    }
}
