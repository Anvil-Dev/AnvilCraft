package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class BlockSmearRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);

        for (Holder<Block> holder : BuiltInRegistries.BLOCK.holders().toList()) {
            HoneycombItem.getWaxed(holder.value().defaultBlockState())
                .ifPresent(state -> blockSmear(provider, Blocks.HONEYCOMB_BLOCK, holder.value(), state.getBlock()));
        }
    }

    private static void blockSmear(RegistrumRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockSmearRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }
}
