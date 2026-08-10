package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockCrushRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.COBBLESTONE, Blocks.GRAVEL);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.GRAVEL, Blocks.SAND);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.GRANITE, Blocks.RED_SAND);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.ANDESITE, ModBlocks.CINERITE.get());
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.DIORITE, ModBlocks.QUARTZ_SAND.get());
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.SOUL_SOIL, Blocks.SOUL_SAND);
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.NETHERRACK, ModBlocks.NETHER_DUST.get());
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.END_STONE, ModBlocks.END_DUST.get());
        BlockCrushRecipeLoader.blockCrush(provider, Blocks.FURNACE, ModBlocks.BURNING_HEATER.get());
    }

    private static void blockCrush(RegistrumRecipeProvider provider, Block input, Block result) {
        BlockCrushRecipe.builder().input(input).result(result).save(provider);
    }
}
