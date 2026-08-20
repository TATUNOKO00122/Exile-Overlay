package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.EquipmentDisplayConfig;
import com.example.exile_overlay.client.config.position.HudPositionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Botania の HUDHandler.renderManaInvBar をキャンセルフックする Forge Mixin。
 * Pseudo + remap=false + defaultRequire=0 により、Botania 未導入環境でもクラッシュせず安全にフォールバックする。
 */
@Pseudo
@Mixin(targets = "vazkii.botania.client.gui.HUDHandler", remap = false)
public abstract class BotaniaHudMixin {

    @Inject(method = "renderManaInvBar", at = @At("HEAD"), cancellable = true, remap = false)
    private static void exileOverlay$cancelBotaniaManaBar(CallbackInfo ci) {
        try {
            // exile_overlay 側で Botania マナバーのキャンセルがON、かつマナバーが表示ONの場合のみ元の描画をキャンセル
            if (EquipmentDisplayConfig.getInstance().isCancelBotaniaMana()
                    && HudPositionManager.getInstance().getPosition("botania_mana_bar").isVisible()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
