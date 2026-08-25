package com.example.exile_overlay.compat.jei;

import com.robertx22.mine_and_slash.database.data.rarities.GearRarity;
import com.robertx22.mine_and_slash.database.data.rarities.GearRarityType;
import com.robertx22.mine_and_slash.itemstack.ExileStack;
import com.robertx22.mine_and_slash.itemstack.StackKeys;
import com.robertx22.mine_and_slash.saveclasses.item_classes.GearItemData;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AffixRecipeCategory implements IRecipeCategory<AffixRecipe> {

    public static final int WIDTH = 176;
    public static final int HEIGHT = 130;
    private static final int LIST_TOP = 28;
    private static final int ROW_HEIGHT = 14;

    private final RecipeType<AffixRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable slotBackground;

    public AffixRecipeCategory(RecipeType<AffixRecipe> recipeType, Component title, ItemStack iconStack, IGuiHelper guiHelper) {
        this.recipeType = recipeType;
        this.title = title;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(iconStack);
        this.slotBackground = guiHelper.getSlotDrawable();
    }

    @Override
    public RecipeType<AffixRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AffixRecipe recipe, IFocusGroup focuses) {
        if (recipe.getInputItems().isEmpty()) return;

        // Exclude Runed and Unique gear from affix recipe matching
        boolean isExcludedFocus = focuses.getAllFocuses().stream().anyMatch(focus -> {
            Object val = focus.getTypedValue().getIngredient();
            if (val instanceof ItemStack stack) {
                return isRunedOrUnique(stack);
            }
            return false;
        });

        if (isExcludedFocus) {
            return;
        }

        builder.addSlot(RecipeIngredientRole.INPUT, 5, 4)
                .setBackground(slotBackground, -1, -1)
                .addItemStacks(recipe.getInputItems());
    }

    private static boolean isRunedOrUnique(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        try {
            ExileStack exStack = ExileStack.of(stack);
            if (exStack.get(StackKeys.GEAR).has()) {
                GearItemData gear = exStack.get(StackKeys.GEAR).get();
                if (gear != null) {
                    GearRarity rarity = gear.getRarity();
                    if (rarity != null) {
                        if (rarity.type == GearRarityType.RUNED
                                || rarity.type == GearRarityType.UNIQUE
                                || rarity.is_unique_item
                                || rarity.can_have_runewords) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    @Override
    public void draw(AffixRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // 1. Header: Gear Type Name
        Component gearName = recipe.getGearTypeName();
        guiGraphics.drawString(font, gearName, 28, 4, 0xFFFFFF, false);

        // Subtitle: Slot badge & Page/Affix count
        String slotBadge = "[" + recipe.getSlot().name() + "]";
        int badgeColor = switch (recipe.getSlot()) {
            case prefix -> 0x55AAFF;
            case suffix -> 0xFFAA00;
            case implicit -> 0xCC77FF;
            default -> 0x55FF55;
        };
        guiGraphics.drawString(font, slotBadge, 28, 14, badgeColor, false);

        int startIdx = (recipe.getPageIndex() - 1) * AffixRecipeMaker.ENTRIES_PER_PAGE + 1;
        int endIdx = startIdx + recipe.getEntries().size() - 1;
        String countText = "(" + startIdx + "-" + endIdx + " / " + recipe.getTotalAffixesCount() + ")";
        guiGraphics.drawString(font, countText, 28 + font.width(slotBadge) + 4, 14, 0x888888, false);

        // Separator line
        guiGraphics.fill(4, 25, WIDTH - 4, 26, 0x33FFFFFF);

        // 2. Affix List (Only effect names, uniform clean rows)
        List<AffixRecipe.AffixEntry> entries = recipe.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            AffixRecipe.AffixEntry entry = entries.get(i);
            int rowY = LIST_TOP + i * ROW_HEIGHT;

            // Hover highlight
            if (mouseX >= 4 && mouseX <= WIDTH - 4 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                guiGraphics.fill(4, rowY, WIDTH - 4, rowY + ROW_HEIGHT, 0x22FFFFFF);
            }

            // Bullet icon
            guiGraphics.drawString(font, "•", 6, rowY + 3, 0x777777, false);

            // Effect name only (truncated with ellipsis if too long)
            Component effectName = !entry.getEffectNames().isEmpty()
                    ? entry.getEffectNames().get(0)
                    : entry.getDisplayName();
            Component truncated = truncateText(font, effectName, WIDTH - 22);
            guiGraphics.drawString(font, truncated, 14, rowY + 3, 0xFFFFFF, false);
        }
    }

    private static Component truncateText(Font font, Component text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String str = text.getString();
        String ellipsis = "...";
        int ellipsisWidth = font.width(ellipsis);
        int availableWidth = maxWidth - ellipsisWidth;
        if (availableWidth <= 0) {
            return text;
        }
        String truncated = font.plainSubstrByWidth(str, availableWidth);
        return Component.literal(truncated + ellipsis).withStyle(text.getStyle());
    }

    @Override
    public List<Component> getTooltipStrings(AffixRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();

        if (mouseX < 4 || mouseX > WIDTH - 4 || mouseY < LIST_TOP) {
            return tooltip;
        }

        int index = (int) ((mouseY - LIST_TOP) / ROW_HEIGHT);
        List<AffixRecipe.AffixEntry> entries = recipe.getEntries();

        if (index >= 0 && index < entries.size()) {
            AffixRecipe.AffixEntry entry = entries.get(index);

            // Title: Affix Name & Slot
            String slotName = recipe.getSlot().name();
            tooltip.add(entry.getDisplayName().copy().append(" (" + slotName + ")").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            tooltip.add(Component.literal("ID: " + entry.getGuid()).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Weight: " + entry.getWeight()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.empty());

            // Stats / Min-Max Range
            tooltip.add(Component.literal("Stats / Range:").withStyle(ChatFormatting.WHITE));
            for (Component stat : entry.getRawStatLines()) {
                tooltip.add(Component.literal(" • ").withStyle(ChatFormatting.GRAY).append(stat));
            }

            // Requirements
            if (!entry.getRequirementLines().isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Requirements:").withStyle(ChatFormatting.WHITE));
                for (Component req : entry.getRequirementLines()) {
                    tooltip.add(req);
                }
            }
        }

        return tooltip;
    }
}



