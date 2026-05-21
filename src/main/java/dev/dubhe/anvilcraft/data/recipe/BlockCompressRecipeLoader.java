package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockCompressRecipe;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockCompressRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.STONE,
            Blocks.DEEPSLATE
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.ICE,
            Blocks.PACKED_ICE
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.PACKED_ICE,
            Blocks.BLUE_ICE
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.NETHER_WART_BLOCK,
            Blocks.NETHERRACK,
            Blocks.CRIMSON_NYLIUM
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.WARPED_WART_BLOCK,
            Blocks.NETHERRACK,
            Blocks.WARPED_NYLIUM
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            Blocks.BASALT,
            Blocks.BLACKSTONE
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            ModBlocks.CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.CAKE_BLOCK.get()
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            ModBlocks.BERRY_CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.BERRY_CAKE_BLOCK.get()
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            ModBlocks.CHOCOLATE_CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.CHOCOLATE_CAKE_BLOCK.get()
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            BlockTags.LEAVES,
            Blocks.DIRT,
            Blocks.PODZOL
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            ModBlockTags.MUSHROOM_BLOCK,
            Blocks.DIRT,
            Blocks.MYCELIUM
        );
        BlockCompressRecipeLoader.recipe(
            provider,
            ModBlocks.VOID_MATTER_BLOCK.get(),
            ModBlocks.SUPERCRITICAL_NESTING_SHULKER_BOX.get(),
            ModBlocks.SPACE_OVERCOMPRESSOR.get()
        );
    }

    private static void recipe(RegistrumRecipeProvider provider, Block block, Block result) {
        BlockCompressRecipe.builder().input(block).input(block).result(result).save(provider);
    }

    private static void recipe(RegistrumRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockCompressRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }

    @SuppressWarnings("SameParameterValue")
    private static void recipe(RegistrumRecipeProvider provider, TagKey<Block> tag1, Block block2, Block result) {
        HolderGetter<Block> blocks = provider.getRegistries().lookupOrThrow(Registries.BLOCK);
        BlockCompressRecipe.builder().input(blocks, tag1).input(block2).result(result).save(provider);
    }
}
