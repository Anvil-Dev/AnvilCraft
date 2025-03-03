package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.MeshRecipe;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MeshRecipeCache {
    private final RecipeManager recipeManager;
    private Map<Item, List<RecipeHolder<MeshRecipe>>> meshCaches;

    public MeshRecipeCache(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    @Nullable
    public List<RecipeHolder<MeshRecipe>> getMeshRecipes(ItemStack stack) {
        if (meshCaches == null) {
            buildRecipeCache();
        }
        return meshCaches.get(stack.getItem());
    }

    private void buildRecipeCache() {
        meshCaches = new HashMap<>();
        for (RecipeHolder<MeshRecipe> recipeHolder : recipeManager.getAllRecipesFor(ModRecipeTypes.MESH_TYPE.get())) {
            MeshRecipe recipe = recipeHolder.value();
            for (ItemStack stack : recipe.getInput().getItems()) {
                meshCaches
                    .computeIfAbsent(stack.getItem(), k -> new ArrayList<>())
                    .add(recipeHolder);
            }
        }
    }
}
