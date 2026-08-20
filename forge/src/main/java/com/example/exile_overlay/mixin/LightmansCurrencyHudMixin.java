package com.example.exile_overlay.mixin;

import com.example.exile_overlay.client.config.position.HudPositionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lightman's Currency の WalletDisplayOverlay.render をキャンセルフックする Forge Mixin。
 * Pseudo + remap=false + defaultRequire=0 により、未導入環境でもクラッシュせず安全にフォールバックする。
 * exile_overlay 側でコイン表示が有効な場合、二重描画を防止するため元のオーバーレイ描画をキャンセル。
 */
@Pseudo
@Mixin(targets = "io.github.lightman314.lightmanscurrency.client.gui.overlay.WalletDisplayOverlay", remap = false)
public abstract class LightmansCurrencyHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void exileOverlay$cancelLightmansCurrencyWalletOverlay(CallbackInfo ci) {
        try {
            // exile_overlay 側でコイン表示が有効な場合のみ元の描画をキャンセル
            if (HudPositionManager.getInstance().getPosition("lightmans_currency_coins").isVisible()) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
