package com.example.exile_overlay.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class DropItemResolver {

    public record ItemInfo(String itemId, String uniqueId, String rarity) {}

    private DropItemResolver() {
    }

    public static ItemInfo resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemInfo("", null, null);
        }

        String itemId = "";
        try {
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null) {
                itemId = key.toString();
            }
        } catch (Exception ignored) {
        }

        String uniqueId = resolveUniqueId(stack);
        String rarity = ItemRarityResolver.resolveRarity(stack);

        return new ItemInfo(itemId, uniqueId, rarity);
    }

    public static String resolveFilterId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        String uniqueId = resolveUniqueId(stack);
        if (uniqueId != null && !uniqueId.isEmpty()) {
            return "mmorpg:unique/" + uniqueId;
        }
        var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        String itemId = key != null ? key.toString() : "";
        if (itemId.isEmpty()) {
            return "";
        }

        // カレンシーやバニラ素材等でない場合で、レアリティがあるアイテム(オーメン、ジュエル、装備等)は itemId@rarity とする
        String rarity = ItemRarityResolver.resolveRarity(stack);
        if (rarity != null && !rarity.isEmpty() && !itemId.startsWith("mmorpg:currency/")) {
            return itemId + "@" + rarity.toLowerCase(java.util.Locale.ROOT);
        }
        return itemId;
    }

    private static String resolveUniqueId(ItemStack stack) {
        if (!stack.hasTag() || !ModList.get().isLoaded("mmorpg")) {
            return null;
        }

        try {
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("mmorpg_custom_data")) {
                String jsonStr = tag.getString("mmorpg_custom_data");
                JsonObject obj = JsonParser.parseString(jsonStr).getAsJsonObject();
                if (obj.has("data")) {
                    JsonObject dataObj = obj.getAsJsonObject("data");
                    if (dataObj.has("map")) {
                        JsonObject mapObj = dataObj.getAsJsonObject("map");
                        if (mapObj.has("uq")) {
                            return mapObj.get("uq").getAsString();
                        }
                    }
                }
                if (obj.has("uq")) {
                    return obj.get("uq").getAsString();
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
