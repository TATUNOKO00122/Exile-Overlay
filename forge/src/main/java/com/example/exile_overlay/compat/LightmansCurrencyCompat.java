package com.example.exile_overlay.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lightman's Currency MOD との動作互換ブリッジクラス。
 * Lightman's Currency 未導入環境でクラスロードエラーが発生しないよう、リフレクション経由で APIに安全にアクセスする。
 */
public class LightmansCurrencyCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(LightmansCurrencyCompat.class);
    private static final String MOD_ID = "lightmanscurrency";

    private static boolean initialized = false;
    private static boolean modLoaded = false;

    // Reflection objects
    private static Method lazyGetWalletHandlerMethod;
    private static Method getWalletMethod;
    private static Method isWalletMethod;
    private static Method getMoneyApiMethod;
    private static Method getPlayersMoneyHandlerMethod;
    private static Method getStoredMoneyMethod;
    private static Method isEmptyMoneyViewMethod;
    private static Method getRandomValueMethod;
    private static Method getRandomValueTextMethod;
    private static Method getAsItemListMethod;

    private static Class<?> itemBasedValueClass;
    private static Object moneyApiInstance;

    private static final ResourceLocation WALLET_RES = ResourceLocation.tryParse("lightmanscurrency:wallet_leather");
    private static final ResourceLocation COIN_COPPER_RES = ResourceLocation.tryParse("lightmanscurrency:coin_copper");
    private static final ResourceLocation COIN_IRON_RES = ResourceLocation.tryParse("lightmanscurrency:coin_iron");
    private static final ResourceLocation COIN_GOLD_RES = ResourceLocation.tryParse("lightmanscurrency:coin_gold");
    private static final ResourceLocation COIN_EMERALD_RES = ResourceLocation.tryParse("lightmanscurrency:coin_emerald");
    private static final ResourceLocation COIN_DIAMOND_RES = ResourceLocation.tryParse("lightmanscurrency:coin_diamond");

    /**
     * リフレクション初期化
     */
    private static synchronized void initReflection() {
        if (initialized) {
            return;
        }
        initialized = true;
        modLoaded = ModList.get().isLoaded(MOD_ID);
        if (!modLoaded) {
            return;
        }

        try {
            // WalletCapability.lazyGetWalletHandler(Entity)
            Class<?> walletCapClass = Class.forName("io.github.lightman314.lightmanscurrency.common.capability.wallet.WalletCapability");
            lazyGetWalletHandlerMethod = walletCapClass.getMethod("lazyGetWalletHandler", net.minecraft.world.entity.Entity.class);

            // IWalletHandler.getWallet()
            Class<?> walletHandlerClass = Class.forName("io.github.lightman314.lightmanscurrency.common.capability.wallet.IWalletHandler");
            getWalletMethod = walletHandlerClass.getMethod("getWallet");

            // WalletItem.isWallet(ItemStack)
            Class<?> walletItemClass = Class.forName("io.github.lightman314.lightmanscurrency.common.items.WalletItem");
            isWalletMethod = walletItemClass.getMethod("isWallet", ItemStack.class);

            // MoneyAPI.getApi()
            Class<?> moneyApiClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.MoneyAPI");
            getMoneyApiMethod = moneyApiClass.getMethod("getApi");
            moneyApiInstance = getMoneyApiMethod.invoke(null);

            // MoneyAPI.GetPlayersMoneyHandler(Player)
            getPlayersMoneyHandlerMethod = moneyApiClass.getMethod("GetPlayersMoneyHandler", Player.class);

            // IMoneyHolder.getStoredMoney()
            Class<?> moneyHolderClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.holder.IMoneyHolder");
            getStoredMoneyMethod = moneyHolderClass.getMethod("getStoredMoney");

            // MoneyView methods
            Class<?> moneyViewClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.MoneyView");
            isEmptyMoneyViewMethod = moneyViewClass.getMethod("isEmpty");
            getRandomValueMethod = moneyViewClass.getMethod("getRandomValue");
            getRandomValueTextMethod = moneyViewClass.getMethod("getRandomValueText");

            // IItemBasedValue
            itemBasedValueClass = Class.forName("io.github.lightman314.lightmanscurrency.api.money.value.IItemBasedValue");
            getAsItemListMethod = itemBasedValueClass.getMethod("getAsItemList");

            LOGGER.info("LightmansCurrencyCompat initialized successfully.");
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize LightmansCurrencyCompat reflection", t);
            modLoaded = false;
        }
    }

    /**
     * Lightman's Currency が導入されているか確認
     */
    public static boolean isLightmansCurrencyLoaded() {
        if (!initialized) {
            initReflection();
        }
        return modLoaded;
    }

    /**
     * プレイヤーが装備している財布 ItemStack を取得
     */
    public static ItemStack getEquippedWallet(Player player) {
        if (!isLightmansCurrencyLoaded() || player == null) {
            return ItemStack.EMPTY;
        }

        try {
            Object handler = lazyGetWalletHandlerMethod.invoke(null, player);
            if (handler != null) {
                ItemStack wallet = (ItemStack) getWalletMethod.invoke(handler);
                if (wallet != null && !wallet.isEmpty()) {
                    boolean isWallet = (Boolean) isWalletMethod.invoke(null, wallet);
                    if (isWallet) {
                        return wallet;
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Error getting equipped wallet from LightmansCurrency", t);
        }
        return ItemStack.EMPTY;
    }

    /**
     * プレイヤーが所持しているコインの ItemStack リストを取得
     */
    @SuppressWarnings("unchecked")
    public static List<ItemStack> getStoredCoins(Player player) {
        if (!isLightmansCurrencyLoaded() || player == null) {
            return Collections.emptyList();
        }

        try {
            if (moneyApiInstance == null) {
                moneyApiInstance = getMoneyApiMethod.invoke(null);
            }
            Object moneyHolder = getPlayersMoneyHandlerMethod.invoke(moneyApiInstance, player);
            if (moneyHolder == null) {
                return Collections.emptyList();
            }

            Object moneyView = getStoredMoneyMethod.invoke(moneyHolder);
            if (moneyView == null) {
                return Collections.emptyList();
            }

            boolean isEmpty = (Boolean) isEmptyMoneyViewMethod.invoke(moneyView);
            if (isEmpty) {
                return Collections.emptyList();
            }

            Object randomValue = getRandomValueMethod.invoke(moneyView);
            if (randomValue != null && itemBasedValueClass.isInstance(randomValue)) {
                List<ItemStack> list = (List<ItemStack>) getAsItemListMethod.invoke(randomValue);
                if (list != null && !list.isEmpty()) {
                    return list;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Error getting stored coins from LightmansCurrency", t);
        }
        return Collections.emptyList();
    }

    /**
     * プレイヤーの所持金テキスト（例: "100c"）を取得
     */
    public static Component getStoredMoneyText(Player player) {
        if (!isLightmansCurrencyLoaded() || player == null) {
            return Component.empty();
        }

        try {
            if (moneyApiInstance == null) {
                moneyApiInstance = getMoneyApiMethod.invoke(null);
            }
            Object moneyHolder = getPlayersMoneyHandlerMethod.invoke(moneyApiInstance, player);
            if (moneyHolder == null) {
                return Component.empty();
            }

            Object moneyView = getStoredMoneyMethod.invoke(moneyHolder);
            if (moneyView == null) {
                return Component.empty();
            }

            boolean isEmpty = (Boolean) isEmptyMoneyViewMethod.invoke(moneyView);
            if (isEmpty) {
                return Component.empty();
            }

            Object textObj = getRandomValueTextMethod.invoke(moneyView);
            if (textObj instanceof Component comp) {
                return comp;
            }
        } catch (Throwable t) {
            LOGGER.debug("Error getting stored money text from LightmansCurrency", t);
        }
        return Component.empty();
    }

    /**
     * プレイヤーが財布またはコインを所持しているか判定
     */
    public static boolean hasWalletOrCoins(Player player) {
        if (!isLightmansCurrencyLoaded() || player == null) {
            return false;
        }
        ItemStack wallet = getEquippedWallet(player);
        if (!wallet.isEmpty()) {
            return true;
        }
        List<ItemStack> coins = getStoredCoins(player);
        return !coins.isEmpty();
    }

    /**
     * プレビュー表示用のダミー財布アイテムを取得
     */
    public static ItemStack getPreviewWallet() {
        if (WALLET_RES != null) {
            Item item = BuiltInRegistries.ITEM.get(WALLET_RES);
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.BUNDLE);
    }

    /**
     * プレビュー表示用のダミーコインアイテムリストを取得
     */
    public static List<ItemStack> getPreviewCoins() {
        List<ItemStack> previewList = new ArrayList<>();
        ResourceLocation[] coinIds = {
            COIN_DIAMOND_RES,
            COIN_EMERALD_RES,
            COIN_GOLD_RES,
            COIN_IRON_RES,
            COIN_COPPER_RES
        };

        for (ResourceLocation id : coinIds) {
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != Items.AIR) {
                    previewList.add(new ItemStack(item, 5));
                }
            }
        }

        if (previewList.isEmpty()) {
            previewList.add(new ItemStack(Items.GOLD_NUGGET, 10));
            previewList.add(new ItemStack(Items.IRON_NUGGET, 20));
        }

        return previewList;
    }
}
