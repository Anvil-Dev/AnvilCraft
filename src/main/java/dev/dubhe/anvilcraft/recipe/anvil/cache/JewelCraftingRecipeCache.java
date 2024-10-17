package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class JewelCraftingRecipeCache {
    private final RecipeManager recipeManager;
    private Object2ObjectMap<Item, RecipeHolder<JewelCraftingRecipe>> jewelCraftingCache;

    public JewelCraftingRecipeCache(RecipeManager recipeManager) {
        this.recipeManager = recipeManager;
    }

    public @Nullable RecipeHolder<JewelCraftingRecipe> getJewelRecipeByResult(ItemStack result) {
        if (jewelCraftingCache == null) {
            buildJewelCraftingCache();
        }
        return jewelCraftingCache.get(result.getItem());
    }

    public Set<Item> getAllJewelResultItem() {
        if (jewelCraftingCache == null) {
            buildJewelCraftingCache();
        }
        return jewelCraftingCache.keySet();
    }

    private void buildJewelCraftingCache() {
        jewelCraftingCache = new Object2ObjectOpenHashMap<>();
        for (RecipeHolder<JewelCraftingRecipe> recipe : recipeManager.getAllRecipesFor(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get())) {
            jewelCraftingCache.put(recipe.value().result.getItem(), recipe);
        }
    }
}
