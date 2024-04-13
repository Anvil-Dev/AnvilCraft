package dev.dubhe.anvilcraft.data.generator.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.generator.AnvilCraftDatagen;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class SqueezeRecipesLoader {
    /**
     * 挤压配方
     *
     * @param provider 提供器
     */
    public static void init(RegistrateRecipeProvider provider) {
        squeeze(Blocks.MAGMA_BLOCK, Blocks.NETHERRACK, ModBlocks.LAVA_CAULDRON.get(), provider);
        squeeze(Blocks.WET_SPONGE, Blocks.SPONGE, Blocks.WATER_CAULDRON, provider);
        squeeze(Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.WATER_CAULDRON, provider);
        squeeze(Blocks.SNOW_BLOCK, Blocks.ICE, Blocks.POWDER_SNOW_CAULDRON, provider);

        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.MOSS_BLOCK)
            .hasBlock(new Vec3(0.0, -2.0, 0.0), Blocks.COBBLESTONE)
            .setBlock(new Vec3(0.0, -2.0, 0.0), Blocks.MOSSY_COBBLESTONE)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.MOSS_BLOCK.asItem()),
                AnvilCraftDatagen.has(Blocks.MOSS_BLOCK.asItem()))
            .save(provider, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK
                .getKey(Blocks.MOSSY_COBBLESTONE).getPath()));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(Blocks.MOSS_BLOCK)
            .hasBlock(new Vec3(0.0, -2.0, 0.0), Blocks.STONE_BRICKS)
            .setBlock(new Vec3(0.0, -2.0, 0.0), Blocks.MOSSY_STONE_BRICKS)
            .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.MOSS_BLOCK.asItem()),
                AnvilCraftDatagen.has(Blocks.MOSS_BLOCK.asItem()))
            .save(provider, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK
                .getKey(Blocks.MOSSY_STONE_BRICKS).getPath()));

        waxed(Blocks.COPPER_BLOCK, provider);
        waxed(Blocks.WEATHERED_COPPER, provider);
        waxed(Blocks.EXPOSED_COPPER, provider);
        waxed(Blocks.OXIDIZED_COPPER, provider);
        waxed(Blocks.OXIDIZED_CUT_COPPER, provider);
        waxed(Blocks.WEATHERED_CUT_COPPER, provider);
        waxed(Blocks.EXPOSED_CUT_COPPER, provider);
        waxed(Blocks.CUT_COPPER, provider);
        waxed(Blocks.OXIDIZED_CUT_COPPER_STAIRS, provider);
        waxed(Blocks.WEATHERED_CUT_COPPER_STAIRS, provider);
        waxed(Blocks.EXPOSED_CUT_COPPER_STAIRS, provider);
        waxed(Blocks.CUT_COPPER_STAIRS, provider);
        waxed(Blocks.OXIDIZED_CUT_COPPER_SLAB, provider);
        waxed(Blocks.WEATHERED_CUT_COPPER_SLAB, provider);
        waxed(Blocks.EXPOSED_CUT_COPPER_SLAB, provider);
        waxed(Blocks.CUT_COPPER_SLAB, provider);
    }

    /**
     * 涂蜡
     *
     * @param block    方块
     * @param provider 提供器
     */
    public static void waxed(@NotNull Block block, RegistrateRecipeProvider provider) {
        Optional<BlockState> waxed = HoneycombItem.getWaxed(block.defaultBlockState());
        if (waxed.isPresent()) {
            BlockState state = waxed.get();
            AnvilRecipe.Builder.create(RecipeCategory.MISC)
                .hasBlock(Blocks.HONEYCOMB_BLOCK)
                .hasBlock(new Vec3(0.0, -2.0, 0.0), block)
                .setBlock(new Vec3(0.0, -2.0, 0.0), state)
                .unlockedBy(AnvilCraftDatagen.hasItem(Blocks.HONEYCOMB_BLOCK.asItem()),
                    AnvilCraftDatagen.has(Blocks.HONEYCOMB_BLOCK.asItem()))
                .save(provider, AnvilCraft.of("daub/" + BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath()));
        }
    }

    /**
     * 挤压
     *
     * @param block    方块
     * @param block1   产物
     * @param block2   产物
     * @param provider 提供器
     */
    public static void squeeze(
        @NotNull Block block, @NotNull Block block1, @NotNull Block block2, RegistrateRecipeProvider provider
    ) {
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(block)
            .hasBlock(Blocks.CAULDRON, new Vec3(0.0, -2.0, 0.0))
            .setBlock(block1)
            .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 1))
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath()
                + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_1"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(block)
            .hasBlock(block2, new Vec3(0.0, -2.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 1))
            .setBlock(block1)
            .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 2))
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath()
                + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_2"));
        AnvilRecipe.Builder.create(RecipeCategory.MISC)
            .hasBlock(block)
            .hasBlock(block2, new Vec3(0.0, -2.0, 0.0), Map.entry(LayeredCauldronBlock.LEVEL, 2))
            .setBlock(block1)
            .setBlock(new Vec3(0.0, -2.0, 0.0), block2.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3))
            .unlockedBy(AnvilCraftDatagen.hasItem(block.asItem()), AnvilCraftDatagen.has(block.asItem()))
            .save(provider, AnvilCraft.of("squeeze/" + BuiltInRegistries.BLOCK.getKey(block).getPath()
                + "_" + BuiltInRegistries.BLOCK.getKey(block2).getPath() + "_3"));
    }
}
