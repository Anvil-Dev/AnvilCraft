package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public abstract class AbstractPipeBlockEntity extends BlockEntity {
    protected AbstractPipeBlockEntity(
        BlockEntityType<? extends AbstractPipeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    protected void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    protected void sendNeighbourUpdate() {
        if (this.level == null) return;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        if (!level.isLoaded(blockPos)) return null;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof PipeNodeBlock) {
            return new PipeEnd(blockPos.relative(direction.getOpposite()), direction);
        }
        if (blockState.getBlock() instanceof PipeStraightBlock) {
            return PipeBlockEntity.getPipeStraightEnd(level, blockPos, blockState, direction);
        }
        if (blockState.getBlock() instanceof PipeCornerBlock) {
            return PipeBlockEntity.getPipeCornerEnd(level, blockPos, blockState, direction);
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
        return PipeBlockEntity.getPipeEnd(level, blockPos.relative(targetDir), direction);
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
        return PipeBlockEntity.getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite());
    }

    public static void moveFluidWithHeightCheck(
        Level level,
        BlockPos sourceCurPos,
        Direction sourceCurDirection,
        BlockPos targetCurPos,
        Direction targetCurDirection
    ) {
        BlockPos sourcePos = sourceCurPos.relative(sourceCurDirection);
        BlockPos targetPos = targetCurPos.relative(targetCurDirection);
        if (sourcePos.getY() <= targetPos.getY()) {
            return;
        }
        Direction sourceDirection = sourceCurDirection.getOpposite();
        Direction targetDirection = targetCurDirection.getOpposite();
        PipeBlockEntity.moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection);
    }

    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        Direction sourceDirection,
        BlockPos targetPos,
        Direction targetDirection
    ) {
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceDirection);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetDirection);
        int heightDiff = sourcePos.getY() - targetPos.getY();
        int maxSpeed = heightDiff * 50;
        if (source == null || target == null) {
            return;
        }
        for (int i = 0; i < target.getTanks(); i++) {
            int targetTankCapacity = target.getTankCapacity(i);
            int speed = Math.min(maxSpeed, targetTankCapacity);
            FluidStack fluidInTargetTank = target.getFluidInTank(i);
            FluidStack drain;
            if (fluidInTargetTank.isEmpty()) {
                drain = source.drain(speed, IFluidHandler.FluidAction.SIMULATE);
            } else {
                int want = targetTankCapacity - fluidInTargetTank.getAmount();
                FluidStack tryDrainFluid = fluidInTargetTank.copyWithAmount(Math.min(want, speed));
                drain = source.drain(tryDrainFluid, IFluidHandler.FluidAction.SIMULATE);
            }
            if (drain.isEmpty()) {
                continue;
            }
            int filled = target.fill(drain, IFluidHandler.FluidAction.SIMULATE);
            if (filled <= 0) {
                continue;
            }
            FluidStack drainFluid = drain.copyWithAmount(filled);
            drainFluid = source.drain(drainFluid, IFluidHandler.FluidAction.EXECUTE);
            target.fill(drainFluid, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    public record PipeEnd(BlockPos pos, Direction direction) {
    }
}
