package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import lombok.Getter;
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

/**
 * 管道 BlockEntity 抽象基类，提供 PipeEnd 寻路、流体传输和方块更新通知。
 *
 * <p>子类：
 * <ul>
 *   <li>{@link PipeBlockEntity} — 直管/弯管流体 tick 逻辑</li>
 *   <li>{@link PipeNodeBlockEntity} — 节点流体存储和分发</li>
 * </ul>
 *
 * <h3>PipeEnd 寻路</h3>
 * 从管道起点出发，沿管道连接方向递归追踪，直到遇到端头（有端头即为终点，
 * 无端头则继续向下一个管道追踪）。返回终点位置和出口方向。
 *
 * <h3>流体传输</h3>
 * 从源 IFluidHandler drain，向目标 IFluidHandler fill，支持高度差
 * 限制流速（每格高度差 50 mB/tick）。
 *
 * <h3>等效高度（Effective Height）</h3>
 * 代表该段管道的等效高度偏移（泵可增大此值实现扬程）。
 * 流体传输和 PipeEnd 排序均以 {@code pos.getY() + heightBonus} 作为等效高度，
 * 而非真实 Y 坐标。
 */
@Getter
public abstract class AbstractPipeBlockEntity extends BlockEntity {
    protected AbstractPipeBlockEntity(BlockEntityType<? extends AbstractPipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 触发客户端渲染更新
     */
    protected void sendUpdate() {
        if (this.level == null) {
            return;
        }
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    /**
     * 触发邻居方块更新
     */
    protected void sendNeighbourUpdate() {
        if (this.level == null) {
            return;
        }
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    /**
     * 从指定位置出发，沿管道递归追踪 PipeEnd。
     * 按管型（节点/直管/弯管）分派到对应的追踪方法。
     * 沿途累加各管道 BlockEntity 的 {@code heightBonus}。
     *
     * @param level             世界
     * @param blockPos          起始位置
     * @param direction         追踪方向
     * @param accumulatedHeight 沿途已累积的等效高度偏移
     * @return 管道终点（位置 + 出口方向 + 累计等效高度），不可达时返回 null
     */
    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction, int accumulatedHeight) {
        if (!level.isLoaded(blockPos)) {
            return null;
        }
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
                // 泵可工作且方向匹配 → 等效距离 +10 并继续追踪
                return getPumpPipeEnd(level, blockPos, direction, accumulatedHeight);
            }
            // 方向不匹配或泵不能工作 → 清空等效距离并返回
            return null;
        }
        return null;
    }

    /**
     * 无累计高度的便捷调用
     */
    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        return getPipeEnd(level, blockPos, direction, 0);
    }

    /**
     * 在直管中沿管道方向递归追踪 PipeEnd。
     * <ul>
     *   <li>追踪方向与轴不同 → 不可达（null）</li>
     *   <li>追踪方向的对端有端头 → 终点即为当前位置</li>
     *   <li>追踪方向的对端无端头 → 递归到下一个管道</li>
     * </ul>
     */
    public static @Nullable PipeEnd getPipeStraightEnd(
        Level level,
        BlockPos blockPos,
        BlockState blockState,
        Direction direction,
        int accumulatedHeight
    ) {
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
            // 检查端头指向的方块是否是泵，若是则继续追踪
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return getPipeEnd(level, neighborPos, direction, accumulatedHeight);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), direction, accumulatedHeight);
    }

    /**
     * 在弯管中沿管道方向递归追踪 PipeEnd。
     * <ul>
     *   <li>追踪方向与弯管两方向都不同 → 不可达（null）</li>
     *   <li>追踪方向的对端有端头 → 终点即为当前位置</li>
     *   <li>追踪方向的对端无端头 → 转向弯管的另一方向继续追踪</li>
     * </ul>
     */
    public static @Nullable PipeEnd getPipeCornerEnd(
        Level level,
        BlockPos blockPos,
        BlockState blockState,
        Direction direction,
        int accumulatedHeight
    ) {
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
            // 检查端头指向的方块是否是泵，若是则继续追踪
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return getPipeEnd(level, neighborPos, targetDir.getOpposite(), accumulatedHeight);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite(), accumulatedHeight);
    }

    /**
     * 从泵的输出端继续追踪 PipeEnd。
     * <ul>
     *   <li>相邻方块是管道/泵 → 继续递归追踪</li>
     *   <li>相邻方块是 IFluidHandler → 返回 PipeEnd（泵输出端直接对其排液）</li>
     *   <li>其他 → null（无有效终点）</li>
     * </ul>
     *
     * @param level             世界
     * @param pumpPos           泵的位置
     * @param direction         追踪方向（泵的输出方向）
     * @param accumulatedHeight 已累积的等效高度（含泵的 heightBonus）
     * @return PipeEnd，不可达时返回 null
     */
    private static @Nullable PipeEnd getPumpPipeEnd(Level level, BlockPos pumpPos, Direction direction, int accumulatedHeight) {
        BlockPos nextPos = pumpPos.relative(direction.getOpposite());
        if (!level.isLoaded(nextPos)) return null;

        BlockState nextState = level.getBlockState(nextPos);

        // 若相邻是管道或泵，继续管道追踪
        if (
            nextState.getBlock() instanceof PipeNodeBlock
            || nextState.getBlock() instanceof PipeStraightBlock
            || nextState.getBlock() instanceof PipeCornerBlock
            || nextState.getBlock() instanceof PumpBlock
        ) {
            return getPipeEnd(level, nextPos, direction, accumulatedHeight + PumpBlockEntity.PUMP_HEADLIFT);
        }

        // 若相邻是流体处理器，泵输出端直接对其排液
        if (PipeBlock.isFluidHandler(level, nextPos)) {
            return new PipeEnd(pumpPos, direction.getOpposite(), accumulatedHeight + PumpBlockEntity.PUMP_HEADLIFT);
        }

        return null;
    }

    /**
     * 流体传输（带高度差检查）：使用等效高度替代真实 Y 坐标。
     * 仅在源等效高度高于目标等效高度时执行传输。
     *
     * <p>高度差基于移位后的实际连接位置（sourcePos/targetPos），
     * 而非管道自身位置，以正确处理同方块两端端头的情况。
     */
    public static void moveFluidWithHeightCheck(
        Level level,
        BlockPos sourceCurPos,
        Direction sourceCurDirection,
        BlockPos targetCurPos,
        Direction targetCurDirection,
        int effectiveHeight
    ) {
        BlockPos sourcePos = sourceCurPos.relative(sourceCurDirection);
        BlockPos targetPos = targetCurPos.relative(targetCurDirection);

        // 用移位后的实际连接位置计算基础高度
        int sourceEffectiveY = sourcePos.getY();
        int targetEffectiveY = targetPos.getY();

        targetEffectiveY -= effectiveHeight;

        if (sourceEffectiveY <= targetEffectiveY) return;

        Direction sourceDirection = sourceCurDirection.getOpposite();
        Direction targetDirection = targetCurDirection.getOpposite();
        moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection, sourceEffectiveY - targetEffectiveY);
    }

    /**
     * 使用高度差计算流速的流体传输
     */
    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        Direction sourceDirection,
        BlockPos targetPos,
        Direction targetDirection,
        int heightDiff
    ) {
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceDirection);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetDirection);
        if (source == null || target == null || target.equals(source)) {
            return;
        }

        int maxSpeed = heightDiff * 50; // 每格等效高度差 50 mB/tick

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

    /**
     * 便捷调用（无高度差时用真实Y差）
     */
    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        Direction sourceDirection,
        BlockPos targetPos,
        Direction targetDirection
    ) {
        int heightDiff = sourcePos.getY() - targetPos.getY();
        moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection, heightDiff);
    }

    /**
     * 管道终点记录。
     *
     * @param pos             终点方块位置
     * @param direction       出口方向（从终点指向接收方的方向）
     * @param effectiveHeight 沿途累计的等效高度（= 各段 heightBonus 总和 + 终点真实Y）
     */
    public record PipeEnd(BlockPos pos, Direction direction, int effectiveHeight) {
    }
}
