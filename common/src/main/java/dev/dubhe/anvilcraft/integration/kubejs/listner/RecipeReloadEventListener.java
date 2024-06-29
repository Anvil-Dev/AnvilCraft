package dev.dubhe.anvilcraft.integration.kubejs.listner;

import dev.anvilcraft.lib.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.recipe.RecipeReloadEvent;
import dev.dubhe.anvilcraft.integration.kubejs.evnet.AnvilEvents;
import dev.dubhe.anvilcraft.integration.kubejs.evnet.LoadAnvilCraftingRecipeEvent;

public class RecipeReloadEventListener {
    @SubscribeEvent
    public void onReload(RecipeReloadEvent recipeReloadEvent) {
        AnvilEvents.LOAD_ANVIL_CRAFTING_RECIPES.post(new LoadAnvilCraftingRecipeEvent());
    }
}
