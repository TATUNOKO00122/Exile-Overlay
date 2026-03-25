package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * MODデータプロバイダーを簡単に作成するためのビルダークラス
 * 
 * 【スロットベース設計】
 * このビルダーは、HUD上の「どのスロットに」データを供給するかを定義します。
 * 各スロットの「意味」（HP、Manaなど）は各MODが決定します。
 * 
 * 【安全性機能】
 * - Reflection検証: 初期化時にクラスとメソッドを検証
 * - ホワイトリスト: 許可されたパッケージのみリフレクション可能
 * - MethodHandleキャッシュ: 検証済みハンドルを再利用
 * 
 * スロット配置:
 * - ORB_1: 画面左下のメインスロット
 * - ORB_1_OVERLAY: ORB_1に重なるオーバーレイ
 * - ORB_2: 画面右下のメインスロット
 * - ORB_3: 画面左上のサブスロット
 * 
 * 使用例:
 * 
 * 方法1: ラムダ式を使用（推奨）
 * ModDataProviderBuilder.create("my_mod")
 * .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 * .withOrb1(player -> MyModAPI.getHealth(player)) // ORB_1にHealthを表示
 * .withOrb2(player -> MyModAPI.getMana(player)) // ORB_2にManaを表示
 * .register();
 * 
 * 方法2: リフレクションを使用（ホワイトリスト対象のみ）
 * ModDataProviderBuilder.create("my_mod")
 * .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 * .withReflectionOrb1("com.mymod.API", "getPlayerHealth")
 * .withReflectionOrb2("com.mymod.API", "getPlayerMana")
 * .register();
 * 
 * 方法3: 完全にカスタマイズ
 * ModDataProviderBuilder.create("my_mod")
 * .priority(150)
 * .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 * .withOrb3(player -> MyModAPI.getStamina(player)) // ORB_3にStaminaを表示
 * .withCustom("custom_data", player -> MyModAPI.getCustomData(player))
 * .register();
 */
public class ModDataProviderBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final String providerId;
    private int priority = 100;
    private Supplier<Boolean> availabilityCheck;

    // スロット別データ取得関数
    private Function<Player, Float> orb1Func;
    private Function<Player, Float> orb1MaxFunc;
    private Function<Player, Float> orb1OverlayFunc;
    private Function<Player, Float> orb1OverlayMaxFunc;
    private Function<Player, Float> orb2Func;
    private Function<Player, Float> orb2MaxFunc;
    private Function<Player, Boolean> orb2BloodModeFunc;
    private Function<Player, Float> orb3Func;
    private Function<Player, Float> orb3MaxFunc;
    private Function<Player, Float> orb4Func;
    private Function<Player, Float> orb4MaxFunc;

    // その他のデータ
    private Function<Player, Integer> levelFunc;
    private Function<Player, Float> expFunc;
    private Function<Player, Float> expRequiredFunc;

    // カスタムデータ
    private final Map<String, Function<Player, Object>> customData = new HashMap<>();

    // リフレクション検証器
    private final ReflectionValidator reflectionValidator = new ReflectionValidator();

    private ModDataProviderBuilder(String providerId) {
        this.providerId = providerId;
    }

    /**
     * ビルダーを作成
     */
    public static ModDataProviderBuilder create(String providerId) {
        return new ModDataProviderBuilder(providerId);
    }

    /**
     * 優先度を設定
     */
    public ModDataProviderBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 利用可能性チェックを設定
     */
    public ModDataProviderBuilder checkAvailable(Supplier<Boolean> check) {
        this.availabilityCheck = check;
        return this;
    }

    /**
     * MOD IDで利用可能性をチェック（Fabric/Forge共通）
     */
    public ModDataProviderBuilder checkModLoaded(String modId) {
        this.availabilityCheck = () -> {
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
        };
        return this;
    }

    // ========== ORB 1: 左下メインスロット ==========

    /**
     * ORB_1現在値取得関数を設定
     */
    public ModDataProviderBuilder withOrb1(Function<Player, Float> func) {
        this.orb1Func = func;
        return this;
    }

    /**
     * ORB_1最大値取得関数を設定
     */
    public ModDataProviderBuilder withOrb1Max(Function<Player, Float> func) {
        this.orb1MaxFunc = func;
        return this;
    }

    // ========== ORB 1 OVERLAY ==========

    /**
     * ORB_1_OVERLAY現在値取得関数を設定
     */
    public ModDataProviderBuilder withOrb1Overlay(Function<Player, Float> func) {
        this.orb1OverlayFunc = func;
        return this;
    }

    /**
     * ORB_1_OVERLAY最大値取得関数を設定
     */
    public ModDataProviderBuilder withOrb1OverlayMax(Function<Player, Float> func) {
        this.orb1OverlayMaxFunc = func;
        return this;
    }

    // ========== ORB 2: 右下メインスロット ==========

    /**
     * ORB_2現在値取得関数を設定
     */
    public ModDataProviderBuilder withOrb2(Function<Player, Float> func) {
        this.orb2Func = func;
        return this;
    }

    /**
     * ORB_2最大値取得関数を設定
     */
    public ModDataProviderBuilder withOrb2Max(Function<Player, Float> func) {
        this.orb2MaxFunc = func;
        return this;
    }

    /**
     * ORB_2 Bloodモード判定関数を設定
     */
    public ModDataProviderBuilder withOrb2BloodMode(Function<Player, Boolean> func) {
        this.orb2BloodModeFunc = func;
        return this;
    }

    // ========== ORB 3: 左上サブスロット ==========

    /**
     * ORB_3現在値取得関数を設定
     */
    public ModDataProviderBuilder withOrb3(Function<Player, Float> func) {
        this.orb3Func = func;
        return this;
    }

    /**
     * ORB_3最大値取得関数を設定
     */
    public ModDataProviderBuilder withOrb3Max(Function<Player, Float> func) {
        this.orb3MaxFunc = func;
        return this;
    }

    // ========== ORB 4: 拡張用 ==========

    /**
     * ORB_4現在値取得関数を設定
     */
    public ModDataProviderBuilder withOrb4(Function<Player, Float> func) {
        this.orb4Func = func;
        return this;
    }

    /**
     * ORB_4最大値取得関数を設定
     */
    public ModDataProviderBuilder withOrb4Max(Function<Player, Float> func) {
        this.orb4MaxFunc = func;
        return this;
    }

    // ========== 経験値・レベル ==========

    /**
     * レベル取得関数を設定
     */
    public ModDataProviderBuilder withLevel(Function<Player, Integer> func) {
        this.levelFunc = func;
        return this;
    }

    /**
     * 経験値取得関数を設定
     */
    public ModDataProviderBuilder withExp(Function<Player, Float> func) {
        this.expFunc = func;
        return this;
    }

    /**
     * 必要経験値取得関数を設定
     */
    public ModDataProviderBuilder withExpRequired(Function<Player, Float> func) {
        this.expRequiredFunc = func;
        return this;
    }

    // ========== カスタムデータ ==========

    /**
     * カスタムデータ取得関数を設定
     */
    public ModDataProviderBuilder withCustom(String key, Function<Player, Object> func) {
        this.customData.put(key, func);
        return this;
    }

    // ========== リフレクション設定 ==========

    /**
     * リフレクションでORB_1現在値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb1(String className, String methodName) {
        return withOrb1(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでORB_1最大値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb1Max(String className, String methodName) {
        return withOrb1Max(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでORB_2現在値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb2(String className, String methodName) {
        return withOrb2(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでORB_2最大値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb2Max(String className, String methodName) {
        return withOrb2Max(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでORB_3現在値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb3(String className, String methodName) {
        return withOrb3(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでORB_3最大値取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionOrb3Max(String className, String methodName) {
        return withOrb3Max(createReflectionFloatFunc(className, methodName));
    }

    /**
     * リフレクションでFloat値を取得する関数を作成
     * 【安全性向上】初期化時に検証し、MethodHandleをキャッシュ
     */
    private Function<Player, Float> createReflectionFloatFunc(String className, String methodName) {
        // 初期化時に検証
        ReflectionValidator.ValidationResult result = reflectionValidator.validateFloatMethod(className, methodName);

        if (!result.isValid()) {
            LOGGER.error("Reflection validation failed for {}.{}: {}",
                    className, methodName, result.getErrorMessage());
            // 安全なフォールバック: 常に0を返す
            return player -> 0.0f;
        }

        // 検証済みのMethodHandleをキャッシュして使用
        MethodHandle cachedHandle = result.getMethodHandle();
        return player -> {
            try {
                return (float) cachedHandle.invoke(player);
            } catch (Throwable e) {
                LOGGER.debug("Reflection invocation error for {}.{}: {}",
                        className, methodName, e.getMessage());
                return 0.0f;
            }
        };
    }

    // ========== ビルド・登録 ==========

    /**
     * プロバイダーを構築してレジストリに登録
     */
    public IModDataProvider register() {
        if (availabilityCheck == null) {
            LOGGER.error("Availability check not set for provider: {}", providerId);
            availabilityCheck = () -> false;
        }

        IModDataProvider provider = new BuiltModDataProvider(
                providerId, priority, availabilityCheck,
                orb1Func, orb1MaxFunc, orb1OverlayFunc, orb1OverlayMaxFunc,
                orb2Func, orb2MaxFunc, orb2BloodModeFunc,
                orb3Func, orb3MaxFunc, orb4Func, orb4MaxFunc,
                levelFunc, expFunc, expRequiredFunc, customData);

        ModDataProviderRegistry.register(provider);
        LOGGER.info("Registered data provider '{}' via builder (priority: {})",
                providerId, priority);

        return provider;
    }

    /**
     * ビルドされたプロバイダークラス（スロットベース）
     */
    private static class BuiltModDataProvider extends AbstractModDataProvider {

        private final String id;
        private final int priority;
        private final Supplier<Boolean> availabilityCheck;

        // スロット別データ取得関数
        private final Function<Player, Float> orb1Func;
        private final Function<Player, Float> orb1MaxFunc;
        private final Function<Player, Float> orb1OverlayFunc;
        private final Function<Player, Float> orb1OverlayMaxFunc;
        private final Function<Player, Float> orb2Func;
        private final Function<Player, Float> orb2MaxFunc;
        private final Function<Player, Boolean> orb2BloodModeFunc;
        private final Function<Player, Float> orb3Func;
        private final Function<Player, Float> orb3MaxFunc;
        private final Function<Player, Float> orb4Func;
        private final Function<Player, Float> orb4MaxFunc;

        // その他のデータ
        private final Function<Player, Integer> levelFunc;
        private final Function<Player, Float> expFunc;
        private final Function<Player, Float> expRequiredFunc;

        // カスタムデータ
        private final Map<String, Function<Player, Object>> customData;

        BuiltModDataProvider(
                String id, int priority, Supplier<Boolean> availabilityCheck,
                Function<Player, Float> orb1Func, Function<Player, Float> orb1MaxFunc,
                Function<Player, Float> orb1OverlayFunc, Function<Player, Float> orb1OverlayMaxFunc,
                Function<Player, Float> orb2Func, Function<Player, Float> orb2MaxFunc,
                Function<Player, Boolean> orb2BloodModeFunc,
                Function<Player, Float> orb3Func, Function<Player, Float> orb3MaxFunc,
                Function<Player, Float> orb4Func, Function<Player, Float> orb4MaxFunc,
                Function<Player, Integer> levelFunc, Function<Player, Float> expFunc,
                Function<Player, Float> expRequiredFunc,
                Map<String, Function<Player, Object>> customData) {
            this.id = id;
            this.priority = priority;
            this.availabilityCheck = availabilityCheck;
            this.orb1Func = orb1Func;
            this.orb1MaxFunc = orb1MaxFunc;
            this.orb1OverlayFunc = orb1OverlayFunc;
            this.orb1OverlayMaxFunc = orb1OverlayMaxFunc;
            this.orb2Func = orb2Func;
            this.orb2MaxFunc = orb2MaxFunc;
            this.orb2BloodModeFunc = orb2BloodModeFunc;
            this.orb3Func = orb3Func;
            this.orb3MaxFunc = orb3MaxFunc;
            this.orb4Func = orb4Func;
            this.orb4MaxFunc = orb4MaxFunc;
            this.levelFunc = levelFunc;
            this.expFunc = expFunc;
            this.expRequiredFunc = expRequiredFunc;
            this.customData = new HashMap<>(customData);
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean isAvailable() {
            return availabilityCheck.get();
        }

        @Override
        public float getValue(Player player, DataType type) {
            return switch (type) {
                case ORB_1_CURRENT -> orb1Func != null ? orb1Func.apply(player) : super.getValue(player, type);
                case ORB_1_OVERLAY_CURRENT ->
                    orb1OverlayFunc != null ? orb1OverlayFunc.apply(player) : super.getValue(player, type);
                case ORB_2_CURRENT -> orb2Func != null ? orb2Func.apply(player) : super.getValue(player, type);
                case ORB_3_CURRENT -> orb3Func != null ? orb3Func.apply(player) : super.getValue(player, type);
                case ORB_4_CURRENT -> orb4Func != null ? orb4Func.apply(player) : super.getValue(player, type);
                case LEVEL -> levelFunc != null ? (float) levelFunc.apply(player) : super.getValue(player, type);
                case EXP -> expFunc != null ? expFunc.apply(player) : super.getValue(player, type);
                default -> super.getValue(player, type);
            };
        }

        @Override
        public float getMaxValue(Player player, DataType type) {
            return switch (type) {
                case ORB_1_MAX -> orb1MaxFunc != null ? orb1MaxFunc.apply(player) : super.getMaxValue(player, type);
                case ORB_1_OVERLAY_MAX ->
                    orb1OverlayMaxFunc != null ? orb1OverlayMaxFunc.apply(player) : super.getMaxValue(player, type);
                case ORB_2_MAX -> orb2MaxFunc != null ? orb2MaxFunc.apply(player) : super.getMaxValue(player, type);
                case ORB_3_MAX -> orb3MaxFunc != null ? orb3MaxFunc.apply(player) : super.getMaxValue(player, type);
                case ORB_4_MAX -> orb4MaxFunc != null ? orb4MaxFunc.apply(player) : super.getMaxValue(player, type);
                case EXP_REQUIRED ->
                    expRequiredFunc != null ? expRequiredFunc.apply(player) : super.getMaxValue(player, type);
                default -> super.getMaxValue(player, type);
            };
        }

        @Override
        public boolean getAttribute(Player player, String attributeKey) {
            if (DataType.ORB_2_IS_BLOOD.getKey().equals(attributeKey) && orb2BloodModeFunc != null) {
                return orb2BloodModeFunc.apply(player);
            }
            return super.getAttribute(player, attributeKey);
        }

        /**
         * カスタムデータを取得
         */
        public Object getCustomData(String key, Player player) {
            Function<Player, Object> func = customData.get(key);
            if (func != null) {
                try {
                    return func.apply(player);
                } catch (Exception e) {
                    logger.debug("Error getting custom data '{}' for player {}: {}",
                            key, player.getName().getString(), e.getMessage());
                }
            }
            return null;
        }
    }
}
