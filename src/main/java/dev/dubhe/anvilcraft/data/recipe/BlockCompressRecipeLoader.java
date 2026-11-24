package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

public class BlockCompressRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        blockCompress(provider, Blocks.STONE, Blocks.STONE, Blocks.DEEPSLATE);
        blockCompress(provider, Blocks.ICE, Blocks.ICE, Blocks.PACKED_ICE);
        blockCompress(provider, Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
        BlockCompressRecipe.builder()
            .input(Blocks.NETHER_WART_BLOCK)
            .input(Tags.Blocks.NETHERRACKS)
            .result(Blocks.CRIMSON_NYLIUM)
            .save(provider);
        BlockCompressRecipe.builder()
            .input(Blocks.WARPED_WART_BLOCK)
            .input(Tags.Blocks.NETHERRACKS)
            .result(Blocks.WARPED_NYLIUM)
            .save(provider);
        blockCompress(provider, Blocks.BASALT, Blocks.BASALT, Blocks.BLACKSTONE);
        blockCompress(provider, ModBlocks.CREAM_BLOCK.get(), ModBlocks.CAKE_BASE_BLOCK.get(), ModBlocks.CAKE_BLOCK.get());
        blockCompress(provider, ModBlocks.BERRY_CREAM_BLOCK.get(), ModBlocks.CAKE_BASE_BLOCK.get(), ModBlocks.BERRY_CAKE_BLOCK.get());
        blockCompress(
            provider,
            ModBlocks.CHOCOLATE_CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.CHOCOLATE_CAKE_BLOCK.get()
        );
        blockCompress(provider, ModBlocks.HONEY_CREAM_BLOCK.get(), ModBlocks.CAKE_BASE_BLOCK.get(), ModBlocks.HONEY_CAKE_BLOCK.get());
        blockCompress(provider, ModBlocks.MATCHA_CREAM_BLOCK.get(), ModBlocks.CAKE_BASE_BLOCK.get(), ModBlocks.MATCHA_CAKE_BLOCK.get());
        blockCompress(provider, BlockTags.LEAVES, Blocks.DIRT, Blocks.PODZOL);
        blockCompress(provider, Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK);
        blockCompress(provider, ModBlockTags.MUSHROOM_BLOCK, Blocks.DIRT, Blocks.MYCELIUM);
    }

    private static void blockCompress(RegistrumRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockCompressRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void blockCompress(RegistrumRecipeProvider provider, TagKey<Block> tag1, Block block2, Block result) {
        BlockCompressRecipe.builder().input(tag1).input(block2).result(result).save(provider);
    }
}
