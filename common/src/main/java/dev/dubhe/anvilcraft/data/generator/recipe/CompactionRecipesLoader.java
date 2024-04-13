package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CompactionRecipesLoader {
    /**
     * 初始化压合配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        compaction(Blocks.STONE, Blocks.STONE, Blocks.DEEPSLATE, provider);
        compaction(Blocks.ICE, Blocks.ICE, Blocks.PACKED_ICE, provider);
        compaction(Blocks.PACKED_ICE, Blocks.PACKED_ICE, Blocks.BLUE_ICE, provider);
        compaction(Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK, provider);
        compaction(BlockTags.LEAVES, Blocks.DIRT, Blocks.PODZOL, provider);
        compaction(ModBlockTags.MUSHROOM_BLOCK, Blocks.DIRT, Blocks.MYCELIUM, provider);
        compaction(Blocks.NETHER_WART_BLOCK, Blocks.NETHERRACK, Blocks.CRIMSON_NYLIUM, provider);
        compaction(Blocks.WARPED_WART_BLOCK, Blocks.NETHERRACK, Blocks.WARPED_NYLIUM, provider);
    }

    /**
     * 简单压合配方
     *
     * @param block    方块1
     * @param block1   方块2
     * @param block2   产物
     * @param provider 提供器
     */
    public static void compaction(
        Block block, @NotNull Block block1, @NotNull Block block2, RegistrateRecipeProvider provider
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlockIngredient(block)
            .hasBlock(new Vec3(0.0, -2.0, 0.0), block1)
            .setBlock(new Vec3(0.0, -2.0, 0.0), block2)
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(
                provider,
                AnvilCraft.of(
                    "smash_block/"
                        + BuiltInRegistries.BLOCK.getKey(block).getPath()
                        + "_and_"
                        + BuiltInRegistries.BLOCK.getKey(block1).getPath()
                        + "_2_"
                        + BuiltInRegistries.BLOCK.getKey(block2).getPath()
                )
            );
    }

    /**
     * 简单压合配方
     *
     * @param block    方块1
     * @param block1   方块2
     * @param block2   产物
     * @param provider 提供器
     */
    public static void compaction(
        TagKey<Block> block, @NotNull Block block1, @NotNull Block block2, RegistrateRecipeProvider provider
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlockIngredient(block)
            .hasBlock(new Vec3(0.0, -2.0, 0.0), block1)
            .setBlock(new Vec3(0.0, -2.0, 0.0), block2)
            .unlockedBy(AnvilCraftDatagen.hasItem(block1.asItem()), AnvilCraftDatagen.has(block1.asItem()))
            .save(
                provider,
                AnvilCraft.of(
                    "smash_block/"
                        + block.location().getPath()
                        + "_and_"
                        + BuiltInRegistries.BLOCK.getKey(block1).getPath()
                        + "_2_"
                        + BuiltInRegistries.BLOCK.getKey(block2).getPath()
                )
            );
    }
}
