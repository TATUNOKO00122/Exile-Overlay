package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.render.exp.ExpAccumulatorEventHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * バニラ Gui クラスへの Mixin。
 * M&S のアクションバー経験値通知を抑制する。
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/GuiMixin");

    @Inject(
        method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void exileOverlay$cancelMnsExpOverlayMessage(Component component, boolean animate, CallbackInfo ci) {
        try {
            if (component == null) return;

            ExpAccumulatorEventHandler.ParsedExpMessage parsed = ExpAccumulatorEventHandler.parseMnsExpMessage(component);
            if (parsed != null) {
                // EXPデータをManagerへ登録
                com.example.exile_overlay.client.render.exp.ExpAccumulatorManager.getInstance().onMnsExpMessageReceived(
                        parsed.gained(),
                        parsed.profComponent(),
                        parsed.percentage()
                );

                if (EquipmentDisplayConfig.getInstance().isCancelMnsExpActionBar()) {
                    ci.cancel();
                    LOGGER.debug("Suppressed M&S action bar EXP message via GuiMixin: {}", component.getString());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to process overlay message cancellation in GuiMixin", e);
        }
    }
}
