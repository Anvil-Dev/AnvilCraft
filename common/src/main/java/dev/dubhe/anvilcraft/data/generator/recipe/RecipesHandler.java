package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class RecipesHandler {

    /**
     * 初始化配方生成器
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        VanillaRecipesLoader.init(provider);
        BulgingAndCrystallizeRecipesLoader.init(provider);
        CompactionRecipesLoader.init(provider);
        CompressRecipesLoader.init(provider);
        CookingRecipesLoader.init(provider);
        ItemInjectRecipesLoader.init(provider);
        BlockSmashRecipesLoader.init(provider);
        SievingRecipesLoader.init(provider);
        ItemSmashRecipesLoader.init(provider);
        SqueezeRecipesLoader.init(provider);
        StampingRecipesLoader.init(provider);
        TimeWarpRecipesLoader.init(provider);
        SuperHeatingRecipesLoader.init(provider);
        SmithingRecipesLoader.init(provider);
        MobTransformRecipesLoader.init(provider);
        BulgingLikeRecipesLoader.init(provider);
        MultiblockCraftingHandler.init(provider);
    }
}
