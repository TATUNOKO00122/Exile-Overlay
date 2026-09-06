package com.example.exile_overlay;

import com.example.exile_overlay.client.ClientSetup;
import com.example.exile_overlay.compat.MineAndSlashTrackerCompat;
import com.example.exile_overlay.dmgtracker.network.NetworkHandler;
import com.example.exile_overlay.itemlock.ItemLockServerHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Exile Overlay MOD メインエントリーポイント
 */
@Mod(ExileOverlayMod.MOD_ID)
public final class ExileOverlayMod {
    public static final String MOD_ID = "exile_overlay";

    public ExileOverlayMod() {
        // サーバー・クライアント両方で接続を許可（オプショナルMOD設定）
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> IExtensionPoint.DisplayTest.IGNORE_ALL_VERSION.get());

        // 物理クライアント専用の初期化処理（Dedicated Server環境でのNoClassDefFoundErrorを防止）
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);

        // ネットワークパケット初期化
        NetworkHandler.register(FMLJavaModLoadingContext.get().getModEventBus());

        // アイテムロック機能のサーバーイベント登録
        ItemLockServerHandler.register();

        // サーバー・クライアント共通（連携データ収集・パケット初期化）
        if (ModList.get().isLoaded("mmorpg")) {
            MineAndSlashTrackerCompat.register();
        }
    }
}
