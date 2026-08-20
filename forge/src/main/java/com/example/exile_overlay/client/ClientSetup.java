package com.example.exile_overlay.client;

import com.example.exile_overlay.client.damage.DamagePopupConfig;
import com.example.exile_overlay.client.event.ExileOverlayGui;
import com.example.exile_overlay.client.render.exp.ExpAccumulatorEventHandler;
// import com.example.exile_overlay.client.render.kill.KillCounterEventHandler;
import com.example.exile_overlay.client.render.orb.OrbDataProviders;
import com.example.exile_overlay.client.render.orb.OrbRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;

/**
 * 物理クライアント専用の初期化マネージャー
 * Dedicated Server 環境でのクラスロード事故 (NoClassDefFoundError) を防止
 */
@OnlyIn(Dist.CLIENT)
public final class ClientSetup {

    private ClientSetup() {}

    /**
     * クライアントサイドの初期化処理を実行
     */
    public static void init() {
        OrbRegistry.initialize();
        OrbDataProviders.initializeDefaults();
        DamagePopupConfig.getInstance();
        MinecraftForge.EVENT_BUS.register(ExileOverlayGui.class);
        // MinecraftForge.EVENT_BUS.register(KillCounterEventHandler.class);
        MinecraftForge.EVENT_BUS.register(ExpAccumulatorEventHandler.class);
    }
}
