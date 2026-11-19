package org.betterx.datagen.betterend.recipes;

import org.betterx.bclib.api.v3.datagen.RecipeDataProvider;
import org.betterx.betterend.BetterEnd;
import org.betterx.betterend.recipe.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    private final PackOutput.PathProvider guideAdvancementPath;
    public EndRecipeDataProvider(PackOutput output) {
        super(List.of(BetterEnd.MOD_ID), output);
        guideAdvancementPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancements");
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
            addPatchouliCondition(id, recipeJson);
            if (!RecipeCompatFixer.fix(id, recipeJson)) {
                BetterEnd.LOGGER.warning("[datagen] skipped recipe " + id + " because empty ingredients could not be fixed");
                return;
            }

            futures.add(DataProvider.saveStable(cachedOutput, recipeJson, this.recipePathProvider.json(id)));

            JsonObject advancementJson = recipe.serializeAdvancement();
            if (advancementJson != null) {
                addPatchouliCondition(id, advancementJson);
                CompletableFuture<?> advancementFuture = this.saveAdvancement(cachedOutput, recipe, advancementJson);
                if (advancementFuture != null) {
                    futures.add(advancementFuture);
                }
            }
        });

        // The release contains this conditional recipe even though no builder supplies it.
        // Keep it in the provider output so HashCache cannot remove it as a stale file.
        ResourceLocation guideId = BetterEnd.makeID("guide_book");
        if (!seenRecipes.contains(guideId)) {
            futures.add(DataProvider.saveStable(cachedOutput, guideTemplate("guide_book_recipe.json"), recipePathProvider.json(guideId)));
            futures.add(DataProvider.saveStable(cachedOutput, guideTemplate("guide_book_advancement.json"),
                    guideAdvancementPath.json(BetterEnd.makeID("recipes/misc/guide_book"))));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject guideTemplate(String file) {
        String path = "/betterend/datagen/" + file;
        try (InputStream input = BetterEnd.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing datagen template " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read datagen template " + path, e);
        }
    }

    private static void addPatchouliCondition(ResourceLocation id, JsonObject json) {
        if (id.equals(BetterEnd.makeID("guide_book"))) {
            JsonObject condition = new JsonObject();
            condition.addProperty("type", "forge:mod_loaded");
            condition.addProperty("modid", "patchouli");
            JsonArray conditions = new JsonArray();
            conditions.add(condition);
            json.add("forge:conditions", conditions);
        }
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
