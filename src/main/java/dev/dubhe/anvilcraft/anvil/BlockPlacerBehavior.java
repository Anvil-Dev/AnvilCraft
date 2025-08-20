package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.BlockPlacerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BlockPlacerBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        BlockPlacerBlock block = (BlockPlacerBlock) hitBlockState.getBlock();
        int distance = Math.min((int) event.getFallDistance() + 2, 5);
        level.setBlock(hitBlockPos, hitBlockState.setValue(BlockPlacerBlock.TRIGGERED, true), 2);
        block.placeBlock(distance, level, hitBlockPos, hitBlockState.getValue(BlockPlacerBlock.ORIENTATION));
        level.scheduleTick(hitBlockPos, block, 4);
        return true;
    }
}
