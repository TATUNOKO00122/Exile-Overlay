package com.example.exile_overlay.mixin;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.example.exile_overlay.dmgtracker.tracking.ServerAilmentTracker;
import com.example.exile_overlay.dmgtracker.util.IDamageEventAccessor;
import com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent;
import com.robertx22.mine_and_slash.uncommon.effectdatas.rework.EventData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.OwnableEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.robertx22.mine_and_slash.uncommon.effectdatas.DamageEvent", remap = false)
public class DamageEventMixin implements IDamageEventAccessor {
    @Unique
    private static final Logger exileOverlay$LOGGER = LoggerFactory.getLogger("exile_overlay/DamageEventMixin");

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
            // 攻撃者がプレイヤー、またはプレイヤーがオーナーの召喚物/傭兵である場合に特定
            ServerPlayer player = null;
            if (event.source instanceof ServerPlayer sp) {
                player = sp;
            } else if (event.source instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer sp) {
                player = sp;
            }

            try {
                if (event.target != null) {
                    String ailmentId = event.data.getString(EventData.AILMENT);
                    if (!ailmentId.isEmpty()) {
                        ServerAilmentTracker.track(player, event.target);
                    }
                }
            } catch (Throwable t) {
                // non-critical
            }

            if (player != null) {
                try {
                    DamageTrackerManager.recordDamage(player, event);
                } catch (Exception e) {
                    exileOverlay$LOGGER.error("Error tracking damage", e);
                }
            }
        }
    }
}
