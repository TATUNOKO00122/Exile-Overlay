package com.example.exile_overlay.mixin;

import com.example.exile_overlay.dmgtracker.util.IDamageEventAccessor;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import net.minecraft.server.level.ServerPlayer;

@Pseudo
@Mixin(targets = "com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent", remap = false)
public class DamageEventMixin implements IDamageEventAccessor {
    @Unique
    private DamageEvent.DmgByElement exileOverlay$dmgByElement;

    @Inject(method = "calculateAllBonusElementalDamage", at = @At("RETURN"))
    private void exileOverlay$onCalculateAllBonusElementalDamage(CallbackInfoReturnable<DamageEvent.DmgByElement> cir) {
        this.exileOverlay$dmgByElement = cir.getReturnValue();
    }

    @Override
    public DamageEvent.DmgByElement exileOverlay$getDmgByElement() {
        return this.exileOverlay$dmgByElement;
    }

    @Override
    public void exileOverlay$setDmgByElement(DamageEvent.DmgByElement info) {
        this.exileOverlay$dmgByElement = info;
    }

    @Inject(method = "activate", at = @At("RETURN"))
    private void exileOverlay$onActivateReturn(CallbackInfo ci) {
        DamageEvent event = (DamageEvent) (Object) this;
        if (!event.data.isCanceled()) {
            try {
                if (event.target != null) {
                    String ailmentId = event.data.getString(com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData.AILMENT);
                    if ("bleed".equalsIgnoreCase(ailmentId)) {
                        com.example.exile_overlay.client.render.ailment.ClientAilmentTracker.getInstance()
                                .recordBleedDamage(event.target, event.data.getNumber());
                    } else if ("poison".equalsIgnoreCase(ailmentId)) {
                        com.example.exile_overlay.client.render.ailment.ClientAilmentTracker.getInstance()
                                .recordPoisonDamage(event.target);
                    }
                }
            } catch (Throwable t) {
                // non-critical
            }

            // 攻撃者がプレイヤーである場合のみダメージトラッカーに記録
            if (event.source instanceof ServerPlayer player) {
                try {
                    DamageTrackerManager.recordDamage(player, event);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger("exile_overlay/DamageEventMixin").error("Error tracking damage", e);
                }
            }
        }
    }
}
