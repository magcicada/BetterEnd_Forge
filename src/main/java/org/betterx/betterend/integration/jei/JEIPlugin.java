package org.betterx.betterend.integration.jei;

import mezz.jei.api.helpers.IGuiHelper;
import org.betterx.bclib.recipes.AlloyingRecipe;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.blocks.basis.EndAnvilBlock;
import org.betterx.betterend.recipe.builders.InfusionRecipe;
import org.betterx.betterend.registry.EndBlocks;
import org.betterx.bclib.recipes.AnvilRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@JeiPlugin
public class JEIPlugin implements IModPlugin {
    public static final RecipeType<AlloyingDisplay> ALLOYING_RECIPE_TYPE =
            RecipeType.create(BetterEnd.MOD_ID, "alloying", AlloyingDisplay.class);

    public static final RecipeType<AnvilRecipe> ANVIL_RECIPE_TYPE =
            RecipeType.create(BetterEnd.MOD_ID, "anvil", AnvilRecipe.class);

    public static final RecipeType<InfusionDisplay> INFUSION_RECIPE_TYPE =
            RecipeType.create(BetterEnd.MOD_ID, "infusion", InfusionDisplay.class);
    public static List<ItemStack> ALLOYING_FUELS;

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return BetterEnd.makeID("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();

        registration.addRecipeCategories(new JEIAlloyingCategory(guiHelper));
        registration.addRecipeCategories(new JEIAnvilCategory(guiHelper));
        registration.addRecipeCategories(new JEIInfusionCategory(guiHelper));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();

        if (ALLOYING_FUELS == null || ALLOYING_FUELS.isEmpty()) {
            ALLOYING_FUELS = org.betterx.betterend.blocks.entities.EndStoneSmelterBlockEntity.availableFuels()
                    .keySet().stream().map(ItemStack::new).toList();
        }

        List<AlloyingRecipe> alloyingRecipes = manager.getAllRecipesFor(AlloyingRecipe.TYPE);
            List<AlloyingDisplay> alloyingDisplays = alloyingRecipes.stream()
                    .map(AlloyingDisplay::new)
                    .toList();
            registration.addRecipes(ALLOYING_RECIPE_TYPE, alloyingDisplays);
        var blastingRecipes = manager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.BLASTING);
            registration.addRecipes(ALLOYING_RECIPE_TYPE,
                    blastingRecipes.stream().map(AlloyingDisplay::new).toList());

        List<InfusionRecipe> infusionRecipes = manager.getAllRecipesFor(InfusionRecipe.TYPE);
            List<InfusionDisplay> infusionDisplays = infusionRecipes.stream()
                    .map(InfusionDisplay::new)
                    .toList();
            registration.addRecipes(INFUSION_RECIPE_TYPE, infusionDisplays);

        registration.addRecipes(ANVIL_RECIPE_TYPE, manager.getAllRecipesFor(AnvilRecipe.TYPE));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        List<ItemStack> anvils = EndBlocks.getModBlocks()
                .stream()
                .filter(EndAnvilBlock.class::isInstance)
                .map(Block::asItem)
                .map(ItemStack::new)
                .toList();

        registration.addRecipeCatalyst(new ItemStack(Blocks.ANVIL), ANVIL_RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(EndBlocks.END_STONE_SMELTER), ALLOYING_RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(EndBlocks.INFUSION_PEDESTAL), INFUSION_RECIPE_TYPE);

        for (ItemStack stack : anvils) {
            registration.addRecipeCatalyst(stack, ANVIL_RECIPE_TYPE);
        }
    }
}
