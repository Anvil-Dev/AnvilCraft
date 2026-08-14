package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrindstoneBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

public class BlockSmearRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        blockSmear(provider, Blocks.MOSS_BLOCK, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);

        for (Holder<Block> holder : BuiltInRegistries.BLOCK.holders().toList()) {
            HoneycombItem.getWaxed(holder.value().defaultBlockState())
                .ifPresent(state -> blockSmear(provider, Blocks.HONEYCOMB_BLOCK, holder.value(), state.getBlock()));
        }

        /// 倒置的砂轮可以打磨下方的铜类方块和原木：去蜡、去锈、去皮。
        BlockStatePredicate invertedGrindstone = BlockStatePredicate.builder()
            .of(
                Blocks.GRINDSTONE,
                ModBlocks.EMBER_GRINDSTONE.get(),
                ModBlocks.FROST_GRINDSTONE.get(),
                ModBlocks.ROYAL_GRINDSTONE.get(),
                ModBlocks.TRANSCENDENCE_GRINDSTONE.get()
            )
            .with(GrindstoneBlock.FACING, Direction.DOWN)
            .build();

        for (Holder<Block> holder : BuiltInRegistries.BLOCK.holders().toList()) {
            Block block = holder.value();
            Block waxedOff = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(block);
            if (waxedOff != null) {
                blockSmear(provider, invertedGrindstone, block, waxedOff, "dewax");
            }
            WeatheringCopper.getPrevious(block.defaultBlockState())
                .ifPresent(state -> blockSmear(provider, invertedGrindstone, block, state.getBlock(), "deoxidize"));
            BlockState stripped = AxeItem.getAxeStrippingState(block.defaultBlockState());
            if (stripped != null) {
                blockSmear(provider, invertedGrindstone, block, stripped.getBlock(), "strip");
            }
        }
    }

    private static void blockSmear(RegistrumRecipeProvider provider, Block block1, Block block2, Block result) {
        BlockSmearRecipe.builder().input(block1).input(block2).result(result).save(provider);
    }

    private static void blockSmear(
        RegistrumRecipeProvider provider,
        BlockStatePredicate input1,
        Block input2,
        Block result,
        String suffix
    ) {
        String id = BuiltInRegistries.BLOCK.getKey(result).getPath() + "_" + suffix;
        BlockSmearRecipe.builder().input(input1).input(input2).result(result).save(provider, id);
    }
}
