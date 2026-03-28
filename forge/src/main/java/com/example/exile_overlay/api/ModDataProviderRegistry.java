package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * MODデータプロバイダーのレジストリ
 * 利用可能なデータプロバイダーを管理し、優先度順にデータを提供する
 * 
 * 【スロットベース設計について】
 * このレジストリは、HUD上の「どのスロットに」データを表示するかを管理します。
 * アクティブなプロバイダーが、各スロットに表示するデータの「意味」を決定します。
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット（デフォルト: HP）
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ（デフォルト: Shield）
 * - ORB_2: 画面右下のメインスロット（デフォルト: Mana/Blood）
 * - ORB_3: 画面左上のサブスロット（デフォルト: Stamina/Energy）
 */
public class ModDataProviderRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<IModDataProvider> providers = new CopyOnWriteArrayList<>();
    private static volatile IModDataProvider activeProvider = null;
    private static volatile boolean initialized = false;
    private static final Object INIT_LOCK = new Object();

    /**
     * データプロバイダーを登録する
     */
    public static void register(IModDataProvider provider) {
        if (provider == null) {
            LOGGER.warn("Attempted to register null provider");
            return;
        }

        providers.add(provider);
        LOGGER.debug("Registered data provider: {} (priority: {})",
                provider.getId(), provider.getPriority());

        // 優先度順にソート
        providers.sort(Comparator.comparingInt(IModDataProvider::getPriority).reversed());

        // アクティブプロバイダーを再評価
        updateActiveProvider();
    }

    /**
     * データプロバイダーの登録を解除する
     */
    public static void unregister(IModDataProvider provider) {
        providers.remove(provider);
        updateActiveProvider();
    }

    /**
     * アクティブなデータプロバイダーを取得する
     * 優先度の高い利用可能なプロバイダーを返す
     */
    public static IModDataProvider getActiveProvider() {
        if (!initialized) {
            synchronized (INIT_LOCK) {
                if (!initialized) {
                    initializeDefaults();
                }
            }
        }
        return activeProvider;
    }

    /**
     * 特定のデータプロバイダーを取得する
     */
    public static IModDataProvider getProvider(String id) {
        for (IModDataProvider provider : providers) {
            if (provider.getId().equals(id)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * 全ての登録済みプロバイダーを取得する
     * CopyOnWriteArrayListを使用しているため、スレッド安全なスナップショットが返される
     */
    public static List<IModDataProvider> getAllProviders() {
        return List.copyOf(providers);
    }

    /**
     * アクティブプロバイダーを更新する
     */
    private static void updateActiveProvider() {
        IModDataProvider previous = activeProvider;
        activeProvider = null;

        for (IModDataProvider provider : providers) {
            if (provider.isAvailable()) {
                activeProvider = provider;
                break;
            }
        }

        if (activeProvider != previous) {
            if (activeProvider != null) {
                LOGGER.info("Active data provider changed to: {} (priority: {})",
                        activeProvider.getId(), activeProvider.getPriority());
            } else {
                LOGGER.info("No active data provider available");
            }
        }
    }

    /**
     * デフォルトのプロバイダーを初期化する
     */
    private static void initializeDefaults() {
        synchronized (INIT_LOCK) {
            if (initialized) return;

            register(new VanillaDataProvider());
            register(new MineAndSlashDataProvider());
            register(new DungeonRealmDataProvider());

            initialized = true;
        }
    }

    /**
     * リフレッシュして利用可能なプロバイダーを再評価する
     */
    public static void refresh() {
        updateActiveProvider();
    }

    // ========== 汎用データ取得ヘルパー ==========

    private static IModDataProvider getProviderOrDefault() {
        IModDataProvider provider = getActiveProvider();
        return provider != null ? provider : VanillaDataProvider.INSTANCE;
    }

    /**
     * 指定されたデータタイプの現在値をアクティブプロバイダーから取得
     */
    public static float getValue(Player player, DataType type) {
        IModDataProvider provider = getProviderOrDefault();
        float value = provider.getValue(player, type);
        if (type == DataType.ORB_1_CURRENT || type == DataType.ORB_1_MAX) {
            LOGGER.debug("ModDataProviderRegistry.getValue({}): provider={}, value={}", type, provider.getId(), value);
        }
        return value;
    }

    /**
     * 指定されたデータタイプの最大値をアクティブプロバイダーから取得
     */
    public static float getMaxValue(Player player, DataType type) {
        IModDataProvider provider = getProviderOrDefault();
        float value = provider.getMaxValue(player, type);
        if (type == DataType.ORB_1_CURRENT || type == DataType.ORB_1_MAX) {
            LOGGER.debug("ModDataProviderRegistry.getMaxValue({}): provider={}, value={}", type, provider.getId(), value);
        }
        return value;
    }

    /**
     * 指定された属性フラグを取得
     */
    public static boolean getAttribute(Player player, String attributeKey) {
        return getProviderOrDefault().getAttribute(player, attributeKey);
    }

    // ========== ModData サポート ==========

    /**
     * アクティブプロバイダーから全データをModDataとして取得
     */
    public static ModData getModData(Player player) {
        IModDataProvider provider = getProviderOrDefault();
        return ModData.fromPlayer(provider, player);
    }

    /**
     * 指定プロバイダーから全データをModDataとして取得
     */
    public static ModData getModData(String providerId, Player player) {
        IModDataProvider provider = getProvider(providerId);
        if (provider == null) {
            provider = VanillaDataProvider.INSTANCE;
        }
        return ModData.fromPlayer(provider, player);
    }
}
