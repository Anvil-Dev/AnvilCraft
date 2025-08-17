package dev.dubhe.anvilcraft.util.injection;

import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public interface IRecipeManager {
    default void anc$setInWorldRecipeManager(InWorldRecipeManager manager) {
        throw new AssertionError();
    }

    default InWorldRecipeManager anc$getInWorldRecipeManager() {
        throw new AssertionError();
    }

    default HolderLookup.Provider anc$getRegistries() {
        throw new AssertionError();
    }

    default void anc$addRecipes(List<RecipeHolder<InWorldRecipe>> recipes) {
        throw new AssertionError();
    }
}
