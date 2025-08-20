package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.BlockDevourerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class BlockDevourerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockDevourerBlock block = (BlockDevourerBlock) hitBlockState.getBlock();
        int range = Math.min((int) fallDistance + 2, 4);
        level.setBlock(hitBlockPos, hitBlockState.setValue(BlockDevourerBlock.TRIGGERED, true), 2);
        if (
            hitBlockState.getValue(BlockDevourerBlock.FACING) == Direction.DOWN
                && level.isOutsideBuildHeight(hitBlockPos.below())
        ) {
            level.scheduleTick(hitBlockPos, block, 4);
            return true;
        }
        block.devourBlock(
            serverLevel,
            hitBlockPos,
            hitBlockState.getValue(BlockDevourerBlock.FACING),
            range,
            event.getEntity().getBlockState().getBlock()
        );
        if (
            hitBlockState.getValue(BlockDevourerBlock.FACING) == Direction.DOWN
                && level.getBlockState(hitBlockPos.below()).getBlock().defaultDestroyTime() >= 0
        ) {
            level.setBlock(hitBlockPos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(hitBlockPos.below(), hitBlockState.setValue(BlockDevourerBlock.TRIGGERED, true), 2);
            level.scheduleTick(hitBlockPos.below(), block, 4);
        }
        level.scheduleTick(hitBlockPos, block, 4);
        return true;
    }
}
