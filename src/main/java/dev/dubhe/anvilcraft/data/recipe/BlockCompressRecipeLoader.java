package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.BlockCompressRecipe;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockCompressRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        blockCompress(provider, Blocks.STONE, Blocks.STONE, Blocks.DEEPSLATE);
        blockCompress(provider, Blocks.ICE, Blocks.ICE, Blocks.PACKED_ICE);
        blockCompress(provider, Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE);
        blockCompress(provider, Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK);
        blockCompress(provider, Blocks.NETHER_WART_BLOCK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM);
        blockCompress(provider, Blocks.WARPED_WART_BLOCK, Blocks.NETHERRACK, Blocks.WARPED_NYLIUM);
        blockCompress(provider, Blocks.BASALT, Blocks.BASALT, Blocks.BLACKSTONE);
        blockCompress(
            provider, ModBlocks.CREAM_BLOCK.get(), ModBlocks.CAKE_BASE_BLOCK.get(), ModBlocks.CAKE_BLOCK.get());
        blockCompress(
            provider,
            ModBlocks.BERRY_CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.BERRY_CAKE_BLOCK.get());
        blockCompress(
            provider,
            ModBlocks.CHOCOLATE_CREAM_BLOCK.get(),
            ModBlocks.CAKE_BASE_BLOCK.get(),
            ModBlocks.CHOCOLATE_CAKE_BLOCK.get());
        blockCompress(provider, Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        blockCompress(provider, Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        BlockCompressRecipe.builder()
            .input(BlockTags.LEAVES)
            .input(Blocks.DIRT)
            .result(Blocks.PODZOL)
            .save(provider);
        BlockCompressRecipe.builder()
            .input(ModBlockTags.MUSHROOM_BLOCK)
            .input(Blocks.DIRT)
            .result(Blocks.MYCELIUM)
            .save(provider);
        BlockCompressRecipe.builder()
            .input(ModBlocks.VOID_MATTER_BLOCK.get())
            .input(ModBlocks.SUPERCRITICAL_NESTING_SHULKER_BOX.get())
            .result(ModBlocks.SPACE_OVERCOMPRESSOR.get())
            .save(provider);
    }

    private static void blockCompress(RegistrateRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockCompressRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }
}
