package com.example.exile_overlay.compat.jei;

import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.gear_types.bases.BaseGearType;
import com.robertx22.mine_and_slash.database.data.requirements.bases.GearRequestedFor;
import com.robertx22.mine_and_slash.database.registry.ExileDB;
import com.robertx22.mine_and_slash.mmorpg.registers.common.items.SlashItems;
import com.robertx22.mine_and_slash.uncommon.interfaces.data_items.IRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class AffixRecipeMaker {

    public static final int ENTRIES_PER_PAGE = 7;

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

                if (!items.isEmpty()) {
                    gearTypeItemMap.put(gearType, items);
                }
            }

            // 2. Pre-filter all valid Affixes
            List<Affix> allAffixes = new ArrayList<>();
            for (Affix affix : ExileDB.Affixes().getList()) {
                if (affix == null || !affix.isRegistryEntryValid()) {
                    continue;
                }
                if (affix.getHideFromWiki() != null && affix.getHideFromWiki()) {
                    continue;
                }
                allAffixes.add(affix);
            }

            Comparator<AffixRecipe.AffixEntry> entryComp = Comparator.comparing(e -> {
                if (!e.getEffectNames().isEmpty()) {
                    return e.getEffectNames().get(0).getString();
                }
                return e.getDisplayName().getString();
            });

            // 3. For each BaseGearType, collect matching affixes by slot type
            for (Map.Entry<BaseGearType, List<ItemStack>> entry : gearTypeItemMap.entrySet()) {
                BaseGearType gearType = entry.getKey();
                List<ItemStack> items = entry.getValue();
                Component typeName = gearType.locName();

                List<AffixRecipe.AffixEntry> prefixEntries = new ArrayList<>();
                List<AffixRecipe.AffixEntry> suffixEntries = new ArrayList<>();
                List<AffixRecipe.AffixEntry> implicitEntries = new ArrayList<>();
                List<AffixRecipe.AffixEntry> enchantEntries = new ArrayList<>();

                for (Affix affix : allAffixes) {
                    if (affix.type == null) continue;

                    try {
                        GearRequestedFor requested = new GearRequestedFor(gearType);
                        if (affix.meetsRequirements(requested)) {
                            AffixRecipe.AffixEntry affixEntry = new AffixRecipe.AffixEntry(affix);

                            if (affix.type == Affix.AffixSlot.prefix || affix.type == Affix.AffixSlot.tool) {
                                prefixEntries.add(affixEntry);
                            } else if (affix.type == Affix.AffixSlot.suffix) {
                                suffixEntries.add(affixEntry);
                            } else if (affix.type == Affix.AffixSlot.implicit) {
                                implicitEntries.add(affixEntry);
                            } else if (affix.type == Affix.AffixSlot.enchant) {
                                enchantEntries.add(affixEntry);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                }

                prefixEntries.sort(entryComp);
                suffixEntries.sort(entryComp);
                implicitEntries.sort(entryComp);
                enchantEntries.sort(entryComp);

                createPagedRecipes(typeName, items, Affix.AffixSlot.prefix, prefixEntries, result.prefixes, result.all);
                createPagedRecipes(typeName, items, Affix.AffixSlot.suffix, suffixEntries, result.suffixes, result.all);
                createPagedRecipes(typeName, items, Affix.AffixSlot.implicit, implicitEntries, result.implicits, result.all);
                createPagedRecipes(typeName, items, Affix.AffixSlot.enchant, enchantEntries, result.enchants, result.all);
            }

            // Sort recipes alphabetically by GearType name
            Comparator<AffixRecipe> recipeComp = Comparator.comparing(r -> r.getGearTypeName().getString());
            result.prefixes.sort(recipeComp);
            result.suffixes.sort(recipeComp);
            result.implicits.sort(recipeComp);
            result.enchants.sort(recipeComp);
            result.all.sort(recipeComp);

        } catch (Exception ignored) {
        }

        return result;
    }

    private static void createPagedRecipes(Component gearTypeName, List<ItemStack> items, Affix.AffixSlot slot,
                                           List<AffixRecipe.AffixEntry> allEntries,
                                           List<AffixRecipe> targetCategoryList, List<AffixRecipe> allList) {
        if (allEntries.isEmpty()) return;

        int totalCount = allEntries.size();
        int totalPages = (int) Math.ceil((double) totalCount / ENTRIES_PER_PAGE);

        for (int p = 0; p < totalPages; p++) {
            int from = p * ENTRIES_PER_PAGE;
            int to = Math.min(from + ENTRIES_PER_PAGE, totalCount);
            List<AffixRecipe.AffixEntry> pageEntries = allEntries.subList(from, to);

            AffixRecipe recipe = new AffixRecipe(gearTypeName, items, slot, pageEntries, p + 1, totalPages, totalCount);
            targetCategoryList.add(recipe);
            allList.add(recipe);
        }
    }
}


