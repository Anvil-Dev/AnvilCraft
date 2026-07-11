package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneWireBlockItem extends BlockItem {
    public RedstoneWireBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState oldState = level.getBlockState(pos);
        if (oldState.getBlock() instanceof RedstoneWireBlock wire) {
            BlockState placementState = wire.getStateForPlacement(context);
            if (placementState != null
                && placementState.getValue(RedstoneWireBlock.ATTACHMENT)
                    != oldState.getValue(RedstoneWireBlock.ATTACHMENT)) {
                placementState = placementState.setValue(
                    RedstoneWireBlock.POWER, oldState.getValue(RedstoneWireBlock.POWER)
                );
                if (wire.reattach(level, pos, placementState)) {
                    return InteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return super.place(context);
    }
}
