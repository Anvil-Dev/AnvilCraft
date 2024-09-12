package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.BlockCompressRecipe;

import net.minecraft.world.level.block.Blocks;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class BlockCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        BlockCompressRecipe.builder()
                .input(Blocks.STONE)
                .input(Blocks.STONE)
                .result(Blocks.DEEPSLATE)
                .save(provider, AnvilCraft.of("block_compress/deepslate"));

        BlockCompressRecipe.builder()
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .input(Blocks.ICE)
                .result(Blocks.PACKED_ICE)
                .save(provider, AnvilCraft.of("block_compress/packed_ice"));
    }
}
