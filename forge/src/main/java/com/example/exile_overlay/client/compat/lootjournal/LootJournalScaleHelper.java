package com.example.exile_overlay.client.compat.lootjournal;

import com.example.exile_overlay.client.config.LootJournalCompatConfig;
import com.example.exile_overlay.util.DropItemResolver;
import com.example.exile_overlay.util.ItemRarityResolver;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

public final class LootJournalScaleHelper {

    private LootJournalScaleHelper() {
    }

    public static float calculateScale(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 1.0f;
        }

        LootJournalCompatConfig config = LootJournalCompatConfig.getInstance();
        if (!config.isEnableCompat() || !config.isAutoScaleByWeight()) {
            return 1.0f;
        }

        // 1. ドロップ重み (Weight) による判定
        int weight = DropItemResolver.resolveWeight(stack);
        if (weight == 1) {
            return config.getMirrorScale(); // Mirror of Kalandra: 2.0x
        }
        if (weight <= 10 || weight == 0) {
            return config.getMegaUberScale(); // Mega Uber / 封印限定パーフェクトシード等: 1.7x
        }
        if (weight <= 50) {
            return config.getUberScale(); // Uber カレンシー等: 1.4x
        }
        if (weight <= 250) {
            return config.getRareScale(); // Rare カレンシー等: 1.2x
        }

        // 2. レアリティによるフォールバック判定
        String rarity = ItemRarityResolver.resolveRarity(stack);
        if (rarity != null) {
            String lower = rarity.toLowerCase(Locale.ROOT);
            if (lower.equals("mythic")) {
                return config.getMegaUberScale();
            }
            if (lower.equals("unique")) {
                return config.getUberScale();
            }
        }

        return 1.0f;
    }
}
