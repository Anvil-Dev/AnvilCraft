package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.generators.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockSmearRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        BlockSmearRecipeLoader.blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        BlockSmearRecipeLoader.blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        BlockSmearRecipeLoader.blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK);

        for (Holder<Block> holder : provider.getRegistries().lookupOrThrow(Registries.BLOCK).listElements().toList()) {
            HoneycombItem.getWaxed(holder.value().defaultBlockState())
                .ifPresent(state -> BlockSmearRecipeLoader.blockSmear(provider, Blocks.HONEYCOMB_BLOCK, holder.value(), state.getBlock()));
        }
    }

    private static void blockSmear(RegistrumRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockSmearRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }
}
