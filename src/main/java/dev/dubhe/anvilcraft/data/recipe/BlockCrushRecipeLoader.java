package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class BlockCrushRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlockCrushRecipe.builder().input(Tags.Blocks.COBBLESTONES_NORMAL).result(Blocks.GRAVEL).save(provider);
        BlockCrushRecipe.builder().input(Tags.Blocks.GRAVELS).result(Blocks.SAND).save(provider);
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
        BlockCrushRecipe.builder().input(Tags.Blocks.NETHERRACKS).result(ModBlocks.NETHER_DUST.get()).save(provider);
        BlockCrushRecipe.builder().input(Tags.Blocks.END_STONES).result(ModBlocks.END_DUST.get()).save(provider);
        BlockCrushRecipe.builder().input(Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES).result(ModBlocks.BURNING_HEATER.get()).save(provider);
    }

    private static void blockCrush(RegistrumRecipeProvider provider, Block input, Block result) {
        BlockCrushRecipe.builder().input(input).result(result).save(provider);
    }
}
