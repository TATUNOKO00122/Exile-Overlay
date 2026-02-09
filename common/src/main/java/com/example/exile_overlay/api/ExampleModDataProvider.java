package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * 新規MODプロバイダー作成のサンプル実装
 * 
 * このクラスは、新しいMODとの互換性を追加する際の参考実装です。
 * 以下の3つの方法を示しています：
 * 
 * 方法1: AbstractModDataProviderを継承（推奨）
 * 方法2: ModDataProviderBuilderを使用
 * 方法3: IModDataProviderを直接実装
 */
public class ExampleModDataProvider {
    
    // ==================== 方法1: AbstractModDataProviderを継承（推奨） ====================
    
    /**
     * 抽象クラスを継承した実装例
     * キャッシュ管理とエラーハンドリングが自動的に提供される
     */
    public static class MyModProvider extends AbstractModDataProvider {
        
        private static final String MOD_ID = "my_mod";
        private static final int PRIORITY = 150;
        
        // MODロード確認用
        private static Boolean modAvailable = null;
        
        public MyModProvider() {
            // キャッシュ有効期限を設定（ミリ秒）
            setCacheDuration(200);
        }
        
        @Override
        public boolean isAvailable() {
            if (modAvailable == null) {
                // MODがロードされているかチェック
                modAvailable = checkModLoaded(MOD_ID);
                if (modAvailable) {
                    logger.info("MyMod detected and enabled.");
                }
            }
            return modAvailable;
        }
        
        @Override
        public int getPriority() {
            return PRIORITY;
        }
        
        @Override
        public String getId() {
            return MOD_ID;
        }
        
        @Override
        public float getCurrentMana(Player player) {
            // MODのAPIからデータを取得
            return fetchSafely(player, DataType.CURRENT_MANA, p -> {
                // ここで実際のMOD APIを呼び出す
                // 例: return MyModAPI.getMana(p);
                return 100.0f; // ダミー値
            });
        }
        
        @Override
        public float getMaxMana(Player player) {
            return fetchSafely(player, DataType.MAX_MANA, p -> {
                // 例: return MyModAPI.getMaxMana(p);
                return 100.0f; // ダミー値
            });
        }
        
        // 必要に応じて他のメソッドもオーバーライド
        
        private boolean checkModLoaded(String modId) {
            try {
                // Fabric
                Class<?> fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader");
                Object instance = fabricLoader.getMethod("getInstance").invoke(null);
                return (Boolean) fabricLoader.getMethod("isModLoaded", String.class)
                        .invoke(instance, modId);
            } catch (Exception e) {
                try {
                    // Forge
                    Class<?> modList = Class.forName("net.minecraftforge.fml.ModList");
                    Object instance = modList.getMethod("get").invoke(null);
                    return (Boolean) modList.getMethod("isLoaded", String.class)
                            .invoke(instance, modId);
                } catch (Exception ex) {
                    return false;
                }
            }
        }
    }
    
    // ==================== 方法2: ModDataProviderBuilderを使用 ====================
    
    /**
     * ビルダーパターンを使用した実装例
     * 最小限のコードでプロバイダーを作成可能
     */
    public static void registerWithBuilder() {
        ModDataProviderBuilder.create("my_simple_mod")
                .priority(120)
                .checkModLoaded("my_simple_mod")
                .withHealth(player -> {
                    // return MySimpleModAPI.getHealth(player);
                    return player.getHealth();
                })
                .withMana(player -> {
                    // return MySimpleModAPI.getMana(player);
                    return 50.0f;
                })
                .withMaxMana(player -> {
                    // return MySimpleModAPI.getMaxMana(player);
                    return 100.0f;
                })
                .withLevel(player -> {
                    // return MySimpleModAPI.getLevel(player);
                    return player.experienceLevel;
                })
                // カスタムデータも追加可能
                .withCustom("custom_stat", player -> {
                    // return MySimpleModAPI.getCustomStat(player);
                    return 42;
                })
                .register();
    }
    
    /**
     * リフレクションを使用したビルダー実装例
     * MODのソースを直接参照できない場合に便利
     */
    public static void registerWithReflectionBuilder() {
        ModDataProviderBuilder.create("my_reflection_mod")
                .priority(130)
                .checkModLoaded("my_reflection_mod")
                // リフレクションでメソッドを指定
                .withReflectionHealth("com.mymod.PlayerData", "getCurrentHealth")
                .withReflectionMana("com.mymod.PlayerData", "getCurrentMana")
                .withReflectionMaxMana("com.mymod.PlayerData", "getMaxMana")
                .register();
    }
    
    // ==================== 登録方法 ====================
    
    /**
     * MODの初期化時に呼び出す登録メソッド
     * ExampleMod.javaのinit()などから呼び出す
     */
    public static void registerProviders() {
        // 方法1: クラスを直接登録
        ModDataProviderRegistry.register(new MyModProvider());
        
        // 方法2: ビルダーを使用
        registerWithBuilder();
        
        // 方法3: リフレクションビルダーを使用
        registerWithReflectionBuilder();
        
        // 登録後の確認
        System.out.println("Registered providers: " + 
                ModDataProviderRegistry.getAllProviders().size());
    }
    
    // ==================== データ使用例 ====================
    
    /**
     * データを使用する例
     */
    public static void usageExample(Player player) {
        // 方法1: 個別にデータを取得
        float health = ModDataProviderRegistry.getCurrentHealth(player);
        float mana = ModDataProviderRegistry.getCurrentMana(player);
        int level = ModDataProviderRegistry.getLevel(player);
        
        // 方法2: ModDataとして一括取得
        ModData data = ModDataProviderRegistry.getModData(player);
        float health2 = data.getFloat(DataType.CURRENT_HEALTH.getKey());
        float mana2 = data.getFloat(DataType.CURRENT_MANA.getKey());
        
        // 方法3: DataTypeを使用
        float health3 = data.get(DataType.CURRENT_HEALTH, 0.0f);
        int level3 = data.get(DataType.LEVEL, 1);
        
        // カスタムデータの取得
        Object customStat = data.get("custom_stat");
    }
    
    // ==================== カスタムデータタイプの登録 ====================
    
    /**
     * カスタムデータタイプを登録する例
     */
    public static void registerCustomDataTypes() {
        DataTypeRegistry registry = DataTypeRegistry.getInstance();
        
        // 新しいデータタイプを登録
        registry.registerFloat("stamina.current", 100.0f);
        registry.registerFloat("stamina.max", 100.0f);
        registry.registerInt("reputation", 0);
        registry.registerBoolean("pvp_enabled", false);
        registry.registerString("player_class", "warrior");
        
        // 登録したデータタイプはModDataで使用可能
        // data.set("stamina.current", 75.0f);
        // float stamina = data.getFloat("stamina.current");
    }
}
