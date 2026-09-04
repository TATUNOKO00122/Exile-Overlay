package com.example.exile_overlay.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.uncommon.interfaces.IRarityItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.GemItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.Locale;

/**
 * ItemStackからMine and SlashのレアリティID（unique, mythic, legendary等）を解決するユーティリティ。
 * NBTを持つ装備・ジェム・マップ等と、NBTを持たないカレンシーや宝石の両方に対応。
 */
public final class ItemRarityResolver {

    private static final String[] NBT_RARITY_KEYS = {
            "mmorpg_gear",
            "mmorpg_skill_gem",
            "mmorpg_jewel",
            "mmorpg_map",
            "mmorpg_stat_soul",
            "mmorpg_loot_crate",
            "mmorpg_omen",
            "mmorpg_tool_stats",
            "mmorpg_loot_chest"
    };

    private ItemRarityResolver() {
    }

    /**
     * 指定されたItemStackのM&Sレアリティ文字列を小文字で返す。
     * レアリティが存在しない、または未対応のアイテムの場合はnullを返す。
     */
    public static String resolveRarity(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        if (!ModList.get().isLoaded("mmorpg")) {
            return null;
        }

        try {
            // 1. NBTタグ内にJSONとして保存されているアイテム（ギア、スキルジェム、ジュエル、マップ等）
            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    for (String key : NBT_RARITY_KEYS) {
                        if (tag.contains(key)) {
                            String jsonStr = tag.getString(key);
                            JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                            if (obj.has("rar")) {
                                return obj.get("rar").getAsString().toLowerCase(Locale.ROOT);
                            }
                        }
                    }
                }
            }

            // 2. カレンシー（ExileCurrencyItem等）: IRarityItem経由で判定
            if (stack.getItem() instanceof IRarityItem rarityItem) {
                GearRarity rarity = rarityItem.getItemRarity(stack);
                if (rarity != null && rarity.GUID() != null) {
                    return rarity.GUID().toLowerCase(Locale.ROOT);
                }
            }

            // 3. 宝石（GemItem）: rank.rar から判定
            if (stack.getItem() instanceof GemItem gemItem) {
                if (gemItem.gemRank != null && gemItem.gemRank.rar != null) {
                    return gemItem.gemRank.rar.toLowerCase(Locale.ROOT);
                }
            }
        } catch (Exception ignored) {
            // アイテムデータの不整合等によるエラー時は安全にnullを返却
        }

        return null;
    }
}
