package com.example.exile_overlay.client.render.kill;

/*
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

/ **
 * キルカウンター関連のイベントハンドラー
 *
 * - プレイヤーの敵攻撃検知（AttackEntityEvent）
 * - クライアント側ティックでのエンティティ死亡監視とバニラ統計同期
 * - ForgeのLivingDeathEvent（サーバー・シングルプレイ）の購読
 * - プレイヤー死亡時・ワールド離脱時のリセット
 * /
@OnlyIn(Dist.CLIENT)
public class KillCounterEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("exile_overlay/KillCounterEventHandler");

    private static int lastKnownVanillaKills = -1;
    private static int tickCounter = 0;
    private static String lastDimension = null;

    / **
     * プレイヤーがエンティティを攻撃した瞬間に記録
     * /
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

    / **
     * クライアントティック毎の死亡チェックおよびバニラ統計同期
     * /
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

        String currentDim = mc.level.dimension().location().toString();
        if (lastDimension != null && !lastDimension.equals(currentDim)) {
            KillCountManager.getInstance().reset();
            lastKnownVanillaKills = -1;
            LOGGER.debug("Dimension changed from {} to {}, reset kill counter", lastDimension, currentDim);
        }
        lastDimension = currentDim;

        tickCounter++;

        // 10ティック（0.5秒）毎に周囲のエンティティとクリーンアップを実行
        if (tickCounter % 5 == 0) {
            try {
                // 周囲64ブロックのエンティティの死亡チェック
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity instanceof LivingEntity living && living != player) {
                        KillCountManager.getInstance().checkEntityDeath(living);
                    }
                }

                // バニラ統計（MOB_KILLS）の監視（マルチサーバー等の補正用）
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

    / **
     * サーバー側 / シングルプレイでの LivingDeathEvent
     * /
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Player clientPlayer = mc.player;
            if (clientPlayer == null) {
                return;
            }

            LivingEntity victim = event.getEntity();
            if (victim == null) {
                return;
            }

            // プレイヤー自身の死亡判定
            if (victim.getUUID().equals(clientPlayer.getUUID())) {
                return;
            }

            // 討伐判定: プレイヤー自身によるキルか
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
}
*/
