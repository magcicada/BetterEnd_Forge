package org.betterx.betterend.integration.jei;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.betterx.bclib.recipes.AlloyingRecipe;

public class AlloyingDisplay {
    public final Recipe<?> recipe;
    public final float experience;
    public final int time;

    public AlloyingDisplay(AlloyingRecipe recipe) {
        this.recipe = recipe;
        this.experience = recipe.getExperience();
        this.time = recipe.getSmeltTime();
    }

    public AlloyingDisplay(AbstractCookingRecipe recipe) {
        this.recipe = recipe;
        this.experience = recipe.getExperience();
        this.time = recipe.getCookingTime();
    }

    public ResourceLocation getId() {
        return recipe.getId();
    }
}
