package com.example.exile_overlay;

import com.example.exile_overlay.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Exile Overlay MOD メインエントリーポイント
 */
@Mod(ExileOverlayMod.MOD_ID)
public final class ExileOverlayMod {
    public static final String MOD_ID = "exile_overlay";

    public ExileOverlayMod() {
        // 物理クライアント専用の初期化処理
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init();
        }

        // サーバー・クライアント共通（連携データ収集・パケット初期化）
        if (ModList.get().isLoaded("mmorpg")) {
            com.example.exile_overlay.compat.MineAndSlashTrackerCompat.register();
        }
    }
}
