package com.example.exile_overlay.compat.jei;

import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class AffixRecipeMaker {

    private AffixRecipeMaker() {
    }

    public static class CategorizedRecipes {
        public final List<AffixRecipe> prefixes = new ArrayList<>();
        public final List<AffixRecipe> suffixes = new ArrayList<>();
        public final List<AffixRecipe> implicits = new ArrayList<>();
        public final List<AffixRecipe> enchants = new ArrayList<>();
        public final List<AffixRecipe> all = new ArrayList<>();
    }

    public static CategorizedRecipes createRecipes() {
        CategorizedRecipes result = new CategorizedRecipes();

        try {
            if (ExileDB.Affixes() == null || ExileDB.GearTypes() == null) {
                return result;
            }

            // 1. Build map of BaseGearType -> List<ItemStack>
            Map<BaseGearType, List<ItemStack>> gearTypeItemMap = new LinkedHashMap<>();
            List<BaseGearType> gearTypes = ExileDB.GearTypes().getList();

            for (BaseGearType gearType : gearTypes) {
                if (gearType == null) continue;
                List<ItemStack> items = new ArrayList<>();
                Set<Item> seenItems = new HashSet<>();

                if (gearType.possible_items != null) {
                    for (BaseGearType.ItemChance ic : gearType.possible_items) {
                        if (ic != null) {
                            try {
                                Item item = ic.getItem();
                                if (item != null && item != Items.AIR && seenItems.add(item)) {
                                    items.add(new ItemStack(item));
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    try {
                        if (ExileDB.GearRarities() != null && ExileDB.GearRarities().get(IRarity.COMMON_ID) != null) {
                            Item sample = gearType.getRandomItem(ExileDB.GearRarities().get(IRarity.COMMON_ID));
                            if (sample != null && sample != Items.AIR && seenItems.add(sample)) {
                                items.add(new ItemStack(sample));
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                gearTypeItemMap.put(gearType, items);
            }

            // Jewel fallback items
            List<ItemStack> jewelItems = new ArrayList<>();
            try {
                if (SlashItems.DEX_JEWEL != null && SlashItems.DEX_JEWEL.get() != null) {
                    jewelItems.add(new ItemStack(SlashItems.DEX_JEWEL.get()));
                }
                if (SlashItems.STR_JEWEL != null && SlashItems.STR_JEWEL.get() != null) {
                    jewelItems.add(new ItemStack(SlashItems.STR_JEWEL.get()));
                }
                if (SlashItems.INT_JEWEL != null && SlashItems.INT_JEWEL.get() != null) {
                    jewelItems.add(new ItemStack(SlashItems.INT_JEWEL.get()));
                }
                if (SlashItems.WATCHER_EYE_JEWEL != null && SlashItems.WATCHER_EYE_JEWEL.get() != null) {
                    jewelItems.add(new ItemStack(SlashItems.WATCHER_EYE_JEWEL.get()));
                }
                if (SlashItems.CRAFTED_UNIQUE_JEWEL != null && SlashItems.CRAFTED_UNIQUE_JEWEL.get() != null) {
                    jewelItems.add(new ItemStack(SlashItems.CRAFTED_UNIQUE_JEWEL.get()));
                }
            } catch (Exception ignored) {
            }

            // 2. Iterate through all Affixes
            List<Affix> affixes = ExileDB.Affixes().getList();
            for (Affix affix : affixes) {
                if (affix == null || !affix.isRegistryEntryValid()) {
                    continue;
                }
                if (affix.getHideFromWiki() != null && affix.getHideFromWiki()) {
                    continue;
                }

                List<ItemStack> matchedItems = new ArrayList<>();
                Set<Item> addedItems = new HashSet<>();

                // Check matches for each BaseGearType
                for (Map.Entry<BaseGearType, List<ItemStack>> entry : gearTypeItemMap.entrySet()) {
                    BaseGearType gearType = entry.getKey();
                    try {
                        GearRequestedFor requested = new GearRequestedFor(gearType);
                        if (affix.meetsRequirements(requested)) {
                            for (ItemStack st : entry.getValue()) {
                                if (addedItems.add(st.getItem())) {
                                    matchedItems.add(st);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                // Check jewel slots
                if (affix.type == Affix.AffixSlot.jewel
                        || affix.type == Affix.AffixSlot.crafted_jewel_unique
                        || affix.type == Affix.AffixSlot.watcher_eye
                        || affix.type == Affix.AffixSlot.jewel_corruption) {
                    for (ItemStack ji : jewelItems) {
                        if (addedItems.add(ji.getItem())) {
                            matchedItems.add(ji);
                        }
                    }
                }

                if (matchedItems.isEmpty()) {
                    continue;
                }

                AffixRecipe recipe = new AffixRecipe(affix, matchedItems);
                result.all.add(recipe);

                if (affix.type == Affix.AffixSlot.prefix || affix.type == Affix.AffixSlot.tool) {
                    result.prefixes.add(recipe);
                } else if (affix.type == Affix.AffixSlot.suffix) {
                    result.suffixes.add(recipe);
                } else if (affix.type == Affix.AffixSlot.implicit) {
                    result.implicits.add(recipe);
                } else {
                    result.enchants.add(recipe);
                }
            }

            // Sort alphabetically
            Comparator<AffixRecipe> comp = Comparator.comparing(r -> r.getDisplayName().getString());
            result.prefixes.sort(comp);
            result.suffixes.sort(comp);
            result.implicits.sort(comp);
            result.enchants.sort(comp);
            result.all.sort(comp);

        } catch (Exception ignored) {
        }

        return result;
    }
}
