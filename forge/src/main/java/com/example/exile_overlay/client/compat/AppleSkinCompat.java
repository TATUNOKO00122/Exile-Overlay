package com.example.exile_overlay.client.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * AppleSkin MOD (appleskin-1.20.1-forge) との互換性処理を行うクラス。
 * AppleSkin MODが導入されている場合のみ機能する。
 */
public class AppleSkinCompat {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppleSkinCompat.class);
    public static final String MOD_ID = "appleskin";
    public static final ResourceLocation APPLESKIN_ICONS = new ResourceLocation(MOD_ID, "textures/icons.png");

    private static Boolean loaded = null;

    // リフレクションキャッシュ
    private static Method canConsumeMethod;
    private static Method isRottenMethod;
    private static Method getModifiedFoodValuesMethod;

    // 点滅アニメーション状態
    private static float unclampedFlashAlpha = 0f;
    private static float flashAlpha = 0f;
    private static byte alphaDir = 1;

    public static boolean isLoaded() {
        if (loaded == null) {
            try {
                loaded = ModList.get().isLoaded(MOD_ID);
                if (loaded) {
                    initReflection();
                }
            } catch (Throwable t) {
                loaded = false;
            }
        }
        return loaded;
    }

    private static void initReflection() {
        try {
            Class<?> foodHelperClass = Class.forName("squeek.appleskin.helpers.FoodHelper");
            canConsumeMethod = foodHelperClass.getMethod("canConsume", ItemStack.class, Player.class);
            isRottenMethod = foodHelperClass.getMethod("isRotten", ItemStack.class, Player.class);
            getModifiedFoodValuesMethod = foodHelperClass.getMethod("getModifiedFoodValues", ItemStack.class, Player.class);
        } catch (Throwable t) {
            LOGGER.debug("AppleSkin reflection init error: {}", t.getMessage());
        }
    }

    /**
     * 毎フレーム/毎Tickの点滅アルファ更新
     */
    public static void tick() {
        unclampedFlashAlpha += alphaDir * 0.125f;
        if (unclampedFlashAlpha >= 1.5f) {
            alphaDir = -1;
        } else if (unclampedFlashAlpha <= -0.5f) {
            alphaDir = 1;
        }
        flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha)) * 0.65f;
    }

    public static float getFlashAlpha() {
        return flashAlpha;
    }

    /**
     * プレイヤーが持っている食べ物アイテムを取得（メインハンド優先、オフハンドフォールバック）
     */
    public static ItemStack getHeldFoodItem(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack mainHand = player.getMainHandItem();
        if (canConsume(mainHand, player)) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (canConsume(offHand, player)) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    public static boolean canConsume(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null || canConsumeMethod == null) return false;
        try {
            return (boolean) canConsumeMethod.invoke(null, stack, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isRotten(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null || isRottenMethod == null) return false;
        try {
            return (boolean) isRottenMethod.invoke(null, stack, player);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static FoodValues getFoodValues(ItemStack stack, Player player) {
        if (stack == null || stack.isEmpty() || player == null || getModifiedFoodValuesMethod == null) {
            return new FoodValues(0, 0f);
        }
        try {
            Object result = getModifiedFoodValuesMethod.invoke(null, stack, player);
            if (result != null) {
                int hunger = (int) result.getClass().getField("hunger").get(result);
                float saturationModifier = (float) result.getClass().getField("saturationModifier").get(result);
                return new FoodValues(hunger, saturationModifier);
            }
        } catch (Throwable ignored) {
        }
        return new FoodValues(0, 0f);
    }

    public record FoodValues(int hunger, float saturationModifier) {
        public float getSaturationIncrement() {
            return hunger * saturationModifier * 2.0f;
        }
    }
}

