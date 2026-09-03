package com.example.exile_overlay.compat.jei;

import com.example.exile_overlay.ExileOverlayMod;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@JeiPlugin
public class ExileOverlayJeiPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_UID = new ResourceLocation(ExileOverlayMod.MOD_ID, "jei_plugin");

    public static final RecipeType<AffixRecipe> PREFIXES_TYPE =
            RecipeType.create(ExileOverlayMod.MOD_ID, "prefixes", AffixRecipe.class);
    public static final RecipeType<AffixRecipe> SUFFIXES_TYPE =
            RecipeType.create(ExileOverlayMod.MOD_ID, "suffixes", AffixRecipe.class);
    public static final RecipeType<AffixRecipe> IMPLICITS_TYPE =
            RecipeType.create(ExileOverlayMod.MOD_ID, "implicits", AffixRecipe.class);
    public static final RecipeType<AffixRecipe> ENCHANTS_TYPE =
            RecipeType.create(ExileOverlayMod.MOD_ID, "enchants", AffixRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        if (!JeiConfig.getInstance().isEnabled()) {
            return;
        }

        IGuiHelper helper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(
                new AffixRecipeCategory(
                        PREFIXES_TYPE,
                        Component.translatable("exile_overlay.jei.category.prefixes"),
                        new ItemStack(Items.DIAMOND_SWORD),
                        helper
                ),
                new AffixRecipeCategory(
                        SUFFIXES_TYPE,
                        Component.translatable("exile_overlay.jei.category.suffixes"),
                        new ItemStack(Items.DIAMOND_CHESTPLATE),
                        helper
                ),
                new AffixRecipeCategory(
                        IMPLICITS_TYPE,
                        Component.translatable("exile_overlay.jei.category.implicits"),
                        new ItemStack(Items.AMETHYST_SHARD),
                        helper
                ),
                new AffixRecipeCategory(
                        ENCHANTS_TYPE,
                        Component.translatable("exile_overlay.jei.category.enchants"),
                        new ItemStack(Items.ENCHANTED_BOOK),
                        helper
                )
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (!JeiConfig.getInstance().isEnabled()) {
            return;
        }

        AffixRecipeMaker.CategorizedRecipes recipes = AffixRecipeMaker.createRecipes();

        if (!recipes.prefixes.isEmpty()) {
            registration.addRecipes(PREFIXES_TYPE, recipes.prefixes);
        }
        if (!recipes.suffixes.isEmpty()) {
            registration.addRecipes(SUFFIXES_TYPE, recipes.suffixes);
        }
        if (!recipes.implicits.isEmpty()) {
            registration.addRecipes(IMPLICITS_TYPE, recipes.implicits);
        }
        if (!recipes.enchants.isEmpty()) {
            registration.addRecipes(ENCHANTS_TYPE, recipes.enchants);
        }
    }

    private static mezz.jei.api.runtime.IJeiRuntime jeiRuntime;

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        jeiRuntime = runtime;
    }

    @Override
    public void onRuntimeUnavailable() {
        jeiRuntime = null;
    }

    public static ItemStack getItemStackUnderMouse(double mouseX, double mouseY) {
        if (jeiRuntime == null) {
            return ItemStack.EMPTY;
        }

        try {
            var overlay = jeiRuntime.getIngredientListOverlay();
            if (overlay != null) {
                java.util.Optional<mezz.jei.api.ingredients.ITypedIngredient<?>> underMouse = overlay.getIngredientUnderMouse();
                if (underMouse.isPresent()) {
                    java.util.Optional<ItemStack> stack = underMouse.get().getItemStack();
                    if (stack.isPresent() && !stack.get().isEmpty()) {
                        return stack.get();
                    }
                }
            }

            var screenHelper = jeiRuntime.getScreenHelper();
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (screenHelper != null && mc.screen != null) {
                var clicked = screenHelper.getClickableIngredientUnderMouse(mc.screen, mouseX, mouseY).findFirst();
                if (clicked.isPresent()) {
                    mezz.jei.api.ingredients.ITypedIngredient<?> typed = clicked.get().getTypedIngredient();
                    java.util.Optional<ItemStack> stack = typed.getItemStack();
                    if (stack.isPresent() && !stack.get().isEmpty()) {
                        return stack.get();
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return ItemStack.EMPTY;
    }
}
