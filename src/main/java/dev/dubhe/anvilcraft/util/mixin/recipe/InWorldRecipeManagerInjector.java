package dev.dubhe.anvilcraft.util.mixin.recipe;

import dev.dubhe.anvilcraft.recipe.anvil.InWorldRecipeManager;

public interface InWorldRecipeManagerInjector {
    default InWorldRecipeManager anvilcraft$getInWorldRecipeManager() {
        throw new AssertionError();
    }
}
