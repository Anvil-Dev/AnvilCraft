package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.BlockSmearRecipe;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public class BlockSmearRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.DIRT, Blocks.GRASS_BLOCK);

        for (Holder<Block> holder : BuiltInRegistries.BLOCK.holders().toList()) {
            Optional<BlockState> waxed = HoneycombItem.getWaxed(holder.value().defaultBlockState());
            waxed.ifPresent(state -> blockSmear(provider, Blocks.HONEYCOMB_BLOCK, holder.value(), state.getBlock()));
        }
    }

    private static void blockSmear(RegistrateRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockSmearRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }
}
