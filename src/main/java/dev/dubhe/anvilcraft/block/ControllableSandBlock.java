package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.entity.StandableFallingBlockEntity;
import dev.dubhe.anvilcraft.entity.StandableLevitatingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ControllableSandBlock extends Block {
    public ControllableSandBlock(Properties properties) {
        super(properties);
    }

    @SuppressWarnings("ConstantValue")
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBestNeighborSignal(pos) > 0) {
            BlockState above = level.getBlockState(pos.above());
            BlockState below = level.getBlockState(pos.below());
            boolean aboveIsFree = FallingBlock.isFree(above);
            boolean belowIsFree = FallingBlock.isFree(below);
            if (!aboveIsFree && !belowIsFree) return;
            if (aboveIsFree && belowIsFree) {
                if (random.nextIntBetweenInclusive(1, 100) > 50) aboveIsFree = false;
                else belowIsFree = false;
            }

            if (!aboveIsFree) {
                StandableFallingBlockEntity.fall(level, pos, state);
            } else if (!belowIsFree) {
                StandableLevitatingBlockEntity.levitate(level, pos, state);
            }
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.getBestNeighborSignal(pos) > 0) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    @Override
    protected void neighborChanged(
        BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston
    ) {
        if (level.getBestNeighborSignal(pos) > 0) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    protected int getDelayAfterPlace() {
        return 2;
    }
}
