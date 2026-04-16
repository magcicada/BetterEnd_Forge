package org.betterx.datagen.betterend.recipes;

import org.betterx.bclib.api.v3.datagen.RecipeDataProvider;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.recipe.*;

import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EndRecipeDataProvider extends RecipeDataProvider {
    public EndRecipeDataProvider(PackOutput output) {
        super(List.of(BetterEnd.MOD_ID), output);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        Set<ResourceLocation> seenRecipes = new HashSet<>();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        this.buildRecipes((FinishedRecipe recipe) -> {
            ResourceLocation id = recipe.getId();
            if (!seenRecipes.add(id)) {
                throw new IllegalStateException("Duplicate recipe " + id);
            }

            JsonObject recipeJson = recipe.serializeRecipe();
            if (!RecipeCompatFixer.fix(id, recipeJson)) {
                BetterEnd.LOGGER.warning("[datagen] skipped recipe " + id + " because empty ingredients could not be fixed");
                return;
            }

            futures.add(DataProvider.saveStable(cachedOutput, recipeJson, this.recipePathProvider.json(id)));

            JsonObject advancementJson = recipe.serializeAdvancement();
            if (advancementJson != null) {
                CompletableFuture<?> advancementFuture = this.saveAdvancement(cachedOutput, recipe, advancementJson);
                if (advancementFuture != null) {
                    futures.add(advancementFuture);
                }
            }
        });

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public static void buildRecipes() {
        RecipeDataProvider.defer(EndRecipeDataProvider::registerRecipes);
    }

    private static void registerRecipes() {
        CraftingRecipes.register();
        FurnaceRecipes.register();
        AlloyingRecipes.register();
        AnvilRecipes.register();
        SmithingRecipes.register();
        InfusionRecipes.register();
    }
}
