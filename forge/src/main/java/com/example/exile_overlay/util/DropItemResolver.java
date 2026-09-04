package com.example.exile_overlay.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.robertx22.mine_and_slash.a_libraries.curios.interfaces.IRing;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.unique_items.UniqueGear;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.uncommon.datasaving.StackSaving;
import com.robertx22.mine_and_slash.uncommon.interfaces.IRarityItem;
import com.robertx22.mine_and_slash.vanilla_mc.items.gemrunes.GemItem;
import com.robertx22.orbs_of_crafting.register.ExileCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public final class DropItemResolver {

    public record ItemInfo(String itemId, String uniqueId, String rarity, String slot, String gearType) {}

    private record SlotAndType(String slot, String gearType) {}

    private DropItemResolver() {
    }

    public static ItemInfo resolve(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ItemInfo("", null, null, null, null);
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
        SlotAndType slotAndType = resolveSlotAndType(stack, uniqueId);

        return new ItemInfo(itemId, uniqueId, rarity, slotAndType.slot(), slotAndType.gearType());
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

    private static SlotAndType resolveSlotAndType(ItemStack stack, String uniqueId) {
        if (stack == null || stack.isEmpty()) {
            return new SlotAndType(null, null);
        }

        String slot = null;
        String gearType = null;

        if (ModList.get().isLoaded("mmorpg")) {
            try {
                if (StackSaving.GEARS.has(stack)) {
                    var gearData = StackSaving.GEARS.loadFrom(stack);
                    if (gearData != null) {
                        gearType = gearData.gtype;
                        BaseGearType base = ExileDB.GearTypes().get(gearData.gtype);
                        if (base != null) {
                            slot = base.gear_slot;
                        }
                    }
                }

                if (slot == null && uniqueId != null && !uniqueId.isEmpty()) {
                    UniqueGear unique = ExileDB.UniqueGears().get(uniqueId);
                    if (unique != null) {
                        gearType = unique.base_gear;
                        BaseGearType base = unique.getBaseGear();
                        if (base == null && unique.base_gear != null) {
                            base = ExileDB.GearTypes().get(unique.base_gear);
                        }
                        if (base != null) {
                            slot = base.gear_slot;
                        }
                    }
                }

                if (slot == null && stack.getItem() instanceof IRing) {
                    slot = "ring";
                    gearType = "ring";
                }
            } catch (Exception ignored) {
            }
        }

        if (slot == null) {
            try {
                if (stack.is(ItemTags.create(new ResourceLocation("curios", "ring")))) {
                    slot = "ring";
                    if (gearType == null) gearType = "ring";
                } else if (stack.is(ItemTags.create(new ResourceLocation("curios", "necklace")))) {
                    slot = "necklace";
                    if (gearType == null) gearType = "necklace";
                }
            } catch (Exception ignored) {
            }
        }

        if (slot == null) {
            if (stack.getItem() instanceof ArmorItem armor) {
                slot = switch (armor.getType()) {
                    case HELMET -> "helmet";
                    case CHESTPLATE -> "chest";
                    case LEGGINGS -> "pants";
                    case BOOTS -> "boots";
                };
            } else if (stack.getItem() instanceof SwordItem) {
                slot = "sword";
            } else if (stack.getItem() instanceof BowItem) {
                slot = "bow";
            } else if (stack.getItem() instanceof CrossbowItem) {
                slot = "crossbow";
            } else if (stack.getItem() instanceof ShieldItem) {
                slot = "shield";
            } else if (stack.getItem() instanceof TridentItem) {
                slot = "trident";
            }
        }

        return new SlotAndType(
                slot != null ? slot.toLowerCase(Locale.ROOT) : null,
                gearType != null ? gearType.toLowerCase(Locale.ROOT) : null
        );
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

    public static boolean isMsItem(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModList.get().isLoaded("mmorpg")) {
            return false;
        }

        try {
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && "mmorpg".equals(key.getNamespace())) {
                return true;
            }

            if (stack.getItem() instanceof IRarityItem || stack.getItem() instanceof GemItem) {
                return true;
            }

            if (stack.hasTag()) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    if (tag.contains("mmorpg_gear")
                            || tag.contains("mmorpg_skill_gem")
                            || tag.contains("mmorpg_jewel")
                            || tag.contains("mmorpg_map")
                            || tag.contains("mmorpg_stat_soul")
                            || tag.contains("mmorpg_loot_crate")
                            || tag.contains("mmorpg_omen")
                            || tag.contains("mmorpg_tool_stats")
                            || tag.contains("mmorpg_loot_chest")
                            || tag.contains("mmorpg_custom_data")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return false;
    }

    public static int resolveWeight(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !ModList.get().isLoaded("mmorpg")) {
            return Integer.MAX_VALUE;
        }

        try {
            var currencyOpt = ExileCurrency.get(stack);
            if (currencyOpt.isPresent()) {
                return currencyOpt.get().Weight();
            }

            String uniqueId = resolveUniqueId(stack);
            if (uniqueId != null && !uniqueId.isEmpty()) {
                UniqueGear unique = ExileDB.UniqueGears().get(uniqueId);
                if (unique != null) {
                    return unique.Weight();
                }
            }
        } catch (Exception ignored) {
        }

        return Integer.MAX_VALUE;
    }

    public static Component resolveDisplayName(ItemStack stack) {
        return resolveDisplayName(stack, true);
    }

    public static Component resolveDisplayName(ItemStack stack, boolean showFullAffixName) {
        if (stack == null || stack.isEmpty()) {
            return Component.empty();
        }

        if (showFullAffixName && ModList.get().isLoaded("mmorpg")) {
            try {
                if (StackSaving.GEARS.has(stack)) {
                    var gearData = StackSaving.GEARS.loadFrom(stack);
                    if (gearData != null) {
                        var names = gearData.GetDisplayName(ExileStack.of(stack));
                        if (names != null && !names.isEmpty()) {
                            return names.get(0);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        ChatFormatting format = resolveRarityFormatting(stack);
        return Component.empty().append(stack.getHoverName()).withStyle(Style.EMPTY.withColor(format));
    }

    public static ChatFormatting resolveRarityFormatting(ItemStack stack) {
        String rarity = ItemRarityResolver.resolveRarity(stack);
        if (rarity == null) {
            return ChatFormatting.WHITE;
        }
        return switch (rarity.toLowerCase(java.util.Locale.ROOT)) {
            case "unique" -> ChatFormatting.RED;
            case "mythic" -> ChatFormatting.DARK_PURPLE;
            case "legendary" -> ChatFormatting.GOLD;
            case "epic" -> ChatFormatting.LIGHT_PURPLE;
            case "rare" -> ChatFormatting.AQUA;
            case "uncommon", "uncommon_item" -> ChatFormatting.GREEN;
            default -> ChatFormatting.WHITE;
        };
    }
}

