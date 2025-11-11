package dev.dubhe.anvilcraft.integration.jei.recipe;

import java.util.List;

public record TranscendiumRecipe(int recipeId) {
    public static List<TranscendiumRecipe> getAllRecipes() {
        return List.of(
            new TranscendiumRecipe(0),
            new TranscendiumRecipe(1),
            new TranscendiumRecipe(2),
            new TranscendiumRecipe(3),
            new TranscendiumRecipe(4)
        );
    }
}
