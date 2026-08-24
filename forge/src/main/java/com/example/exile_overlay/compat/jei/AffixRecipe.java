package com.example.exile_overlay.compat.jei;

import com.robertx22.mine_and_slash.database.data.StatMod;
import com.robertx22.mine_and_slash.database.data.affixes.Affix;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AffixRecipe {
    private final Affix affix;
    private final Affix.AffixSlot slot;
    private final String guid;
    private final Component displayName;
    private final int weight;
    private final List<ItemStack> applicableItems;
    private final List<Component> statLines;
    private final List<Component> requirementLines;

    public AffixRecipe(Affix affix, List<ItemStack> applicableItems) {
        this.affix = affix;
        this.slot = affix.type;
        this.guid = affix.GUID();
        this.displayName = affix.locName();
        this.weight = affix.weight;
        this.applicableItems = Collections.unmodifiableList(new ArrayList<>(applicableItems));

        // Stat lines
        List<Component> stats = new ArrayList<>();
        if (affix.getStats() != null) {
            for (StatMod mod : affix.getStats()) {
                if (mod != null) {
                    try {
                        stats.addAll(mod.getEstimationTooltip(1));
                    } catch (Exception ignored) {
                        try {
                            MutableComponent range = mod.getRangeToShow(1);
                            String statName = mod.GetStat() != null ? mod.GetStat().locName().getString() : mod.stat;
                            stats.add(range.append(" ").append(statName));
                        } catch (Exception e2) {
                            stats.add(Component.literal(mod.stat + " (" + mod.min + " -> " + mod.max + ")"));
                        }
                    }
                }
            }
        }
        this.statLines = Collections.unmodifiableList(stats);

        // Requirement lines
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

    public Affix.AffixSlot getSlot() {
        return slot;
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

    public List<ItemStack> getApplicableItems() {
        return applicableItems;
    }

    public List<Component> getStatLines() {
        return statLines;
    }

    public List<Component> getRequirementLines() {
        return requirementLines;
    }
}
