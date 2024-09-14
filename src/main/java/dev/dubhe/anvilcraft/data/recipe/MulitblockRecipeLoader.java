package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mulitblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.mulitblock.MulitblockRecipe;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;

public class MulitblockRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        MulitblockRecipe.builder()
                .layer("ACA", "BAB", "ACA")
                .layer("EEE", "EDE", "EEE")
                .layer("JGJ", "HDI", "JFJ")
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
                .result(new ItemStack(ModBlocks.GIANT_ANVIL.asItem()))
                .save(provider);
    }
}
