package dev.dubhe.anvilcraft.recipe.sync;

import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.Level;

public class RecipesRecord {
    public static RecipeMap CLIENTSIDE;

    public static RecipeMap getRecipes(Level level) {
        if (level.isClientSide()) {
            return RecipesRecord.CLIENTSIDE;
        } else {
            return level.getServer().getRecipeManager().recipeMap();
        }
    }
}
