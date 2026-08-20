package com.example.exile_overlay.client.event;

import com.example.exile_overlay.client.damage.DamagePopupManager;
import com.example.exile_overlay.client.render.BossPortalMarkerManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class DamagePopupForgeHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        DamagePopupManager.getInstance().onClientTick();
        BossPortalMarkerManager.getInstance().onClientTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        DamagePopupManager.getInstance().onRenderWorld(event.getPoseStack());
    }
}
