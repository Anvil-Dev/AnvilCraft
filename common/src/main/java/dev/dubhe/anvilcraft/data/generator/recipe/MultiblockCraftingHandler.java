package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.data.recipe.multiblock.MultiblockCraftingRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.Map;

public class MultiblockCraftingHandler {

    /**
     * 初始化多方块合成配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        MultiblockCraftingRecipe.builder(AnvilCraft.of("giant_anvil_1"))
                .layer(
                        "AAA",
                        "AAA",
                        "AAA"
                )
                .layer(
                        "CCC",
                        "CBC",
                        "CCC"
                )
                .layer(
                        "DDD",
                        "DDD",
                        "DDD"
                )
                .define('A', ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get())
                .define('B', ModBlocks.HEAVY_IRON_COLUMN.get())
                .define('C', ModBlocks.HEAVY_IRON_PLATE.get())
                .define('D', ModBlocks.CUT_HEAVY_IRON_BLOCK.get())
                .thenAccept(provider);

        MultiblockCraftingRecipe.builder(AnvilCraft.of("giant_anvil_2"))
                .layer(
                        "ABA",
                        "CAC",
                        "ABA"
                )
                .layer(
                        "EEE",
                        "EDE",
                        "EEE"
                )
                .layer(
                        "JGJ",
                        "HDI",
                        "JFJ"
                )
                .define('A', ModBlocks.POLISHED_HEAVY_IRON_BLOCK.get())
                .define('B', ModBlocks.HEAVY_IRON_BEAM.get(), Map.entry(HeavyIronBeamBlock.AXIS, Direction.Axis.Z))
                .define('C', ModBlocks.HEAVY_IRON_BEAM.get(), Map.entry(HeavyIronBeamBlock.AXIS, Direction.Axis.X))
                .define('D', ModBlocks.HEAVY_IRON_COLUMN.get())
                .define('E', ModBlocks.HEAVY_IRON_PLATE.get())
                .define('F', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(), Map.entry(StairBlock.FACING, Direction.SOUTH), Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('G', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(), Map.entry(StairBlock.FACING, Direction.NORTH), Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('H', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(), Map.entry(StairBlock.FACING, Direction.EAST), Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('I', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(), Map.entry(StairBlock.FACING, Direction.WEST), Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('J', ModBlocks.CUT_HEAVY_IRON_SLAB.get(), Map.entry(SlabBlock.TYPE, SlabType.BOTTOM))
                .thenAccept(provider);

        MultiblockCraftingRecipe.builder(AnvilCraft.of("menger_sponge"))
                .layer(
                        "AAA",
                        "A A",
                        "AAA"
                )
                .layer(
                        "A A",
                        " B ",
                        "A A"
                )
                .layer(
                        "AAA",
                        "A A",
                        "AAA"
                )
                .define('A', Blocks.SPONGE)
                .define('B', ModBlocks.VOID_MATTER_BLOCK.get())
                .define(' ', Blocks.AIR)
                .thenAccept(provider);
    }
}
