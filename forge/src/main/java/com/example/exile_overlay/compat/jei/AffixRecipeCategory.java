package com.example.exile_overlay.compat.jei;

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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AffixRecipeCategory implements IRecipeCategory<AffixRecipe> {

    public static final int WIDTH = 168;
    public static final int HEIGHT = 125;
    public static final int SLOTS_COUNT = 9;

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
        List<ItemStack> items = recipe.getApplicableItems();
        if (items.isEmpty()) return;

        int slotY = 100;
        int itemsPerSlot = Math.max(1, (int) Math.ceil((double) items.size() / SLOTS_COUNT));

        for (int i = 0; i < SLOTS_COUNT; i++) {
            int slotX = i * 18 + 3;
            int fromIdx = i * itemsPerSlot;
            int toIdx = Math.min(fromIdx + itemsPerSlot, items.size());

            if (fromIdx < items.size()) {
                List<ItemStack> subList = items.subList(fromIdx, toIdx);
                builder.addSlot(RecipeIngredientRole.INPUT, slotX + 1, slotY + 1)
                        .setBackground(slotBackground, -1, -1)
                        .addItemStacks(subList);
            } else {
                break;
            }
        }
    }

    @Override
    public void draw(AffixRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        // 1. Header: Affix display name
        Component name = recipe.getDisplayName();
        guiGraphics.drawString(font, name, 4, 3, 0x00FFFF, false);

        // 2. Header right: Weight
        String weightText = "Weight: " + recipe.getWeight();
        int weightWidth = font.width(weightText);
        guiGraphics.drawString(font, weightText, WIDTH - weightWidth - 4, 3, 0x888888, false);

        // 3. Slot badge & ID
        String slotBadge = "[" + recipe.getSlot().name() + "]";
        int badgeColor = switch (recipe.getSlot()) {
            case prefix -> 0x55AAFF;
            case suffix -> 0xFFAA00;
            case implicit -> 0xCC77FF;
            default -> 0x55FF55;
        };
        guiGraphics.drawString(font, slotBadge, 4, 15, badgeColor, false);

        String idText = "ID: " + recipe.getGuid();
        int idWidth = font.width(idText);
        if (WIDTH - idWidth - 4 > font.width(slotBadge) + 10) {
            guiGraphics.drawString(font, idText, WIDTH - idWidth - 4, 15, 0x555555, false);
        }

        // Horizontal separator line
        guiGraphics.fill(4, 26, WIDTH - 4, 27, 0x33FFFFFF);

        // 4. Stats section
        int y = 30;
        List<Component> stats = recipe.getStatLines();
        for (Component statLine : stats) {
            if (y > 66) break;
            List<FormattedText> lines = font.getSplitter().splitLines(statLine, WIDTH - 8, statLine.getStyle());
            for (FormattedText line : lines) {
                if (y > 66) break;
                guiGraphics.drawString(font, line.getString(), 6, y, 0xFFFFAA, false);
                y += 10;
            }
        }

        // 5. Requirements & limits
        int reqY = 70;
        List<Component> reqs = recipe.getRequirementLines();
        if (!reqs.isEmpty()) {
            Component firstReq = reqs.get(0);
            guiGraphics.drawString(font, firstReq.getString(), 6, reqY, 0xAAAAAA, false);
        }

        // 6. Section header: Applicable Gears
        Component gearLabel = Component.translatable("exile_overlay.jei.applicable_gears")
                .withStyle(ChatFormatting.GRAY);
        guiGraphics.drawString(font, gearLabel, 4, 88, 0xAAAAAA, false);
    }

    @Override
    public List<Component> getTooltipStrings(AffixRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        List<Component> tooltip = new ArrayList<>();

        if (mouseY >= 28 && mouseY <= 85 && mouseX >= 4 && mouseX <= WIDTH - 4) {
            tooltip.add(recipe.getDisplayName().copy().withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            tooltip.add(Component.literal("ID: " + recipe.getGuid()).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.literal("Slot: " + recipe.getSlot().name()).withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("Weight: " + recipe.getWeight()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.empty());

            tooltip.add(Component.literal("Stats:").withStyle(ChatFormatting.WHITE));
            for (Component stat : recipe.getStatLines()) {
                tooltip.add(stat);
            }

            if (!recipe.getRequirementLines().isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("Requirements:").withStyle(ChatFormatting.WHITE));
                for (Component req : recipe.getRequirementLines()) {
                    tooltip.add(req);
                }
            }
        }

        return tooltip;
    }
}
