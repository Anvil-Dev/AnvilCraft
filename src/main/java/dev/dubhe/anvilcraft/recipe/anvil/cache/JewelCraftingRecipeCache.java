package dev.dubhe.anvilcraft.recipe.anvil.cache;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.network.RecipeCacheSyncPacket;
import dev.dubhe.anvilcraft.recipe.JewelCraftingRecipe;
import net.minecraft.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public class JewelCraftingRecipeCache {
    private Map<Item, RecipeHolder<JewelCraftingRecipe>> jewelCraftingCache;

    public @Nullable RecipeHolder<JewelCraftingRecipe> getJewelRecipeByResult(ItemStack result) {
        return jewelCraftingCache.get(result.getItem());
    }

    public Set<Item> getAllJewelResultItem() {
        return jewelCraftingCache.keySet();
    }

    public void buildJewelCraftingCache(RecipeManager recipeManager) {
        jewelCraftingCache = recipeManager.getAllRecipesFor(ModRecipeTypes.JEWEL_CRAFTING_TYPE.get())
            .stream()
            .map(it -> Map.entry(it.value().result.getItem(), it))
            .collect(Util.toMap());
    }

    public void buildJewelCraftingCache(Map<ItemStack, RecipeHolder<JewelCraftingRecipe>> data) {
        jewelCraftingCache = data.entrySet()
            .stream()
            .map(it -> Map.entry(it.getKey().getItem(), it.getValue()))
            .collect(Util.toMap());
    }

    public RecipeCacheSyncPacket intoPacket() {
        return new RecipeCacheSyncPacket(
            jewelCraftingCache.entrySet()
                .stream()
                .map(it -> Map.entry(it.getKey().getDefaultInstance(), it.getValue()))
                .collect(Util.toMap())
        );
    }
}
