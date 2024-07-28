package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.HeavyIronBeamBlock;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.RecipeItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipeType;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.multiblock.HasMultiBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.function.Consumer;

public class MultiblockCraftingHandler {

    /**
     * 初始化多方块合成配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        HasMultiBlock giantAnvil1 = HasMultiBlock.builder()
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
                .build();

        multiblock(giantAnvil1, ModBlocks.GIANT_ANVIL.asItem(), "1", provider);
        HasMultiBlock giantAnvil2 = HasMultiBlock.builder()
                .layer(
                        "ACA",
                        "BAB",
                        "ACA"
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
                .define('F', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                        Map.entry(StairBlock.FACING, Direction.SOUTH),
                        Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('G', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                        Map.entry(StairBlock.FACING, Direction.NORTH),
                        Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('H', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                        Map.entry(StairBlock.FACING, Direction.EAST),
                        Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('I', ModBlocks.CUT_HEAVY_IRON_STAIRS.get(),
                        Map.entry(StairBlock.FACING, Direction.WEST),
                        Map.entry(StairBlock.HALF, Half.BOTTOM))
                .define('J', ModBlocks.CUT_HEAVY_IRON_SLAB.get(), Map.entry(SlabBlock.TYPE, SlabType.BOTTOM))
                .build();

        multiblock(giantAnvil2, ModBlocks.GIANT_ANVIL.asItem(), "2", provider);
        HasMultiBlock mengerSponge = HasMultiBlock.builder()
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
                .build();
        multiblock(mengerSponge, ModBlocks.MENGER_SPONGE.asItem(), "", provider);
        HasMultiBlock largeCake = HasMultiBlock.builder()
            .layer(
                "   ",
                " C ",
                "   "
            )
            .layer(
                " B ",
                        "BBB",
                " B "
            ).layer(
                "AAA",
                "AAA",
                "AAA"
            )
            .define('A', ModBlocks.CAKE_BLOCK.get())
            .define('B', ModBlocks.BERRY_CAKE_BLOCK.get())
            .define('C', ModBlocks.CHOCOLATE_CAKE_BLOCK.get())
            .define(' ', Blocks.AIR)
            .build();
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .type(AnvilRecipeType.MULTIBLOCK_CRAFTING)
            .icon(ModBlocks.LARGE_CAKE.asItem())
            .addPredicates(largeCake)
            .setBlock(new Vec3(0.0, -4.0, 0.0), ModBlocks.LARGE_CAKE.get())
            .unlockedBy(
                AnvilCraftDatagen.hasItem(ModBlocks.GIANT_ANVIL),
                AnvilCraftDatagen.has(ModBlocks.GIANT_ANVIL)
            )
            .save(provider, AnvilCraft.of("multiblock_crafting/"
                + BuiltInRegistries.ITEM.getKey(ModBlocks.LARGE_CAKE.asItem()).getPath()
            ));
    }

    private static void multiblock(HasMultiBlock pred, Item output, String postfix, Consumer<FinishedRecipe> cons) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .type(AnvilRecipeType.MULTIBLOCK_CRAFTING)
                .icon(output)
                .addPredicates(pred)
                .spawnItem(new Vec3(0.0, -2.0, 0.0), RecipeItem.of(output))
                .unlockedBy(
                        AnvilCraftDatagen.hasItem(ModBlocks.GIANT_ANVIL),
                        AnvilCraftDatagen.has(ModBlocks.GIANT_ANVIL)
                )
                .save(cons, AnvilCraft.of("multiblock_crafting/"
                        + BuiltInRegistries.ITEM.getKey(output).getPath() + postfix
                ));

    }
}
