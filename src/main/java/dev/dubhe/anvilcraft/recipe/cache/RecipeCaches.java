package dev.dubhe.anvilcraft.recipe.cache;

import dev.dubhe.anvilcraft.recipe.MeshRecipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RecipeCaches {
    private static MeshRecipeCache meshRecipeCache;

    public static void reload(RecipeManager recipeManager) {
        meshRecipeCache = new MeshRecipeCache(recipeManager);
    }

    public static void unload() {
        meshRecipeCache = null;
    }

    @Nullable public static List<RecipeHolder<MeshRecipe>> getCacheMeshRecipes(ItemStack stack) {
        return meshRecipeCache.getMeshRecipes(stack);
    }
}
