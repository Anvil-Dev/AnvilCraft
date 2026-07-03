package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * 管道 BlockEntity 抽象基类，提供 PipeEnd 寻路、流体传输和方块更新通知。
 * 子类：PipeBlockEntity（直管/弯管流体 tick）、PipeNodeBlockEntity（节点流体存储和分发）。
 *
 * <p>26.1 版本使用 NeoForge 新的 ResourceHandler<FluidResource> API 替代旧的 IFluidHandler。
 */
@Getter
public abstract class AbstractPipeBlockEntity extends BlockEntity {
    protected AbstractPipeBlockEntity(BlockEntityType<? extends AbstractPipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    protected void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    protected void sendNeighbourUpdate() {
        if (this.level == null) return;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    /**
     * 从指定位置出发，沿管道递归追踪 PipeEnd。
     */
    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction, int accumulatedHeight) {
        if (!level.isLoaded(blockPos)) return null;
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof PipeNodeBlock) {
            return new PipeEnd(blockPos.relative(direction.getOpposite()), direction, accumulatedHeight);
        }
        if (blockState.getBlock() instanceof PipeStraightBlock) {
            return getPipeStraightEnd(level, blockPos, blockState, direction, accumulatedHeight);
        }
        if (blockState.getBlock() instanceof PipeCornerBlock) {
            return getPipeCornerEnd(level, blockPos, blockState, direction, accumulatedHeight);
        }
        if (blockState.getBlock() instanceof PumpBlock) {
            Direction pumpOutputDir = blockState.getValue(PumpBlock.ORIENTATION).getDirection();
            if (direction == pumpOutputDir && level.getBlockEntity(blockPos) instanceof PumpBlockEntity pumpBe && pumpBe.canPump()) {
                return getPumpPipeEnd(level, blockPos, direction, accumulatedHeight);
            }
            return null;
        }
        return null;
    }

    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        return getPipeEnd(level, blockPos, direction, 0);
    }

    public static @Nullable PipeEnd getPipeStraightEnd(
        Level level, BlockPos blockPos, BlockState blockState, Direction direction, int accumulatedHeight
    ) {
        Direction.Axis axis = blockState.getValue(PipeStraightBlock.AXIS);
        if (!direction.getAxis().equals(axis)) return null;
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        boolean hasNext;
        if (direction.equals(startDir)) hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_END);
        else hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_START);
        Direction targetDir = direction.getOpposite();
        if (!hasNext) {
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return getPipeEnd(level, neighborPos, direction, accumulatedHeight);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), direction, accumulatedHeight);
    }

    public static @Nullable PipeEnd getPipeCornerEnd(
        Level level, BlockPos blockPos, BlockState blockState, Direction direction, int accumulatedHeight
    ) {
        PipeBlock.CornerEnded corner = blockState.getValue(PipeCornerBlock.CORNER_ENDED);
        if (!direction.equals(corner.getFirstDirection()) && !direction.equals(corner.getSecondDirection())) return null;
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
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return getPipeEnd(level, neighborPos, targetDir.getOpposite(), accumulatedHeight);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite(), accumulatedHeight);
    }

    private static @Nullable PipeEnd getPumpPipeEnd(Level level, BlockPos pumpPos, Direction direction, int accumulatedHeight) {
        BlockPos nextPos = pumpPos.relative(direction.getOpposite());
        if (!level.isLoaded(nextPos)) return null;
        BlockState nextState = level.getBlockState(nextPos);
        if (nextState.getBlock() instanceof PipeNodeBlock
            || nextState.getBlock() instanceof PipeStraightBlock
            || nextState.getBlock() instanceof PipeCornerBlock
            || nextState.getBlock() instanceof PumpBlock) {
            return getPipeEnd(level, nextPos, direction, accumulatedHeight + PumpBlockEntity.PUMP_HEADLIFT);
        }
        if (PipeBlock.isFluidHandler(level, nextPos)) {
            return new PipeEnd(pumpPos, direction.getOpposite(), accumulatedHeight + PumpBlockEntity.PUMP_HEADLIFT);
        }
        return null;
    }

    public static void moveFluidWithHeightCheck(
        Level level, BlockPos sourceCurPos, Direction sourceCurDirection,
        BlockPos targetCurPos, Direction targetCurDirection, int effectiveHeight
    ) {
        BlockPos sourcePos = sourceCurPos.relative(sourceCurDirection);
        BlockPos targetPos = targetCurPos.relative(targetCurDirection);
        int sourceEffectiveY = sourcePos.getY();
        int targetEffectiveY = targetPos.getY() - effectiveHeight;
        if (sourceEffectiveY <= targetEffectiveY) return;
        Direction sourceDirection = sourceCurDirection.getOpposite();
        Direction targetDirection = targetCurDirection.getOpposite();
        moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection, sourceEffectiveY - targetEffectiveY);
    }

    /**
     * 流体传输（按预先算好的等效高度）：仅在源等效高度高于目标时执行。
     *
     * <p>与 {@link #moveFluidWithHeightCheck} 不同，此方法的等效高度由调用方直接给出，
     * 且方向允许为 {@code null}——此时该端的容器即为 {@code curPos} 本身（用于节点内部储罐），
     * 容器侧取 {@code null}。
     *
     * @param sourceCurPos          源端当前位置
     * @param sourceCurDirection    源端朝向；{@code null} 表示容器即 {@code sourceCurPos}
     * @param sourceEffectiveHeight 源端容器等效高度
     * @param targetCurPos          目标端当前位置
     * @param targetCurDirection    目标端朝向；{@code null} 表示容器即 {@code targetCurPos}
     * @param targetEffectiveHeight 目标端容器等效高度
     */
    public static void moveFluidByEffectiveHeight(
        Level level,
        BlockPos sourceCurPos,
        @Nullable Direction sourceCurDirection,
        int sourceEffectiveHeight,
        BlockPos targetCurPos,
        @Nullable Direction targetCurDirection,
        int targetEffectiveHeight
    ) {
        if (sourceEffectiveHeight <= targetEffectiveHeight) {
            return;
        }
        BlockPos sourcePos = sourceCurDirection == null ? sourceCurPos : sourceCurPos.relative(sourceCurDirection);
        BlockPos targetPos = targetCurDirection == null ? targetCurPos : targetCurPos.relative(targetCurDirection);
        Direction sourceSide = sourceCurDirection == null ? null : sourceCurDirection.getOpposite();
        Direction targetSide = targetCurDirection == null ? null : targetCurDirection.getOpposite();
        moveFluid(level, sourcePos, sourceSide, targetPos, targetSide, sourceEffectiveHeight - targetEffectiveHeight);
    }

    /**
     * 使用 NeoForge 26.1 ResourceHandler<FluidResource> API 进行流体传输。
     * 替代旧的 IFluidHandler.drain()/fill() API。
     */
    public static void moveFluid(
        Level level, BlockPos sourcePos, Direction sourceDirection,
        BlockPos targetPos, Direction targetDirection, int heightDiff
    ) {
        ResourceHandler<FluidResource> source = level.getCapability(Capabilities.Fluid.BLOCK, sourcePos, sourceDirection);
        ResourceHandler<FluidResource> target = level.getCapability(Capabilities.Fluid.BLOCK, targetPos, targetDirection);
        if (source == null || target == null) return;

        int maxSpeed = Math.max(heightDiff * 50, 50);

        for (int i = 0; i < target.size(); i++) {
            FluidResource resourceInTarget = target.getResource(i);
            int amountInTarget = target.getAmountAsInt(i);

            // 计算目标槽的容量和可接受量
            int targetCapacity = target.getCapacityAsInt(i, resourceInTarget.isEmpty()
                ? FluidResource.EMPTY : resourceInTarget);
            if (targetCapacity <= 0) continue;
            int speed = Math.min(maxSpeed, targetCapacity);

            // 模拟阶段：确定可抽取的流体种类和数量
            FluidResource resourceToDrain = null;
            int requestedDrain = 0;

            if (resourceInTarget.isEmpty()) {
                // 目标空槽：遍历源的所有非空槽
                for (int j = 0; j < source.size(); j++) {
                    FluidResource srcRes = source.getResource(j);
                    if (srcRes.isEmpty() || source.getAmountAsInt(j) <= 0) continue;
                    try (Transaction tx = Transaction.openRoot()) {
                        int drained = source.extract(srcRes, speed, tx);
                        if (drained > 0) {
                            resourceToDrain = srcRes;
                            requestedDrain = drained;
                            break;
                        }
                    }
                }
            } else {
                // 目标已有流体：尝试抽取同种流体
                int want = targetCapacity - amountInTarget;
                if (want <= 0) continue;
                try (Transaction tx = Transaction.openRoot()) {
                    requestedDrain = source.extract(resourceInTarget, Math.min(want, speed), tx);
                    if (requestedDrain > 0) {
                        resourceToDrain = resourceInTarget;
                    }
                }
            }

            if (resourceToDrain == null || requestedDrain <= 0) continue;

            // 执行阶段：在同一个事务中完成抽取和填充
            try (Transaction tx = Transaction.openRoot()) {
                int actualDrained = source.extract(resourceToDrain, requestedDrain, tx);
                if (actualDrained <= 0) continue;
                target.insert(resourceToDrain, actualDrained, tx);
                tx.commit();
            }
        }
        if (heightDiff != 0) {
            TriggerUtil.connectFluidContainers(level, sourcePos);
            TriggerUtil.connectFluidContainers(level, targetPos);
        }
    }

    public static void moveFluid(Level level, BlockPos sourcePos, Direction sourceDirection,
                                  BlockPos targetPos, Direction targetDirection) {
        int heightDiff = sourcePos.getY() - targetPos.getY();
        moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection, heightDiff);
    }

    /**
     * 管道终点记录。
     */
    public record PipeEnd(BlockPos pos, Direction direction, int effectiveHeight) {}
}
