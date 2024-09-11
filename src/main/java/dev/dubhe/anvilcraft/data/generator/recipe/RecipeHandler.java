package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class RecipeHandler {
    public static void init(RegistrateRecipeProvider provider) {
        BlockCrushRecipeLoader.init(provider);
        ItemCrushRecipeLoader.init(provider);
        CompressRecipeLoader.init(provider);
        MeshRecipeLoader.init(provider);
    }
}
