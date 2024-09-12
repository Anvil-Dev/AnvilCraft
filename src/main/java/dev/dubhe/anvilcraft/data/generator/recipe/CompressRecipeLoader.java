package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.CompressRecipe;

import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class CompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        CompressRecipe.builder()
                .input(Blocks.STONE)
                .input(Blocks.STONE)
                .result(Blocks.DEEPSLATE)
                .save(provider, AnvilCraft.of("compress/deepslate"));

        CompressRecipe.builder()
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .result(Blocks.PACKED_ICE)
                .save(provider, AnvilCraft.of("compress/packed_ice"));
    }
}
