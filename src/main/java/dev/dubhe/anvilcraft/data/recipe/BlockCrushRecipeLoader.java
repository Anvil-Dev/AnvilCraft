package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCrushRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockCrushRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        blockCrush(provider, Blocks.STONE, Blocks.COBBLESTONE);
        blockCrush(provider, Blocks.COBBLESTONE, Blocks.GRAVEL);
        blockCrush(provider, Blocks.GRAVEL, Blocks.SAND);
        blockCrush(provider, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
        blockCrush(provider, Blocks.GRANITE, Blocks.RED_SAND);
        blockCrush(provider, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
        blockCrush(provider, Blocks.ANDESITE, ModBlocks.CINERITE.get());
        blockCrush(provider, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
        blockCrush(provider, Blocks.DIORITE, ModBlocks.QUARTZ_SAND.get());
        blockCrush(provider, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        blockCrush(provider, Blocks.DEEPSLATE_BRICKS, Blocks.CRACKED_DEEPSLATE_BRICKS);
        blockCrush(provider, Blocks.NETHER_BRICKS, Blocks.CRACKED_NETHER_BRICKS);
        blockCrush(provider, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_TILES);
        blockCrush(provider, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
        blockCrush(provider, Blocks.SOUL_SOIL, Blocks.SOUL_SAND);
        blockCrush(provider, Blocks.NETHERRACK, ModBlocks.NETHER_DUST.get());
        blockCrush(provider, Blocks.END_STONE, ModBlocks.END_DUST.get());
    }

    private static void blockCrush(RegistrateRecipeProvider provider, Block input, Block result) {
        BlockCrushRecipe.builder().input(input).result(result).save(provider);
    }
}
