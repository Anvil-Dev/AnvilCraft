package dev.dubhe.anvilcraft.data.generator.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.generator.MyRecipesGenerator;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class CompactionRecipesGenerator {
    private CompactionRecipesGenerator() {
    }

    public static void buildRecipes(Consumer<FinishedRecipe> exporter) {
        compaction(Blocks.STONE, Blocks.STONE, Blocks.DEEPSLATE, exporter);
        compaction(Blocks.ICE, Blocks.ICE, Blocks.PACKED_ICE, exporter);
        compaction(Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, exporter);
        compaction(Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK, exporter);
        compaction(BlockTags.LEAVES, Blocks.DIRT, Blocks.PODZOL, exporter);
        compaction(ModBlockTags.MUSHROOM_BLOCK, Blocks.DIRT, Blocks.MYCELIUM, exporter);
        compaction(Blocks.NETHER_WART_BLOCK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, exporter);
        compaction(Blocks.WARPED_WART_BLOCK, Blocks.NETHERRACK, Blocks.WARPED_NYLIUM, exporter);
    }

    public static void compaction(Block block, @NotNull Block block1, @NotNull Block block2, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlockIngredient(block)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), block1)
                .setBlock(new Vec3(0.0, -2.0, 0.0), block2)
                .unlockedBy(MyRecipesGenerator.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
                .save(exporter, AnvilCraft.of("smash_block/" + BuiltInRegistries.BLOCK.getKey(block).getPath() + "_and_" + BuiltInRegistries.BLOCK.getKey(block1).getPath() + "_2_" + BuiltInRegistries.BLOCK.getKey(block2).getPath()));
    }

    public static void compaction(TagKey<Block> block, @NotNull Block block1, @NotNull Block block2, Consumer<FinishedRecipe> exporter) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlockIngredient(block)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), block1)
                .setBlock(new Vec3(0.0, -2.0, 0.0), block2)
                .unlockedBy(MyRecipesGenerator.hasItem(block1.asItem()), AnvilCraftDatagen.has(block1.asItem()))
                .save(exporter, AnvilCraft.of("smash_block/" + block.location().getPath() + "_and_" + BuiltInRegistries.BLOCK.getKey(block1).getPath() + "_2_" + BuiltInRegistries.BLOCK.getKey(block2).getPath()));
    }
}
