package org.betterx.betterend.integration.jei;

import net.minecraft.resources.ResourceLocation;
import org.betterx.betterend.recipe.builders.InfusionRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.List;

public class InfusionDisplay {
    public final InfusionRecipe recipe;
    public final int time;

    public InfusionDisplay(InfusionRecipe recipe) {
        this.recipe = recipe;
        this.time = recipe.getInfusionTime();
    }

    public List<Ingredient> getIngredients() {
        return recipe.getIngredients();
    }

    public ResourceLocation getId() {
        return recipe.getId();
    }
}
