package com.example.exile_overlay;

import com.example.exile_overlay.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;

/**
 * Exile Overlay MOD メインエントリーポイント
 */
@Mod(ExileOverlayMod.MOD_ID)
public final class ExileOverlayMod {
    public static final String MOD_ID = "exile_overlay";

    public ExileOverlayMod() {
        // サーバー・クライアント両方で接続を許可（オプショナルMOD設定）
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> true
                ));

        // 物理クライアント専用の初期化処理（Dedicated Server環境でのNoClassDefFoundErrorを防止）
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);

        // サーバー・クライアント共通（連携データ収集・パケット初期化）
        if (ModList.get().isLoaded("mmorpg")) {
            com.example.exile_overlay.compat.MineAndSlashTrackerCompat.register();
        }
    }
}
