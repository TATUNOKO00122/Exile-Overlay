package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobDeathHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/MobDeathHandler");

    public static void register() {
        ExileEvents.MOB_DEATH.register(new EventConsumer<ExileEvents.OnMobDeath>() {
            @Override
            public void accept(ExileEvents.OnMobDeath event) {
                try {
                    if (!(event.mob.level() instanceof ServerLevel serverLevel)) return;

                    var info = EntityInfoComponent.get(event.mob);
                    if (info == null) return;

                    LivingEntity killer = info.getDamageStats().getHighestDamager(serverLevel);
                    if (killer == null) {
                        try {
                            if (event.mob.getLastDamageSource() != null &&
                                    event.mob.getLastDamageSource().getEntity() instanceof LivingEntity le) {
                                killer = le;
                            }
                        } catch (Exception ignored) {}
                    }

                    ServerPlayer player = null;
                    if (killer instanceof ServerPlayer sp) {
                        player = sp;
                    } else if (killer instanceof net.minecraft.world.entity.OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer sp) {
                        player = sp;
                    }

                    if (player == null) return;

                    DamageTrackerManager.MobLastHitRecord record = DamageTrackerManager.consumeMobLastHit(event.mob.getUUID(), player.getUUID());
                    if (record != null) {
                        DamageTrackerManager.recordKill(player.getUUID(), record.skillId(), record.displayName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error recording kill", e);
                }
            }
        });
    }
}
