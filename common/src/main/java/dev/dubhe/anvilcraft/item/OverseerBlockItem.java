package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class OverseerBlockItem extends BlockItem {

    public OverseerBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean canPlace(BlockPlaceContext context, @NotNull BlockState state) {
        Level level = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        return level.getBlockState(blockPos.above()).is(BlockTags.REPLACEABLE)
            && super.canPlace(context, state);
    }
}
