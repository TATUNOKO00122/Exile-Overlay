package com.example.exile_overlay.forge.client;

import com.example.exile_overlay.ExampleMod;
import com.example.exile_overlay.client.damage.ThreadSafeDamageRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Phase 2: スレッド安全性を確保したDamageRendererを使用
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DamagePopupForgeHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ThreadSafeDamageRenderer.getInstance().onClientTick();
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        ThreadSafeDamageRenderer.getInstance().onRenderWorld(event.getPoseStack());
    }
}
