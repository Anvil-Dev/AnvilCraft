package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class SqueezeRecipesGenerator {
    private SqueezeRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        squeeze(Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, ModBlocks.LAVA_CAULDRON, exporter);
        squeeze(Blocks.WET_SPONGE, Blocks.SPONGE, Blocks.WATER_CAULDRON, exporter);
        squeeze(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.WATER_CAULDRON, exporter);
        squeeze(Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.POWDER_SNOW_CAULDRON, exporter);

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.MOSS_BLOCK)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), Blocks.COBBLESTONE)
                .setBlock(new Vec3(0.0, -2.0, 0.0), Blocks.MOSSY_COBBLESTONE)
                .unlockedBy(MyRecipesGenerator.hasItem(Blocks.MOSS_BLOCK.asItem()), FabricRecipeProvider.has(Blocks.MOSS_BLOCK.asItem()))
                .save(exporter, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK.getKey(Blocks.MOSSY_COBBLESTONE).getPath()));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.MOSS_BLOCK)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), Blocks.STONE_BRICKS)
                .setBlock(new Vec3(0.0, -2.0, 0.0), Blocks.MOSSY_STONE_BRICKS)
                .unlockedBy(MyRecipesGenerator.hasItem(Blocks.MOSS_BLOCK.asItem()), FabricRecipeProvider.has(Blocks.MOSS_BLOCK.asItem()))
                .save(exporter, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK.getKey(Blocks.MOSSY_STONE_BRICKS).getPath()));

        waxed(Blocks.COPPER_BLOCK, exporter);
        waxed(Blocks.WEATHERED_COPPER, exporter);
        waxed(Blocks.EXPOSED_COPPER, exporter);
        waxed(Blocks.OXIDIZED_COPPER, exporter);
        waxed(Blocks.OXIDIZED_CUT_COPPER, exporter);
        waxed(Blocks.WEATHERED_CUT_COPPER, exporter);
        waxed(Blocks.EXPOSED_CUT_COPPER, exporter);
        waxed(Blocks.CUT_COPPER, exporter);
        waxed(Blocks.OXIDIZED_CUT_COPPER_STAIRS, exporter);
        waxed(Blocks.WEATHERED_CUT_COPPER_STAIRS, exporter);
        waxed(Blocks.EXPOSED_CUT_COPPER_STAIRS, exporter);
        waxed(Blocks.CUT_COPPER_STAIRS, exporter);
        waxed(Blocks.OXIDIZED_CUT_COPPER_SLAB, exporter);
        waxed(Blocks.WEATHERED_CUT_COPPER_SLAB, exporter);
        waxed(Blocks.EXPOSED_CUT_COPPER_SLAB, exporter);
        waxed(Blocks.CUT_COPPER_SLAB, exporter);
    }

    public static void waxed(@NotNull Block block, Consumer<FinishedRecipe> exporter) {
        Optional<BlockState> waxed = HoneycombItem.getWaxed(block.defaultBlockState());
        if (waxed.isPresent()) {
            BlockState state = waxed.get();
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                    .hasBlock(Blocks.HONEYCOMB_BLOCK)
                    .hasBlock(new Vec3(0.0, -2.0, 0.0), block)
                    .setBlock(new Vec3(0.0, -2.0, 0.0), state)
                    .unlockedBy(MyRecipesGenerator.hasItem(Blocks.HONEYCOMB_BLOCK.asItem()), FabricRecipeProvider.has(Blocks.HONEYCOMB_BLOCK.asItem()))
                    .save(exporter, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()));
        }
    }

    public static void squeeze(@NotNull Block block, @NotNull Block block1, @NotNull Block block2, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(block)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), Blocks.CAULDRON)
                .setBlock(block1)
                .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_1"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(block)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
                .setBlock(block1)
                .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_2"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(block)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
                .setBlock(block1)
                .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), FabricRecipeProvider.has(block.asItem()))
                .save(exporter, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_3"));
    }
}
