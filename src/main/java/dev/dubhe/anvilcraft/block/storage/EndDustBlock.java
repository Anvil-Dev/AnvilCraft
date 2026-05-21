package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.entity.FloatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

public class EndDustBlock extends Block {
    public EndDustBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(
        BlockState state,
        Level level,
        BlockPos pos,
        BlockState oldState,
        boolean movedByPiston
    ) {
        level.scheduleTick(pos, this, this.getDelayAfterPlace());
    }

    @Override
    public void tick(
        BlockState state,
        ServerLevel level,
        BlockPos pos,
        RandomSource random
    ) {
        if (!level.getFluidState(pos.above()).is(FluidTags.WATER)) return;
        if (!FallingBlock.isFree(level.getBlockState(pos.above()))) return;
        FloatingBlockEntity._float(level, pos, state);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block neighborBlock,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(FluidTags.WATER)) {
                level.scheduleTick(pos, this, this.getDelayAfterPlace());
                return;
            }
        }
    }

    protected int getDelayAfterPlace() {
        return 2;
    }
}
