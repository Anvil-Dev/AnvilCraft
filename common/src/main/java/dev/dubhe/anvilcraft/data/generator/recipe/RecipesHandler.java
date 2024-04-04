package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class RecipesHandler {

    public static void init(RegistrateRecipeProvider provider) {

        VanillaRecipesLoader.init(provider);

        AnvilItemRecipesLoader.init(provider);
        BulgingAndCrystallizeRecipesLoader.init(provider);
        CompactionRecipesLoader.init(provider);
        CompressRecipesLoader.init(provider);
        CookingRecipesLoader.init(provider);
        ItemInjectRecipesLoader.init(provider);
        SmashBlockRecipesLoader.init(provider);
        SmashRecipesLoader.init(provider);
        SqueezeRecipesLoader.init(provider);
        StampingRecipesLoader.init(provider);

    }
}
