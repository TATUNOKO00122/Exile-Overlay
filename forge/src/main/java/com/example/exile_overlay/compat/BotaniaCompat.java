package com.example.exile_overlay.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Botania MOD との動作互換ブリッジクラス。
 * Botania 未導入環境でクラスロードエラーが発生しないよう、リフレクション経由で APIに安全にアクセスする。
 */
public class BotaniaCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(BotaniaCompat.class);

    private static boolean initialized = false;
    private static boolean botaniaPresent = false;

    // Reflection objects
    private static Method getManaItemsMethod;
    private static Method getManaAccesoriesMethod;
    private static Method findManaItemMethod;
    private static Method getManaMethod;
    private static Method getMaxManaMethod;
    private static Method isNoExportMethod;
    private static Object xplatInstance;
    private static Object manaItemHandlerInstance;

    private static TagKey<Item> manaUsingItemsTag;

    /**
     * リフレクション初期化
     */
    private static synchronized void initReflection() {
        if (initialized) {
            return;
        }
        initialized = true;
        botaniaPresent = ModList.get().isLoaded("botania");
        if (!botaniaPresent) {
            return;
        }

        try {
            // ManaItemHandler.instance()
            Class<?> handlerClass = Class.forName("vazkii.botania.api.mana.ManaItemHandler");
            Method instanceMethod = handlerClass.getMethod("instance");
            manaItemHandlerInstance = instanceMethod.invoke(null);
            getManaItemsMethod = handlerClass.getMethod("getManaItems", Player.class);
            getManaAccesoriesMethod = handlerClass.getMethod("getManaAccesories", Player.class);

            // XplatAbstractions.INSTANCE.findManaItem(stack)
            Class<?> xplatClass = Class.forName("vazkii.botania.xplat.XplatAbstractions");
            xplatInstance = xplatClass.getField("INSTANCE").get(null);
            findManaItemMethod = xplatClass.getMethod("findManaItem", ItemStack.class);

            // ManaItem methods
            Class<?> manaItemClass = Class.forName("vazkii.botania.api.mana.ManaItem");
            getManaMethod = manaItemClass.getMethod("getMana");
            getMaxManaMethod = manaItemClass.getMethod("getMaxMana");
            isNoExportMethod = manaItemClass.getMethod("isNoExport");

            manaUsingItemsTag = TagKey.create(Registries.ITEM, ResourceLocation.tryParse("botania:mana_using_items"));
            LOGGER.info("BotaniaCompat initialized successfully.");
        } catch (Throwable t) {
            LOGGER.error("Failed to initialize BotaniaCompat reflection", t);
            botaniaPresent = false;
        }
    }

    /**
     * Botania が導入されているか確認
     */
    public static boolean isBotaniaLoaded() {
        if (!initialized) {
            initReflection();
        }
        return botaniaPresent;
    }

    /**
     * プレイヤーがマナ使用アイテム（mana_using_items）を所持しているか確認
     */
    public static boolean hasManaUsingItem(Player player) {
        if (!isBotaniaLoaded() || player == null) {
            return false;
        }

        try {
            Container inv = player.getInventory();
            int size = inv.getContainerSize();
            for (int i = 0; i < size; i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && manaUsingItemsTag != null && stack.is(manaUsingItemsTag)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Error checking mana using items", t);
        }
        return false;
    }

    public record ManaInfo(int totalMana, int totalMaxMana) {}

    /**
     * プレイヤー全体の保持マナおよび最大マナを取得
     */
    @SuppressWarnings("unchecked")
    public static ManaInfo getPlayerManaInfo(Player player) {
        if (!isBotaniaLoaded() || player == null) {
            return new ManaInfo(0, 0);
        }

        int totalMana = 0;
        int totalMaxMana = 0;

        try {
            List<ItemStack> items = (List<ItemStack>) getManaItemsMethod.invoke(manaItemHandlerInstance, player);
            List<ItemStack> acc = (List<ItemStack>) getManaAccesoriesMethod.invoke(manaItemHandlerInstance, player);

            for (List<ItemStack> list : List.of(items, acc)) {
                if (list == null) continue;
                for (ItemStack stack : list) {
                    if (stack == null || stack.isEmpty()) continue;
                    Object manaItem = findManaItemMethod.invoke(xplatInstance, stack);
                    if (manaItem != null) {
                        boolean noExport = (Boolean) isNoExportMethod.invoke(manaItem);
                        if (!noExport) {
                            totalMana += (Integer) getManaMethod.invoke(manaItem);
                            totalMaxMana += (Integer) getMaxManaMethod.invoke(manaItem);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("Error calculating Botania player mana info", t);
        }

        return new ManaInfo(totalMana, totalMaxMana);
    }
}
