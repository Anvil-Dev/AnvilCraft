package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCrushRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class BlockCrushRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlockCrushRecipeLoader.blockCrush(provider, Tags.Blocks.COBBLESTONES_NORMAL, Blocks.GRAVEL);
        BlockCrushRecipeLoader.blockCrush(provider, Tags.Blocks.GRAVELS, Blocks.SAND);
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
        BlockCrushRecipeLoader.blockCrush(provider, Tags.Blocks.NETHERRACKS, ModBlocks.NETHER_DUST.get());
        BlockCrushRecipeLoader.blockCrush(provider, Tags.Blocks.END_STONES, ModBlocks.END_DUST.get());
        BlockCrushRecipeLoader.blockCrush(provider, Tags.Blocks.PLAYER_WORKSTATIONS_FURNACES, ModBlocks.BURNING_HEATER.get());
    }

    private static void blockCrush(RegistrumRecipeProvider provider, TagKey<Block> input, Block result) {
        var blocks = provider.getRegistries().lookupOrThrow(Registries.BLOCK);
        BlockCrushRecipe.builder().input(blocks, input).result(result).save(provider);
    }

    private static void blockCrush(RegistrumRecipeProvider provider, Block input, Block result) {
        BlockCrushRecipe.builder().input(input).result(result).save(provider);
    }
}
