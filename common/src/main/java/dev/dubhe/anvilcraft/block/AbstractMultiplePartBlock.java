package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.state.MultiplePartBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMultiplePartBlock extends Block {
    public AbstractMultiplePartBlock(Properties properties) {
        super(properties);
    }

    protected abstract <P extends Enum<P>> Enum<P>[] getParts();

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        for (Enum<?> part : getParts()) {
            if (part instanceof MultiplePartBlockState state1) {
                state1.getOffset();
            }
        }
    }
}
