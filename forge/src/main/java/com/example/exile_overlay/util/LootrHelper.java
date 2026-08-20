package com.example.exile_overlay.util;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Set;

/**
 * Lootr MOD 連携およびコンテナ画面判定の一元管理ヘルパー
 *
 * - Lootr 対象ブロック・エンティティ定義の Single Source of Truth
 * - 自動ソート・クイックルートで除外すべき画面（プレイヤーインベントリ、作業台、MOD製バックパック画面等）の判定を一元化
 * - ワールド実体判定（BlockState / BlockEntity / Entity）による誤爆の 100% 防止
 */
public class LootrHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile Boolean loaded = null;
    private static final Object LOCK = new Object();

    // Lootr MOD の全登録ブロックID
    public static final Set<String> LOOTR_BLOCKS = Set.of(
            "lootr:lootr_chest",
            "lootr:lootr_trapped_chest",
            "lootr:lootr_barrel",
            "lootr:lootr_shulker",
            "lootr:lootr_inventory"
    );

    // Lootr MOD の全登録エンティティID（チェスト付きトロッコ等）
    public static final Set<String> LOOTR_ENTITIES = Set.of(
            "lootr:lootr_minecart"
    );

    // 除外対象とする MOD 製画面クラスのパッケージプレフィックス
    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "net.p3pp3rf1y.sophisticatedbackpacks",
            "net.p3pp3rf1y.sophisticatedcore",
            "com.tiviacz.travelersbackpack",
            "top.theillusivec4.curios",
            "draylar.inmis"
    );

    public static boolean isLoaded() {
        if (loaded == null) {
            synchronized (LOCK) {
                if (loaded == null) {
                    try {
                        Class.forName("noobanidus.mods.lootr.api.LootrAPI");
                        loaded = true;
                        LOGGER.info("Lootr detected, enabling Lootr integration.");
                    } catch (ClassNotFoundException e) {
                        loaded = false;
                        LOGGER.debug("Lootr not found, Lootr features disabled.");
                    }
                }
            }
        }
        return loaded;
    }

    /**
     * 指定されたブロックが Lootr の対象コンテナであるか判定
     */
    public static boolean isLootrBlock(BlockState state) {
        if (state == null) return false;
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return LOOTR_BLOCKS.contains(blockId);
    }

    /**
     * 指定された BlockEntity が Lootr の対象であるか判定
     */
    public static boolean isLootrBlockEntity(BlockEntity be) {
        if (be == null) return false;
        String className = be.getClass().getName();
        return className.startsWith("noobanidus.mods.lootr.");
    }

    /**
     * 指定されたエンティティが Lootr の対象（チェスト付きトロッコ等）であるか判定
     */
    public static boolean isLootrEntity(Entity entity) {
        if (entity == null) return false;
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if (LOOTR_ENTITIES.contains(entityId)) {
            return true;
        }
        String className = entity.getClass().getName();
        return className.startsWith("noobanidus.mods.lootr.");
    }

    /**
     * 指定座標に実在するコンテナが Lootr のものであるか検証
     */
    public static boolean isLootrContainerAt(Level level, BlockPos pos) {
        if (level == null || pos == null) return false;
        BlockState state = level.getBlockState(pos);
        if (isLootrBlock(state)) {
            return true;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return isLootrBlockEntity(be);
    }

    /**
     * 自動ソート・クイックルートの対象外とすべき画面（プレイヤーインベントリ、作業台、MOD製バックパック画面等）か包括判定
     */
    public static boolean isExcludedScreen(Screen screen) {
        if (screen == null) {
            return true;
        }

        // 1. バニラインベントリ・作業台・機能画面の包括除外
        if (screen instanceof InventoryScreen
                || screen instanceof CreativeModeInventoryScreen
                || screen instanceof CraftingScreen
                || screen instanceof AnvilScreen
                || screen instanceof EnchantmentScreen
                || screen instanceof FurnaceScreen
                || screen instanceof SmokerScreen
                || screen instanceof BlastFurnaceScreen
                || screen instanceof BrewingStandScreen
                || screen instanceof BeaconScreen
                || screen instanceof HopperScreen
                || screen instanceof DispenserScreen
                || screen instanceof GrindstoneScreen
                || screen instanceof SmithingScreen
                || screen instanceof StonecutterScreen
                || screen instanceof LoomScreen
                || screen instanceof CartographyTableScreen
                || screen instanceof MerchantScreen) {
            return true;
        }

        // 2. バックパック・外部MODインベントリ画面の除外
        String className = screen.getClass().getName();
        for (String pkg : EXCLUDED_PACKAGES) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}