package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class RecipeHandler {
    public static void init(RegistrateRecipeProvider provider) {
        CrushRecipeLoader.init(provider);
        CompressRecipeLoader.init(provider);
    }
}
