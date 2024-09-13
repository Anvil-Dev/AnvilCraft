package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.recipe.BlockCrushRecipe;

import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class BlockCrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        BlockCrushRecipe.builder()
                .input(Blocks.STONE)
                .result(Blocks.COBBLESTONE)
                .save(provider);

        BlockCrushRecipe.builder()
                .input(Blocks.COBBLESTONE)
                .result(Blocks.GRAVEL)
                .save(provider);

        BlockCrushRecipe.builder().input(Blocks.GRAVEL).result(Blocks.SAND).save(provider);
    }
}
