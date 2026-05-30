package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

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

    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        if (!level.isLoaded(blockPos)) return null;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof PipeNodeBlock) {
            return new PipeEnd(blockPos, null);
        }
        if (blockState.getBlock() instanceof PipeStraightBlock) {
            return getPipeStraightEnd(level, blockPos, blockState, direction);
        }
        if (blockState.getBlock() instanceof PipeCornerBlock) {
            return getPipeCornerEnd(level, blockPos, blockState, direction);
        }
        return null;
    }

    public static @Nullable PipeEnd getPipeStraightEnd(Level level, BlockPos blockPos, BlockState blockState, Direction direction) {
        Direction.Axis axis = blockState.getValue(PipeStraightBlock.AXIS);
        if (!direction.getAxis().equals(axis)) {
            return null;
        }
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        boolean hasNext;
        if (direction.equals(startDir)) {
            hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_END);
        } else {
            hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_START);
        }
        Direction targetDir = direction.getOpposite();
        if (!hasNext) {
            return new PipeEnd(blockPos, targetDir);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), direction);
    }

    public static @Nullable PipeEnd getPipeCornerEnd(Level level, BlockPos blockPos, BlockState blockState, Direction direction) {
        PipeBlock.CornerEnded corner = blockState.getValue(PipeCornerBlock.CORNER_ENDED);
        if (!direction.equals(corner.getFirstDirection()) && !direction.equals(corner.getSecondDirection())) {
            return null;
        }
        Direction startDir = corner.getFirstDirection();
        boolean hasNext;
        Direction targetDir;
        if (direction.equals(startDir)) {
            hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_END);
            targetDir = corner.getSecondDirection();
        } else {
            hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_START);
            targetDir = startDir;
        }
        if (!hasNext) {
            return new PipeEnd(blockPos, targetDir);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite());
    }

    public record PipeEnd(BlockPos pos, @Nullable Direction direction) {
    }
}
