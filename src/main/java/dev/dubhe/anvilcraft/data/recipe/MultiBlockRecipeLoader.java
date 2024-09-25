package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MultiBlockRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MultiblockRecipe.builder(ModBlocks.GIANT_ANVIL)
                .layer("JGJ", "HDI", "JFJ")
                .layer("EEE", "EDE", "EEE")
                .layer("ACA", "BAB", "ACA")
                .symbol('A', ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get())
                .symbol(
                        'B',
                        BlockPredicateWithState.of(ModBlocks.HEAVY_IRON_BEAM.get())
                                .hasState(HeavyIronBeamBlock.AXIS, Direction.Axis.X))
                .symbol(
                        'C',
                        BlockPredicateWithState.of(ModBlocks.HEAVY_IRON_BEAM.get())
                                .hasState(HeavyIronBeamBlock.AXIS, Direction.Axis.Z))
                .symbol('D', ModBlocks.HEAVY_IRON_COLUMN.get())
                .symbol('E', ModBlocks.HEAVY_IRON_PLATE.get())
                .symbol(
                        'F',
                        BlockPredicateWithState.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get())
                                .hasState(StairBlock.FACING, Direction.NORTH)
                                .hasState(StairBlock.HALF, Half.BOTTOM))
                .symbol(
                        'G',
                        BlockPredicateWithState.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get())
                                .hasState(StairBlock.FACING, Direction.SOUTH)
                                .hasState(StairBlock.HALF, Half.BOTTOM))
                .symbol(
                        'H',
                        BlockPredicateWithState.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get())
                                .hasState(StairBlock.FACING, Direction.EAST)
                                .hasState(StairBlock.HALF, Half.BOTTOM))
                .symbol(
                        'I',
                        BlockPredicateWithState.of(ModBlocks.CUT_HEAVY_IRON_STAIRS.get())
                                .hasState(StairBlock.FACING, Direction.WEST)
                                .hasState(StairBlock.HALF, Half.BOTTOM))
                .symbol(
                        'J',
                        BlockPredicateWithState.of(ModBlocks.CUT_HEAVY_IRON_SLAB.get())
                                .hasState(SlabBlock.TYPE, SlabType.BOTTOM))
                .save(provider, AnvilCraft.of("mulitblock/giant_anvil_1"));

        MultiblockRecipe.builder(ModBlocks.GIANT_ANVIL)
                .layer("AAA", "AAA", "AAA")
                .layer("CCC", "CBC", "CCC")
                .layer("DDD", "DDD", "DDD")
                .symbol('A', ModBlocks.CUT_HEAVY_IRON_BLOCK.get())
                .symbol('B', ModBlocks.HEAVY_IRON_COLUMN.get())
                .symbol('C', ModBlocks.HEAVY_IRON_PLATE.get())
                .symbol('D', ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get())
                .save(provider, AnvilCraft.of("mulitblock/giant_anvil_2"));

        MultiblockRecipe.builder(ModBlocks.MENGER_SPONGE)
                .layer("AAA", "A A", "AAA")
                .layer("A A", " B ", "A A")
                .layer("AAA", "A A", "AAA")
                .symbol('A', Blocks.SPONGE)
                .symbol('B', ModBlocks.VOID_MATTER_BLOCK.get())
                .save(provider);

        MultiblockRecipe.builder(ModBlocks.LARGE_CAKE)
                .layer("AAA", "AAA", "AAA")
                .layer(" B ", "BBB", " B ")
                .layer("   ", " C ", "   ")
                .symbol('A', ModBlocks.CAKE_BLOCK.get())
                .symbol('B', ModBlocks.BERRY_CAKE_BLOCK.get())
                .symbol('C', ModBlocks.CHOCOLATE_CAKE_BLOCK.get())
                .save(provider);

        MultiblockRecipe.builder(ModBlocks.MENGER_SPONGE)
                .layer(
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA")
                .layer(
                        "A AA AA A",
                        "         ",
                        "A AA AA A",
                        "A A   A A",
                        "         ",
                        "A A   A A",
                        "A AA AA A",
                        "         ",
                        "A AA AA A")
                .layer(
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA")
                .layer(
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "         ",
                        "         ",
                        "         ",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA")
                .layer(
                        "A A   A A",
                        "         ",
                        "A A   A A",
                        "         ",
                        "         ",
                        "         ",
                        "A A   A A",
                        "         ",
                        "A A   A A")
                .layer(
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "         ",
                        "         ",
                        "         ",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA")
                .layer(
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA")
                .layer(
                        "A AA AA A",
                        "         ",
                        "A AA AA A",
                        "A A   A A",
                        "         ",
                        "A A   A A",
                        "A AA AA A",
                        "         ",
                        "A AA AA A")
                .layer(
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA",
                        "AAA   AAA",
                        "A A   A A",
                        "AAA   AAA",
                        "AAAAAAAAA",
                        "A AA AA A",
                        "AAAAAAAAA")
                .symbol('A', "anvilcraft:menger_sponge")
                .save(provider, AnvilCraft.of("mulitblock/menger_sponge_2"));
    }
}
