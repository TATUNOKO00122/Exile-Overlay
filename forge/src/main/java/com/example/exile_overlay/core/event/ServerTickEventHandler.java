package com.example.exile_overlay.core.event;

import com.example.exile_overlay.util.BackpackPickupHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class ServerTickEventHandler {

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            BackpackPickupHelper.processTick();
        }
    }

    /**
     * ワールドアンロード時にそのレベルのエンティティ参照をクリアする。
     * メモリリークを防ぐため、ServerLevel に属するエントリのみを削除する。
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            BackpackPickupHelper.clearForLevel(serverLevel);
        }
    }
}