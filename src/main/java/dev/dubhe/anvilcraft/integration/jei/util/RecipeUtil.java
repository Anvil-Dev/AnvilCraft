package dev.dubhe.anvilcraft.integration.jei.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public class RecipeUtil {
    public static <I extends RecipeInput, T extends Recipe<I>> List<T> getRecipesFromType(RecipeType<T> recipeType) {
        return Minecraft.getInstance().getConnection().getRecipeManager().getAllRecipesFor(recipeType).stream()
                .map(RecipeHolder::value)
                .toList();
    }
}
