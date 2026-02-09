package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * MODデータプロバイダーのレジストリ
 * 利用可能なデータプロバイダーを管理し、優先度順にデータを提供する
 */
public class ModDataProviderRegistry {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<IModDataProvider> providers = new ArrayList<>();
    private static IModDataProvider activeProvider = null;
    private static boolean initialized = false;
    
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
            initializeDefaults();
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
     */
    public static List<IModDataProvider> getAllProviders() {
        return new ArrayList<>(providers);
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
        if (initialized) return;
        
        // バニラプロバイダーを最低優先度で登録
        register(new VanillaDataProvider());
        
        // Mine and Slashプロバイダーを登録（利用可能な場合のみ有効）
        register(new MineAndSlashDataProvider());
        
        initialized = true;
    }
    
    /**
     * リフレッシュして利用可能なプロバイダーを再評価する
     */
    public static void refresh() {
        updateActiveProvider();
    }
    
    // ========== ヘルパーメソッド ==========
    
    private static IModDataProvider getProviderOrDefault() {
        IModDataProvider provider = getActiveProvider();
        return provider != null ? provider : VanillaDataProvider.INSTANCE;
    }
    
    // 各種データ取得メソッド
    public static float getCurrentHealth(Player player) {
        return getProviderOrDefault().getCurrentHealth(player);
    }
    
    public static float getMaxHealth(Player player) {
        return getProviderOrDefault().getMaxHealth(player);
    }
    
    public static float getCurrentMana(Player player) {
        return getProviderOrDefault().getCurrentMana(player);
    }
    
    public static float getMaxMana(Player player) {
        return getProviderOrDefault().getMaxMana(player);
    }
    
    public static float getCurrentMagicShield(Player player) {
        return getProviderOrDefault().getCurrentMagicShield(player);
    }
    
    public static float getMaxMagicShield(Player player) {
        return getProviderOrDefault().getMaxMagicShield(player);
    }
    
    public static float getCurrentEnergy(Player player) {
        return getProviderOrDefault().getCurrentEnergy(player);
    }
    
    public static float getMaxEnergy(Player player) {
        return getProviderOrDefault().getMaxEnergy(player);
    }
    
    public static float getCurrentBlood(Player player) {
        return getProviderOrDefault().getCurrentBlood(player);
    }
    
    public static float getMaxBlood(Player player) {
        return getProviderOrDefault().getMaxBlood(player);
    }
    
    public static int getLevel(Player player) {
        return getProviderOrDefault().getLevel(player);
    }
    
    public static float getExp(Player player) {
        return getProviderOrDefault().getExp(player);
    }
    
    public static float getExpRequiredForLevelUp(Player player) {
        return getProviderOrDefault().getExpRequiredForLevelUp(player);
    }
    
    public static boolean isBloodMagicActive(Player player) {
        return getProviderOrDefault().isBloodMagicActive(player);
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
