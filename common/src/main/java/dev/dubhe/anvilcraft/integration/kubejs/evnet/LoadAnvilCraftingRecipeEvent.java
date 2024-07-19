package dev.dubhe.anvilcraft.integration.kubejs.evnet;

import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.latvian.mods.kubejs.event.EventJS;

public class LoadAnvilCraftingRecipeEvent extends EventJS {
    public static final String ID = "anvil.recipe.load";


    public LoadAnvilCraftingRecipeEvent() {}

    public void register(AnvilRecipe anvilRecipe) {
        AnvilRecipeManager.externalRecipeList.add(anvilRecipe);
    }
}
