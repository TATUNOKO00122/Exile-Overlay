package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;

/**
 * 新規MODプロバイダー作成のサンプル実装（スロットベース設計）
 * 
 * このクラスは、新しいMODとの互換性を追加する際の参考実装です。
 * 【重要】スロットベース設計に更新されています。
 * 
 * 以下の3つの方法を示しています：
 * 
 * 方法1: AbstractModDataProviderを継承（推奨）
 * 方法2: ModDataProviderBuilderを使用
 * 方法3: IModDataProviderを直接実装
 * 
 * 【スロット配置ガイド】
 * - ORB_1: 画面左下のメインスロット（デフォルト: HP）
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ（デフォルト: Shield）
 * - ORB_2: 画面右下のメインスロット（デフォルト: Mana/Blood）
 * - ORB_3: 画面左上のサブスロット（デフォルト: Energy/Stamina）
 */
public class ExampleModDataProvider {

    // ==================== 方法1: AbstractModDataProviderを継承（推奨） ====================

    /**
     * 抽象クラスを継承した実装例
     * キャッシュ管理とエラーハンドリングが自動的に提供される
     * 
     * 【スロットマッピング例】
     * - ORB_1: Health（体力）
     * - ORB_2: Mana（マナ）
     * - ORB_3: Energy（エネルギー）
     * - ORB_1_OVERLAY: Shield（シールド）
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

        // ========== ORB 2: 右下メインスロット（マナ）==========
        @Override
        public float getValue(Player player, DataType type) {
            if (type == DataType.ORB_2_CURRENT) {
                return fetchSafely(player, type, p -> 100.0f); // ダミー値
            }
            return super.getValue(player, type);
        }

        @Override
        public float getMaxValue(Player player, DataType type) {
            if (type == DataType.ORB_2_MAX) {
                return fetchSafely(player, type, p -> 100.0f); // ダミー値
            }
            return super.getMaxValue(player, type);
        }

        // 必要に応じて他のスロットメソッドもオーバーライド
        // getOrb1Current(), getOrb3Current(), getOrb1OverlayCurrent() など

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
     * 
     * 【スロットマッピング例】
     * - ORB_1: Health（体力）
     * - ORB_2: Mana（マナ）
     * - ORB_3: Energy（エネルギー）
     */
    public static void registerWithBuilder() {
        ModDataProviderBuilder.create("my_simple_mod")
                .priority(120)
                .checkModLoaded("my_simple_mod")
                .withOrb1(player -> {
                    // return MySimpleModAPI.getHealth(player);
                    return player.getHealth();
                })
                .withOrb2(player -> {
                    // return MySimpleModAPI.getMana(player);
                    return 50.0f;
                })
                .withOrb2Max(player -> {
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
                .withReflectionOrb1("com.mymod.PlayerData", "getCurrentHealth")
                .withReflectionOrb2("com.mymod.PlayerData", "getCurrentMana")
                .withReflectionOrb2Max("com.mymod.PlayerData", "getMaxMana")
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
     * データを使用する例（スロットベース）
     */
    public static void usageExample(Player player) {
        // 方法1: 個別にデータを取得（汎用）
        float orb1Value = ModDataProviderRegistry.getValue(player, DataType.ORB_1_CURRENT);
        float orb2Value = ModDataProviderRegistry.getValue(player, DataType.ORB_2_CURRENT);
        float orb3Value = ModDataProviderRegistry.getValue(player, DataType.ORB_3_CURRENT);
        float overlayValue = ModDataProviderRegistry.getValue(player, DataType.ORB_1_OVERLAY_CURRENT);
        int level = (int) ModDataProviderRegistry.getValue(player, DataType.LEVEL);

        // 方法2: ModDataとして一括取得
        ModData data = ModDataProviderRegistry.getModData(player);
        float orb1Value2 = data.getFloat(DataType.ORB_1_CURRENT.getKey());
        float orb2Value2 = data.getFloat(DataType.ORB_2_CURRENT.getKey());

        // 方法3: DataTypeを使用
        float orb1Value3 = data.get(DataType.ORB_1_CURRENT, 0.0f);
        float orb2Value3 = data.get(DataType.ORB_2_CURRENT, 0.0f);
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
        registry.registerFloat("custom_stamina.current", 100.0f);
        registry.registerFloat("custom_stamina.max", 100.0f);
        registry.registerInt("reputation", 0);
        registry.registerBoolean("pvp_enabled", false);
        registry.registerString("player_class", "warrior");

        // 登録したデータタイプはModDataで使用可能
        // data.set("custom_stamina.current", 75.0f);
        // float stamina = data.getFloat("custom_stamina.current");
    }
}
