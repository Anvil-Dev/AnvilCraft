package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class PipeBlockEntity extends AbstractPipeBlockEntity {
    protected PipeBlockEntity(
        BlockEntityType<PipeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public static PipeBlockEntity create(BlockEntityType<PipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeBlockEntity(type, pos, blockState);
    }

    public static int getEndCount(BlockState blockState) {
        if (!(blockState.getBlock() instanceof PipeStraightBlock) && !(blockState.getBlock() instanceof PipeCornerBlock)) {
            return -1;
        }
        int count = 0;
        if (blockState.getValue(PipeStraightBlock.HAS_END_START)) count++;
        if (blockState.getValue(PipeStraightBlock.HAS_END_END)) count++;
        return count;
    }

    public static void tick(Level level, BlockPos pos, BlockState state) {
        int endCount = PipeBlockEntity.getEndCount(state);
        if (endCount <= 0) {
            return;
        }
        boolean isStraight = state.getBlock() instanceof PipeStraightBlock;
        if (endCount == 2 && isStraight && Direction.Axis.Y.equals(state.getValue(PipeStraightBlock.AXIS))) {
            return;
        }
        if (
            endCount == 2
            && !isStraight
            && !state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection().equals(Direction.DOWN)
            && !state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection().equals(Direction.UP)
        ) {
            return;
        }
        if (endCount == 2) {
            if (isStraight) {
                AbstractPipeBlockEntity.moveFluidWithHeightCheck(level, pos, Direction.UP, pos, Direction.DOWN);
            } else {
                if (state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection().equals(Direction.DOWN)) {
                    PipeBlock.CornerEnded cornerEnded = state.getValue(PipeCornerBlock.CORNER_ENDED);
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(level, pos, cornerEnded.getSecondDirection(), pos, Direction.DOWN);
                } else {
                    PipeBlock.CornerEnded cornerEnded = state.getValue(PipeCornerBlock.CORNER_ENDED);
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(level, pos, Direction.UP, pos, cornerEnded.getSecondDirection());
                }
            }
            return;
        }
        Direction sourceDirection;
        boolean hasEndStart = state.getValue(PipeBlock.HAS_END_START);
        if (isStraight) {
            if (hasEndStart) {
                sourceDirection = PipeBlock.getDirectionFromAxis(state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.NEGATIVE);
            } else {
                sourceDirection = PipeBlock.getDirectionFromAxis(state.getValue(PipeStraightBlock.AXIS), Direction.AxisDirection.POSITIVE);
            }
        } else {
            if (hasEndStart) {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getFirstDirection();
            } else {
                sourceDirection = state.getValue(PipeCornerBlock.CORNER_ENDED).getSecondDirection();
            }
        }
        PipeEnd pipeEnd = PipeBlockEntity.getPipeEnd(level, pos, sourceDirection);
        if (pipeEnd == null) {
            return;
        }
        AbstractPipeBlockEntity.moveFluidWithHeightCheck(level, pos, sourceDirection, pipeEnd.pos(), pipeEnd.direction());
    }
}
