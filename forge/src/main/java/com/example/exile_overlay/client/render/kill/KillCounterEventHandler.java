package com.example.exile_overlay.client.render.kill;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * キルカウンター関連のイベントハンドラー
 * プレイヤーの攻撃検知、エンティティ死亡監視、バニラ統計同期を担当
 */
@OnlyIn(Dist.CLIENT)
public class KillCounterEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/KillCounterEventHandler");

    private static int lastKnownVanillaKills = -1;
    private static int tickCounter = 0;
    private static String lastDimension = null;

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        try {
            if (event.getEntity() == null || !event.getEntity().level().isClientSide()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (event.getEntity().equals(mc.player) && event.getTarget() instanceof LivingEntity) {
                KillCountManager.getInstance().recordPlayerAttack(event.getTarget().getId());
            }
        } catch (Exception e) {
            LOGGER.error("Error processing AttackEntityEvent", e);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }

        if (player.isDeadOrDying() || player.getHealth() <= 0.001f) {
            if (KillCountManager.getInstance().getKillCount() > 0) {
                KillCountManager.getInstance().reset();
            }
            return;
        }

        String currentDim = mc.level.dimension().location().toString();
        if (lastDimension != null && !lastDimension.equals(currentDim)) {
            KillCountManager.getInstance().reset();
            lastKnownVanillaKills = -1;
            LOGGER.debug("Dimension changed from {} to {}, reset kill counter", lastDimension, currentDim);
        }
        lastDimension = currentDim;

        tickCounter++;

        // 5ティック毎に周囲のエンティティ死亡チェックおよびクリーンアップ
        if (tickCounter % 5 == 0) {
            try {
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof LivingEntity living && living != player) {
                        KillCountManager.getInstance().checkEntityDeath(living);
                    }
                }

                // バニラ統計（MOB_KILLS）の同期
                if (player.getStats() != null) {
                    int currentVanillaKills = player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
                    if (lastKnownVanillaKills >= 0 && currentVanillaKills > lastKnownVanillaKills) {
                        int diff = currentVanillaKills - lastKnownVanillaKills;
                        for (int i = 0; i < diff; i++) {
                            KillCountManager.getInstance().incrementKill();
                        }
                    }
                    lastKnownVanillaKills = currentVanillaKills;
                }

                KillCountManager.getInstance().cleanupOldEntries();
            } catch (Exception e) {
                LOGGER.error("Error in KillCounterEventHandler client tick", e);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Player clientPlayer = mc.player;
            if (clientPlayer == null) {
                return;
            }

            LivingEntity victim = event.getEntity();
            if (victim == null || victim.getUUID().equals(clientPlayer.getUUID())) {
                return;
            }

            DamageSource source = event.getSource();
            if (source != null) {
                Entity attacker = source.getEntity();
                Entity directAttacker = source.getDirectEntity();

                boolean isPlayerKill = (attacker != null && attacker.getUUID().equals(clientPlayer.getUUID()))
                        || (directAttacker != null && directAttacker.getUUID().equals(clientPlayer.getUUID()));

                if (isPlayerKill) {
                    KillCountManager.getInstance().recordPlayerAttack(victim.getId());
                    KillCountManager.getInstance().checkEntityDeath(victim);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error processing LivingDeathEvent in KillCounterEventHandler", e);
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        KillCountManager.getInstance().reset();
        lastKnownVanillaKills = -1;
        lastDimension = null;
    }

    @SubscribeEvent
    public static void onClone(net.minecraftforge.client.event.ClientPlayerNetworkEvent.Clone event) {
        KillCountManager.getInstance().reset();
    }
}
