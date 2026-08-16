package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.block.RubyLaserBlock;
import dev.dubhe.anvilcraft.block.RubyPrismBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.common.Tags;

public class MultiBlockRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        MultiblockRecipe.builder(ModBlocks.GIANT_ANVIL)
            .layer("ABA", "CDE", "AFA")
            .layer("   ", " D ", "   ")
            .layer("GHG", "IGI", "GHG")
            .symbol(
                'A',
                BlockStatePredicate.builder().of(ModBlocks.CUT_HEAVY_IRON_SLAB)
                    .with(SlabBlock.TYPE, SlabType.BOTTOM)
                    .with(SlabBlock.WATERLOGGED, false))
            .symbol(
                'B',
                BlockStatePredicate.builder().of(ModBlocks.CUT_HEAVY_IRON_STAIRS)
                    .with(StairBlock.FACING, Direction.SOUTH)
                    .with(StairBlock.HALF, Half.BOTTOM)
                    .with(StairBlock.WATERLOGGED, false))
            .symbol(
                'C',
                BlockStatePredicate.builder().of(ModBlocks.CUT_HEAVY_IRON_STAIRS)
                    .with(StairBlock.FACING, Direction.EAST)
                    .with(StairBlock.HALF, Half.BOTTOM)
                    .with(StairBlock.WATERLOGGED, false))
            .symbol('D', ModBlocks.HEAVY_IRON_COLUMN)
            .symbol(
                'E',
                BlockStatePredicate.builder().of(ModBlocks.CUT_HEAVY_IRON_STAIRS)
                    .with(StairBlock.FACING, Direction.WEST)
                    .with(StairBlock.HALF, Half.BOTTOM)
                    .with(StairBlock.WATERLOGGED, false))
            .symbol(
                'F',
                BlockStatePredicate.builder().of(ModBlocks.CUT_HEAVY_IRON_STAIRS)
                    .with(StairBlock.FACING, Direction.NORTH)
                    .with(StairBlock.HALF, Half.BOTTOM)
                    .with(StairBlock.WATERLOGGED, false))
            .symbol('G', ModBlocks.POLISHED_HEAVY_IRON_BLOCK)
            .symbol(
                'H',
                BlockStatePredicate.builder().of(ModBlocks.HEAVY_IRON_BEAM)
                    .with(HeavyIronBeamBlock.AXIS, Direction.Axis.Z))
            .symbol(
                'I',
                BlockStatePredicate.builder().of(ModBlocks.HEAVY_IRON_BEAM)
                    .with(HeavyIronBeamBlock.AXIS, Direction.Axis.X))
            .save(provider, AnvilCraft.of("multiblock/giant_anvil_1"));

        MultiblockRecipe.builder(ModBlocks.GIANT_ANVIL)
            .layer("AAA", "AAA", "AAA")
            .layer("   ", " B ", "   ")
            .layer("CCC", "CCC", "CCC")
            .symbol('A', ModBlocks.CUT_HEAVY_IRON_BLOCK)
            .symbol('B', ModBlocks.HEAVY_IRON_COLUMN)
            .symbol('C', ModBlocks.POLISHED_HEAVY_IRON_BLOCK)
            .save(provider, AnvilCraft.of("multiblock/giant_anvil_2"));

        MultiblockRecipe.builder(ModBlocks.LARGE_FLUID_TANK)
            .layer("AAA", "AAA", "AAA")
            .layer("ABA", "B B", "ABA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', ModBlocks.CUT_BRASS_BLOCK)
            .symbol('B', Tags.Blocks.GLASS_BLOCKS)
            .save(provider, AnvilCraft.of("multiblock/large_fluid_tank"));

        MultiblockRecipe.builder("anvilcraft:large_cauldron", 1)
            .layer("AAA", "AAA", "AAA")
            .layer("ABA", "B B", "ABA")
            .layer("ABA", "B B", "ABA")
            .symbol('A', "anvilcraft:cut_heavy_iron_block")
            .symbol('B', "anvilcraft:tempering_glass")
            .save(provider);

        MultiblockRecipe.builder(ModBlocks.MENGER_SPONGE)
            .layer("AAA", "A A", "AAA")
            .layer("A A", " B ", "A A")
            .layer("AAA", "A A", "AAA")
            .symbol('A', Blocks.SPONGE)
            .symbol('B', ModBlocks.VOID_MATTER_BLOCK)
            .save(provider);

        MultiblockRecipe.builder(Blocks.DIAMOND_BLOCK)
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', Blocks.COAL_BLOCK)
            .save(provider);

        MultiblockRecipe.builder(ModBlocks.LARGE_CAKE)
            .layer("AAA", "AAA", "AAA")
            .layer(" B ", "BBB", " B ")
            .layer("   ", " C ", "   ")
            .symbol('A', ModBlocks.CAKE_BLOCK)
            .symbol('B', ModBlocks.BERRY_CAKE_BLOCK)
            .symbol('C', ModBlocks.CHOCOLATE_CAKE_BLOCK)
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
            .symbol('A', ModBlocks.MENGER_SPONGE)
            .save(provider, AnvilCraft.of("multiblock/menger_sponge_2"));

        MultiblockRecipe.builder("anvilcraft:deflection_ring", 1)
            .layer("ABA", "BAB", "ABA")
            .layer("   ", "   ", "   ")
            .layer("ABA", "BAB", "ABA")
            .symbol('A', "anvilcraft:magnetoelectric_core")
            .symbol('B', "anvilcraft:heavy_iron_block")
            .save(provider);

        MultiblockRecipe.builder("anvilcraft:acceleration_ring", 1)
            .layer("ABA", "B B", "ABA")
            .layer("CDC", "D D", "CDC")
            .layer("ABA", "B B", "ABA")
            .symbol('A', "minecraft:copper_block")
            .symbol('B', "anvilcraft:heavy_iron_block")
            .symbol('C', "anvilcraft:magnetoelectric_core")
            .symbol('D', "anvilcraft:tungsten_block")
            .save(provider);

        MultiblockRecipe.builder("anvilcraft:large_laser", 1)
            .layer(" A ", "BCD", " E ")
            .layer("BFD", "BCD", "BGD")
            .layer("AAA", "HCI", "EEE")
            .symbol('A', BlockStatePredicate.builder().of(ModBlocks.RUBY_LASER)
                .with(RubyLaserBlock.FACING, Direction.SOUTH)
            )
            .symbol('B', BlockStatePredicate.builder().of(ModBlocks.RUBY_LASER)
                .with(RubyLaserBlock.FACING, Direction.EAST)
            )
            .symbol('C', BlockStatePredicate.builder().of(ModBlocks.RUBY_PRISM)
                .with(RubyPrismBlock.FACING, Direction.DOWN)
            )
            .symbol('D', BlockStatePredicate.builder().of(ModBlocks.RUBY_LASER)
                .with(RubyLaserBlock.FACING, Direction.WEST)
            )
            .symbol('E', BlockStatePredicate.builder().of(ModBlocks.RUBY_LASER)
                .with(RubyLaserBlock.FACING, Direction.NORTH)
            )
            .symbol('F', BlockStatePredicate.builder().of(ModBlocks.RUBY_PRISM)
                .with(RubyPrismBlock.FACING, Direction.SOUTH)
            )
            .symbol('G', BlockStatePredicate.builder().of(ModBlocks.RUBY_PRISM)
                .with(RubyPrismBlock.FACING, Direction.NORTH)
            )
            .symbol('H', BlockStatePredicate.builder().of(ModBlocks.RUBY_PRISM)
                .with(RubyPrismBlock.FACING, Direction.EAST)
            )
            .symbol('I', BlockStatePredicate.builder().of(ModBlocks.RUBY_PRISM)
                .with(RubyPrismBlock.FACING, Direction.WEST)
            )
            .save(provider);

        MultiblockRecipe.builder("anvilcraft:celestial_forging_anvil", 1)
            .layer("AAA", "ABA", "AAA")
            .layer("CCC", "CFC", "CCC")
            .layer("DED", "E E", "DED")
            .symbol('A', "anvilcraft:transcendium_block")
            .symbol('B', "anvilcraft:spacetime_supercomputer")
            .symbol('C', "anvilcraft:enchanted_gold_block")
            .symbol('D', "anvilcraft:confinement_chamber")
            .symbol('E', "anvilcraft:negative_matter_block")
            .symbol('F', "anvilcraft:mass_energy_inverter")
            .save(provider);

        MultiblockRecipe.builder(ModBlocks.LARGE_CRATE, 1)
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "ABA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', Tags.Blocks.STRIPPED_LOGS)
            .symbol('B', ModBlocks.RESIN_BLOCK)
            .save(provider);

        Multiblock4DRecipe.builder("anvilcraft:hypercube", 1)
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', "anvilcraft:tempering_glass")
            .next()
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', "anvilcraft:tempering_glass")
            .next()
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .layer("AAA", "AAA", "AAA")
            .symbol('A', "anvilcraft:tempering_glass")
            .save(provider);
    }
}
