package com.example.exile_overlay.compat.jei;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import com.robertx22.mine_and_slash.database.data.stats.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AffixRecipe {

    public static class AffixEntry {
        private final Affix affix;
        private final String guid;
        private final Component displayName;
        private final int weight;
        private final List<Component> effectNames;
        private final List<Component> rawStatLines;
        private final List<Component> requirementLines;

        public AffixEntry(Affix affix) {
            this.affix = affix;
            this.guid = affix.GUID();
            this.displayName = affix.locName();
            this.weight = affix.weight;

            List<Component> effects = new ArrayList<>();
            List<Component> rawStats = new ArrayList<>();

            if (affix.getStats() != null) {
                for (StatMod mod : affix.getStats()) {
                    if (mod != null) {
                        try {
                            List<Component> est = mod.getEstimationTooltip(1);
                            rawStats.addAll(est);

                            Stat stat = mod.GetStat();
                            if (stat != null) {
                                effects.add(cleanEffectName(stat.locName()));
                            } else {
                                effects.add(cleanEffectName(Component.literal(mod.stat)));
                            }
                        } catch (Exception ignored) {
                            try {
                                Stat stat = mod.GetStat();
                                Component name = cleanEffectName((stat != null) ? stat.locName() : Component.literal(mod.stat));
                                effects.add(name);
                                rawStats.add(mod.getRangeToShow(1).append(" ").append(name));
                            } catch (Exception e2) {
                                Component fallback = Component.literal(mod.stat);
                                effects.add(fallback);
                                rawStats.add(Component.literal(mod.stat + " (" + mod.min + " -> " + mod.max + ")"));
                            }
                        }
                    }
                }
            }
            this.effectNames = Collections.unmodifiableList(effects);
            this.rawStatLines = Collections.unmodifiableList(rawStats);

            List<Component> reqs = new ArrayList<>();
            if (affix.requirements() != null) {
                try {
                    reqs.addAll(affix.requirements().GetTooltipString());
                } catch (Exception ignored) {
                }
            }
            if (affix.only_one_per_item) {
                reqs.add(Component.translatable("exile_overlay.jei.only_one_per_item")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            if (affix.one_of_a_kind != null && !affix.one_of_a_kind.isEmpty()) {
                reqs.add(Component.translatable("exile_overlay.jei.one_of_a_kind", affix.one_of_a_kind)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            this.requirementLines = Collections.unmodifiableList(reqs);
        }

        public Affix getAffix() {
            return affix;
        }

        public String getGuid() {
            return guid;
        }

        public Component getDisplayName() {
            return displayName;
        }

        public int getWeight() {
            return weight;
        }

        public List<Component> getEffectNames() {
            return effectNames;
        }

        public List<Component> getRawStatLines() {
            return rawStatLines;
        }

        public List<Component> getRequirementLines() {
            return requirementLines;
        }

        private static Component cleanEffectName(Component raw) {
            if (raw == null) return Component.empty();
            String str = raw.getString();
            str = str.replace("[VAL1]%", "")
                     .replace("[VAL1]", "")
                     .trim();
            if (str.startsWith("の確率で")) {
                str = str.substring("の確率で".length()).trim();
            }
            return Component.literal(str).withStyle(raw.getStyle());
        }
    }

    private final Component gearTypeName;
    private final List<ItemStack> inputItems;
    private final Affix.AffixSlot slot;
    private final List<AffixEntry> entries;
    private final int pageIndex;
    private final int totalPages;
    private final int totalAffixesCount;

    public AffixRecipe(Component gearTypeName, List<ItemStack> inputItems, Affix.AffixSlot slot,
                       List<AffixEntry> entries, int pageIndex, int totalPages, int totalAffixesCount) {
        this.gearTypeName = gearTypeName;
        this.inputItems = Collections.unmodifiableList(new ArrayList<>(inputItems));
        this.slot = slot;
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.totalAffixesCount = totalAffixesCount;
    }

    public Component getGearTypeName() {
        return gearTypeName;
    }

    public List<ItemStack> getInputItems() {
        return inputItems;
    }

    public Affix.AffixSlot getSlot() {
        return slot;
    }

    public List<AffixEntry> getEntries() {
        return entries;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getTotalAffixesCount() {
        return totalAffixesCount;
    }
}



