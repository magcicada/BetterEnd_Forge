package org.betterx.datagen.betterend.recipes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Fixes recipe JSON emitted by datagen when optional ingredients resolve to empty arrays in 1.20.1.
 */
public final class RecipeCompatFixer {
    private static final Set<String> STONE_BASES = Set.of(
            "azure_jadestone",
            "flavolite",
            "sandy_jadestone",
            "smaragdant_crystal",
            "sulphuric_rock",
            "umbralith",
            "violecite",
            "virid_jadestone"
    );

    private static final Set<String> LANTERN_BASES = Set.of(
            "azure_jadestone",
            "flavolite",
            "sandy_jadestone",
            "sulphuric_rock",
            "umbralith",
            "violecite",
            "virid_jadestone"
    );

    private RecipeCompatFixer() {
    }

    public static boolean fix(ResourceLocation id, JsonObject recipe) {
        JsonObject key = asObject(recipe.get("key"));
        if (key == null || key.entrySet().isEmpty()) {
            return true;
        }

        String path = id.getPath();
        for (String symbol : key.keySet().toArray(String[]::new)) {
            JsonElement ingredient = key.get(symbol);
            if (!isEmptyArrayIngredient(ingredient)) {
                continue;
            }

            JsonElement replacement = replacementFor(path, symbol);
            if (replacement == null) {
                return false;
            }
            key.add(symbol, replacement);
        }

        return true;
    }

    private static JsonElement replacementFor(String path, String symbol) {
        if ("#".equals(symbol)) {
            if ("smaragdant_crystal_stairs".equals(path)
                    || "smaragdant_crystal_wall".equals(path)
                    || "smaragdant_crystal_bricks".equals(path)) {
                return item("betterend:smaragdant_crystal");
            }
            if (path.endsWith("_pillar")) {
                String base = path.substring(0, path.length() - "_pillar".length());
                if (STONE_BASES.contains(base)) {
                    return item("betterend:" + base + "_slab");
                }
            }
            return null;
        }

        if ("S".equals(symbol)) {
            if (path.endsWith("_pedestal")) {
                String base = path.substring(0, path.length() - "_pedestal".length());
                if (STONE_BASES.contains(base)) {
                    return item("betterend:" + base + "_slab");
                }
            }
            if (path.endsWith("_lantern")) {
                String base = path.substring(0, path.length() - "_lantern".length());
                if (LANTERN_BASES.contains(base)) {
                    return alternatives(
                            "betterend:" + base + "_slab",
                            "betterend:" + base + "_bricks_slab"
                    );
                }
            }
            return null;
        }

        return null;
    }

    private static JsonObject item(String itemId) {
        JsonObject obj = new JsonObject();
        obj.addProperty("item", itemId);
        return obj;
    }

    private static JsonArray alternatives(String firstItemId, String secondItemId) {
        JsonArray array = new JsonArray();
        array.add(item(firstItemId));
        array.add(item(secondItemId));
        return array;
    }

    private static boolean isEmptyArrayIngredient(JsonElement element) {
        return element != null && element.isJsonArray() && element.getAsJsonArray().isEmpty();
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
