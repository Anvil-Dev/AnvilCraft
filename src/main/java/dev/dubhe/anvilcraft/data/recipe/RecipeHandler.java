package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class RecipeHandler {
    public static void init(RegistrateRecipeProvider provider) {
        BlockCrushRecipeLoader.init(provider);
        ItemCrushRecipeLoader.init(provider);
        BlockCompressRecipeLoader.init(provider);
        ItemCompressRecipeLoader.init(provider);
        MeshRecipeLoader.init(provider);
        StampingRecipeLoader.init(provider);
        SuperHeatingRecipeLoader.init(provider);
        TimeWarpRecipeLoader.init(provider);
        CookingRecipeLoader.init(provider);
        BulgingRecipeLoader.init(provider);
        ItemInjectRecipeLoader.init(provider);
        SqueezingRecipeLoader.init(provider);
        MultiBlockRecipeLoader.init(provider);
        MobTransformRecipeLoader.init(provider);
        ConcreteRecipeLoader.init(provider);

        MineralFountainRecipeLoader.init(provider);
    }
}
