package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * バニラMinecraft用のデータプロバイダー
 * AbstractModDataProviderを継承し、キャッシュとエラーハンドリングを統一
 * 常に利用可能で、最も低い優先度を持つ
 */
public class VanillaDataProvider extends AbstractModDataProvider {
    
    public static final VanillaDataProvider INSTANCE = new VanillaDataProvider();
    
    private static final int PRIORITY = 0; // 最低優先度
    
    public VanillaDataProvider() {
        // バニラはキャッシュが不要なので0に設定
        setCacheDuration(0);
    }
    
    @Override
    public boolean isAvailable() {
        // バニラは常に利用可能
        return true;
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public String getId() {
        return "vanilla";
    }
    
    @Override
    public float getCurrentHealth(Player player) {
        return fetchVanillaSafely(player, DataType.CURRENT_HEALTH, 
                p -> p != null ? p.getHealth() : 0);
    }
    
    @Override
    public float getMaxHealth(Player player) {
        return fetchVanillaSafely(player, DataType.MAX_HEALTH, 
                p -> p != null ? p.getMaxHealth() : 1);
    }
    
    @Override
    public float getCurrentMana(Player player) {
        // バニラにはマナがない
        return 0;
    }
    
    @Override
    public float getMaxMana(Player player) {
        // バニラにはマナがない
        return 0;
    }
    
    @Override
    public int getLevel(Player player) {
        return fetchVanillaSafely(player, DataType.LEVEL, 
                p -> p != null ? p.experienceLevel : 0);
    }
    
    @Override
    public float getExp(Player player) {
        return fetchVanillaSafely(player, DataType.EXP, 
                p -> p != null ? p.experienceProgress : 0);
    }
    
    @Override
    public float getExpRequiredForLevelUp(Player player) {
        // バニラの場合、経験値バーは常に0-1の範囲
        // 次のレベルに必要な経験値の割合として1.0fを返す
        return 1.0f;
    }
    
    /**
     * バニラプロバイダーからModDataを作成
     */
    public ModData createModData(Player player) {
        return ModData.fromPlayer(this, player);
    }
}
