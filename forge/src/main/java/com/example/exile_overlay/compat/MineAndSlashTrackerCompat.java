package com.example.exile_overlay.compat;


import com.example.exile_overlay.dmgtracker.events.DamageEventHandler;
import com.example.exile_overlay.dmgtracker.events.MobDeathHandler;
import com.example.exile_overlay.dmgtracker.events.PlayerLoginHandler;
import com.example.exile_overlay.dmgtracker.events.ServerTickSyncHandler;
import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Mine and Slash とのダメージトラッカー関連の互換ブリッジクラス。
 * mine_and_slash MODが有効な場合にのみ呼び出すこと。
 */
public class MineAndSlashTrackerCompat {

    /**
     * トラッカー関連のネットワークハンドラーとイベントハンドラーを登録する
     */
    public static void register() {
        NetworkHandler.register(FMLJavaModLoadingContext.get().getModEventBus());
        DamageEventHandler.register();
        MobDeathHandler.register();
        MinecraftForge.EVENT_BUS.register(new PlayerLoginHandler());
        MinecraftForge.EVENT_BUS.register(new ServerTickSyncHandler());
    }

}
