package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;

public class MultipleToOneSmithingRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        TwoToOneSmithingRecipe.builder(ModItems.MULTIPHASE_MATTER_AXE, 0)
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMBER_METAL_AXE)
            .save(provider);
        TwoToOneSmithingRecipe.builder(ModItems.MULTIPHASE_MATTER_HOE, 0)
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMBER_METAL_HOE)
            .save(provider);
        TwoToOneSmithingRecipe.builder(ModItems.MULTIPHASE_MATTER_PICKAXE, 0)
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMBER_METAL_PICKAXE)
            .save(provider);
        TwoToOneSmithingRecipe.builder(ModItems.MULTIPHASE_MATTER_SHOVEL, 0)
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMBER_METAL_SHOVEL)
            .save(provider);
        TwoToOneSmithingRecipe.builder(ModItems.MULTIPHASE_MATTER_SWORD, 0)
            .material(ModItems.MULTIPHASE_MATTER)
            .input(ModItems.EMBER_METAL_SWORD)
            .save(provider);
    }
}
