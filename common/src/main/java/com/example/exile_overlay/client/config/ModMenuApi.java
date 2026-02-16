package com.example.exile_overlay.client.config;

import com.example.exile_overlay.client.config.position.HudPositionManager;
import com.example.exile_overlay.client.config.screen.HudListScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * ModMenu連携用のAPI
 *
 * Fabric/Forge双方で使用可能な設定画面ファクトリ
 */
public final class ModMenuApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModMenuApi.class);
    private static Function<Screen, Screen> configScreenFactory = HudListScreen::new;
    
    private ModMenuApi() {
        // ユーティリティクラス
    }
    
    /**
     * 設定画面を作成
     * 
     * @param parent 親画面
     * @return 設定画面
     */
    public static Screen createConfigScreen(Screen parent) {
        // 初期化を確実に行う
        HudPositionManager.getInstance().initialize();
        return configScreenFactory.apply(parent);
    }
    
    /**
     * 設定画面ファクトリを設定
     * 
     * @param factory カスタムファクトリ
     */
    public static void setConfigScreenFactory(Function<Screen, Screen> factory) {
        configScreenFactory = factory;
    }
    
    /**
     * 設定画面を直接開く
     */
    public static void openConfigScreen() {
        LOGGER.info("Opening HUD config screen");
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            LOGGER.error("Minecraft instance is null");
            return;
        }
        mc.execute(() -> {
            LOGGER.info("Creating config screen, current screen: {}", mc.screen != null ? mc.screen.getClass().getName() : "null");
            Screen configScreen = createConfigScreen(mc.screen);
            if (configScreen == null) {
                LOGGER.error("Failed to create config screen");
                return;
            }
            mc.setScreen(configScreen);
            LOGGER.info("Config screen opened successfully");
        });
    }
}
