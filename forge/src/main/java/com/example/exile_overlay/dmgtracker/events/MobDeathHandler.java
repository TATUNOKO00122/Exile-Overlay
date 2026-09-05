package com.example.exile_overlay.dmgtracker.events;

import com.example.exile_overlay.dmgtracker.tracking.DamageTrackerManager;
import com.robertx22.library_of_exile.components.EntityInfoComponent;
import com.robertx22.library_of_exile.events.base.EventConsumer;
import com.robertx22.library_of_exile.events.base.ExileEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
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

                    // 1. キラー（トドメを刺したエンティティまたはその召喚主）を解決
                    LivingEntity killer = event.killer;
                    if (killer == null) {
                        var info = EntityInfoComponent.get(event.mob);
                        if (info != null) {
                            killer = info.getDamageStats().getHighestDamager(serverLevel);
                        }
                    }
                    if (killer == null) {
                        try {
                            if (event.mob.getLastDamageSource() != null &&
                                    event.mob.getLastDamageSource().getEntity() instanceof LivingEntity le) {
                                killer = le;
                            }
                        } catch (Exception ignored) {}
                    }

                    ServerPlayer player = resolveServerPlayer(killer);
                    if (player != null) {
                        // キラーがプレイヤーの場合: そのプレイヤーの直近ヒットスキルを優先してキル記録
                        DamageTrackerManager.MobLastHitRecord record =
                                DamageTrackerManager.consumeMobLastHit(event.mob.getUUID(), player.getUUID());
                        if (record != null) {
                            DamageTrackerManager.recordKill(player.getUUID(), record.skillId(), record.displayName());
                        } else {
                            DamageTrackerManager.recordKill(player.getUUID(), "unknown", "exile_overlay.tracker.unknown");
                        }
                        return;
                    }

                    // 2. キラーが特定できない場合（DoTダメージ死・環境死等）: 直近ダメージ記録のプレイヤーへフォールバック
                    DamageTrackerManager.MobLastHitRecord fallbackRecord =
                            DamageTrackerManager.consumeMobLastHit(event.mob.getUUID(), null);
                    if (fallbackRecord != null) {
                        DamageTrackerManager.recordKill(fallbackRecord.attackerUuid(), fallbackRecord.skillId(), fallbackRecord.displayName());
                    }
                } catch (Exception e) {
                    LOGGER.error("Error recording kill", e);
                }
            }
        });
    }

    private static ServerPlayer resolveServerPlayer(LivingEntity entity) {
        if (entity instanceof ServerPlayer sp) {
            return sp;
        } else if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }
}
