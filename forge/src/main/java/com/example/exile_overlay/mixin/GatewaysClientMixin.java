package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.position.HudPositionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Pseudo
@Mixin(targets = "dev.shadowsoffire.gateways.client.GatewaysClient", remap = false)
public class GatewaysClientMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/GatewaysClientMixin");

    @Inject(method = "bossRenderPre", at = @At("HEAD"), cancellable = true, require = 0)
    private static void exileOverlay$cancelBossRender(CustomizeGuiOverlayEvent.BossEventProgress event, CallbackInfo ci) {
        try {
            if (HudPositionManager.getInstance().getPosition("gateway_boss_bar").isVisible()) {
                String name = event.getBossEvent().getName().getString();
                if (name.startsWith("GATEWAY_ID")) {
                    event.setCanceled(true);
                    ci.cancel();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to cancel gateway boss render: {}", e.getMessage());
        }
    }
}
