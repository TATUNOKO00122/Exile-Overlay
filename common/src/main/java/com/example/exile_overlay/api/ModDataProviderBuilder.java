package com.example.exile_overlay.api;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * MODデータプロバイダーを簡単に作成するためのビルダークラス
 * メソッドチェーンで設定し、最小限のコードでプロバイダーを作成可能
 * 
 * 使用例:
 * 
 * 方法1: ラムダ式を使用
 * ModDataProviderBuilder.create("my_mod")
 *     .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 *     .withHealth(player -> MyModAPI.getHealth(player))
 *     .withMana(player -> MyModAPI.getMana(player))
 *     .register();
 * 
 * 方法2: リフレクションを使用
 * ModDataProviderBuilder.create("my_mod")
 *     .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 *     .withReflectionHealth("com.mymod.API", "getPlayerHealth")
 *     .withReflectionMana("com.mymod.API", "getPlayerMana")
 *     .register();
 * 
 * 方法3: 完全にカスタマイズ
 * ModDataProviderBuilder.create("my_mod")
 *     .priority(150)
 *     .checkAvailable(() -> ModList.get().isLoaded("my_mod"))
 *     .withCustom("custom_data", player -> MyModAPI.getCustomData(player))
 *     .register();
 */
public class ModDataProviderBuilder {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final String providerId;
    private int priority = 100;
    private Supplier<Boolean> availabilityCheck;
    
    // データ取得関数
    private Function<Player, Float> healthFunc;
    private Function<Player, Float> maxHealthFunc;
    private Function<Player, Float> manaFunc;
    private Function<Player, Float> maxManaFunc;
    private Function<Player, Float> magicShieldFunc;
    private Function<Player, Float> maxMagicShieldFunc;
    private Function<Player, Float> energyFunc;
    private Function<Player, Float> maxEnergyFunc;
    private Function<Player, Float> bloodFunc;
    private Function<Player, Float> maxBloodFunc;
    private Function<Player, Integer> levelFunc;
    private Function<Player, Float> expFunc;
    private Function<Player, Float> expRequiredFunc;
    private Function<Player, Boolean> bloodMagicFunc;
    
    // カスタムデータ
    private final Map<String, Function<Player, Object>> customData = new HashMap<>();
    
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
    
    // ========== 基本ステータス ==========
    
    /**
     * HP取得関数を設定
     */
    public ModDataProviderBuilder withHealth(Function<Player, Float> func) {
        this.healthFunc = func;
        return this;
    }
    
    /**
     * 最大HP取得関数を設定
     */
    public ModDataProviderBuilder withMaxHealth(Function<Player, Float> func) {
        this.maxHealthFunc = func;
        return this;
    }
    
    /**
     * マナ取得関数を設定
     */
    public ModDataProviderBuilder withMana(Function<Player, Float> func) {
        this.manaFunc = func;
        return this;
    }
    
    /**
     * 最大マナ取得関数を設定
     */
    public ModDataProviderBuilder withMaxMana(Function<Player, Float> func) {
        this.maxManaFunc = func;
        return this;
    }
    
    // ========== 拡張ステータス ==========
    
    /**
     * 魔法シールド取得関数を設定
     */
    public ModDataProviderBuilder withMagicShield(Function<Player, Float> func) {
        this.magicShieldFunc = func;
        return this;
    }
    
    /**
     * 最大魔法シールド取得関数を設定
     */
    public ModDataProviderBuilder withMaxMagicShield(Function<Player, Float> func) {
        this.maxMagicShieldFunc = func;
        return this;
    }
    
    /**
     * エネルギー取得関数を設定
     */
    public ModDataProviderBuilder withEnergy(Function<Player, Float> func) {
        this.energyFunc = func;
        return this;
    }
    
    /**
     * 最大エネルギー取得関数を設定
     */
    public ModDataProviderBuilder withMaxEnergy(Function<Player, Float> func) {
        this.maxEnergyFunc = func;
        return this;
    }
    
    /**
     * ブラッド取得関数を設定
     */
    public ModDataProviderBuilder withBlood(Function<Player, Float> func) {
        this.bloodFunc = func;
        return this;
    }
    
    /**
     * 最大ブラッド取得関数を設定
     */
    public ModDataProviderBuilder withMaxBlood(Function<Player, Float> func) {
        this.maxBloodFunc = func;
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
    
    /**
     * 血魔法アクティブ判定関数を設定
     */
    public ModDataProviderBuilder withBloodMagicCheck(Function<Player, Boolean> func) {
        this.bloodMagicFunc = func;
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
     * リフレクションでHP取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionHealth(String className, String methodName) {
        return withHealth(createReflectionFloatFunc(className, methodName));
    }
    
    /**
     * リフレクションで最大HP取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionMaxHealth(String className, String methodName) {
        return withMaxHealth(createReflectionFloatFunc(className, methodName));
    }
    
    /**
     * リフレクションでマナ取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionMana(String className, String methodName) {
        return withMana(createReflectionFloatFunc(className, methodName));
    }
    
    /**
     * リフレクションで最大マナ取得メソッドを設定
     */
    public ModDataProviderBuilder withReflectionMaxMana(String className, String methodName) {
        return withMaxMana(createReflectionFloatFunc(className, methodName));
    }
    
    /**
     * リフレクションでFloat値を取得する関数を作成
     */
    private Function<Player, Float> createReflectionFloatFunc(String className, String methodName) {
        return player -> {
            try {
                Class<?> clazz = Class.forName(className);
                MethodHandle handle = MethodHandles.publicLookup()
                        .findStatic(clazz, methodName, 
                                MethodType.methodType(float.class, Player.class));
                return (float) handle.invoke(player);
            } catch (Throwable e) {
                LOGGER.debug("Reflection error for {}.{}: {}", className, methodName, e.getMessage());
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
                healthFunc, maxHealthFunc, manaFunc, maxManaFunc,
                magicShieldFunc, maxMagicShieldFunc, energyFunc, maxEnergyFunc,
                bloodFunc, maxBloodFunc, levelFunc, expFunc, expRequiredFunc,
                bloodMagicFunc, customData
        );
        
        ModDataProviderRegistry.register(provider);
        LOGGER.info("Registered data provider '{}' via builder (priority: {})", 
                providerId, priority);
        
        return provider;
    }
    
    /**
     * ビルドされたプロバイダークラス
     */
    private static class BuiltModDataProvider extends AbstractModDataProvider {
        
        private final String id;
        private final int priority;
        private final Supplier<Boolean> availabilityCheck;
        
        private final Function<Player, Float> healthFunc;
        private final Function<Player, Float> maxHealthFunc;
        private final Function<Player, Float> manaFunc;
        private final Function<Player, Float> maxManaFunc;
        private final Function<Player, Float> magicShieldFunc;
        private final Function<Player, Float> maxMagicShieldFunc;
        private final Function<Player, Float> energyFunc;
        private final Function<Player, Float> maxEnergyFunc;
        private final Function<Player, Float> bloodFunc;
        private final Function<Player, Float> maxBloodFunc;
        private final Function<Player, Integer> levelFunc;
        private final Function<Player, Float> expFunc;
        private final Function<Player, Float> expRequiredFunc;
        private final Function<Player, Boolean> bloodMagicFunc;
        private final Map<String, Function<Player, Object>> customData;
        
        BuiltModDataProvider(
                String id, int priority, Supplier<Boolean> availabilityCheck,
                Function<Player, Float> healthFunc, Function<Player, Float> maxHealthFunc,
                Function<Player, Float> manaFunc, Function<Player, Float> maxManaFunc,
                Function<Player, Float> magicShieldFunc, Function<Player, Float> maxMagicShieldFunc,
                Function<Player, Float> energyFunc, Function<Player, Float> maxEnergyFunc,
                Function<Player, Float> bloodFunc, Function<Player, Float> maxBloodFunc,
                Function<Player, Integer> levelFunc, Function<Player, Float> expFunc,
                Function<Player, Float> expRequiredFunc, Function<Player, Boolean> bloodMagicFunc,
                Map<String, Function<Player, Object>> customData) {
            this.id = id;
            this.priority = priority;
            this.availabilityCheck = availabilityCheck;
            this.healthFunc = healthFunc;
            this.maxHealthFunc = maxHealthFunc;
            this.manaFunc = manaFunc;
            this.maxManaFunc = maxManaFunc;
            this.magicShieldFunc = magicShieldFunc;
            this.maxMagicShieldFunc = maxMagicShieldFunc;
            this.energyFunc = energyFunc;
            this.maxEnergyFunc = maxEnergyFunc;
            this.bloodFunc = bloodFunc;
            this.maxBloodFunc = maxBloodFunc;
            this.levelFunc = levelFunc;
            this.expFunc = expFunc;
            this.expRequiredFunc = expRequiredFunc;
            this.bloodMagicFunc = bloodMagicFunc;
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
        public float getCurrentHealth(Player player) {
            return healthFunc != null ? healthFunc.apply(player) : super.getCurrentHealth(player);
        }
        
        @Override
        public float getMaxHealth(Player player) {
            return maxHealthFunc != null ? maxHealthFunc.apply(player) : super.getMaxHealth(player);
        }
        
        @Override
        public float getCurrentMana(Player player) {
            return manaFunc != null ? manaFunc.apply(player) : super.getCurrentMana(player);
        }
        
        @Override
        public float getMaxMana(Player player) {
            return maxManaFunc != null ? maxManaFunc.apply(player) : super.getMaxMana(player);
        }
        
        @Override
        public float getCurrentMagicShield(Player player) {
            return magicShieldFunc != null ? magicShieldFunc.apply(player) : super.getCurrentMagicShield(player);
        }
        
        @Override
        public float getMaxMagicShield(Player player) {
            return maxMagicShieldFunc != null ? maxMagicShieldFunc.apply(player) : super.getMaxMagicShield(player);
        }
        
        @Override
        public float getCurrentEnergy(Player player) {
            return energyFunc != null ? energyFunc.apply(player) : super.getCurrentEnergy(player);
        }
        
        @Override
        public float getMaxEnergy(Player player) {
            return maxEnergyFunc != null ? maxEnergyFunc.apply(player) : super.getMaxEnergy(player);
        }
        
        @Override
        public float getCurrentBlood(Player player) {
            return bloodFunc != null ? bloodFunc.apply(player) : super.getCurrentBlood(player);
        }
        
        @Override
        public float getMaxBlood(Player player) {
            return maxBloodFunc != null ? maxBloodFunc.apply(player) : super.getMaxBlood(player);
        }
        
        @Override
        public int getLevel(Player player) {
            return levelFunc != null ? levelFunc.apply(player) : super.getLevel(player);
        }
        
        @Override
        public float getExp(Player player) {
            return expFunc != null ? expFunc.apply(player) : super.getExp(player);
        }
        
        @Override
        public float getExpRequiredForLevelUp(Player player) {
            return expRequiredFunc != null ? expRequiredFunc.apply(player) : super.getExpRequiredForLevelUp(player);
        }
        
        @Override
        public boolean isBloodMagicActive(Player player) {
            return bloodMagicFunc != null ? bloodMagicFunc.apply(player) : super.isBloodMagicActive(player);
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
