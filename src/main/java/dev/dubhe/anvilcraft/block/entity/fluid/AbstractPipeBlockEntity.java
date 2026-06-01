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
 */
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
     *
     * @param level     世界
     * @param blockPos  起始位置
     * @param direction 追踪方向
     * @return 管道终点（位置 + 出口方向），不可达时返回 null
     */
    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        if (!level.isLoaded(blockPos)) {
            return null;
        }
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.getBlock() instanceof PipeNodeBlock) {
            // 节点：下一跳位置 = 节点相对方向的位置，方向不变
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

    /**
     * 在直管中沿管道方向递归追踪 PipeEnd。
     * <ul>
     *   <li>追踪方向与轴不同 → 不可达（null）</li>
     *   <li>追踪方向的对端有端头 → 终点即为当前位置</li>
     *   <li>追踪方向的对端无端头 → 递归到下一个管道</li>
     * </ul>
     *
     * @param level      世界
     * @param blockPos   当前管道位置
     * @param blockState 当前管道状态
     * @param direction  追踪方向
     * @return 管道终点
     */
    public static @Nullable PipeEnd getPipeStraightEnd(Level level, BlockPos blockPos, BlockState blockState, Direction direction) {
        Direction.Axis axis = blockState.getValue(PipeStraightBlock.AXIS);
        // 追踪方向必须与轴向相同
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
            // 有端头 → 终点
            return new PipeEnd(blockPos, targetDir);
        }
        // 无端头 → 继续追踪下一个管道
        return PipeBlockEntity.getPipeEnd(level, blockPos.relative(targetDir), direction);
    }

    /**
     * 在弯管中沿管道方向递归追踪 PipeEnd。
     * <ul>
     *   <li>追踪方向与弯管两方向都不同 → 不可达（null）</li>
     *   <li>追踪方向的对端有端头 → 终点即为当前位置</li>
     *   <li>追踪方向的对端无端头 → 转向弯管的另一方向继续追踪</li>
     * </ul>
     */
    public static @Nullable PipeEnd getPipeCornerEnd(Level level, BlockPos blockPos, BlockState blockState, Direction direction) {
        PipeBlock.CornerEnded corner = blockState.getValue(PipeCornerBlock.CORNER_ENDED);
        // 追踪方向必须是弯管两方向之一
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
            // 有端头 → 终点
            return new PipeEnd(blockPos, targetDir);
        }
        // 无端头 → 转向弯管另一方向继续追踪
        return PipeBlockEntity.getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite());
    }

    /**
     * 流体传输（带高度差检查）：仅在源位置高于目标位置时执行传输。
     */
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
            return; // 源不高于目标 → 跳过
        }
        Direction sourceDirection = sourceCurDirection.getOpposite();
        Direction targetDirection = targetCurDirection.getOpposite();
        PipeBlockEntity.moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection);
    }

    /**
     * 执行流体传输：从源 drain → 向目标 fill。
     * 流速受高度差限制：每格 50 mB/tick。
     * 支持多 tank 目标逐个填充。
     *
     * @param level           世界
     * @param sourcePos       源位置
     * @param sourceDirection 源方向
     * @param targetPos       目标位置
     * @param targetDirection 目标方向
     */
    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        Direction sourceDirection,
        BlockPos targetPos,
        Direction targetDirection
    ) {
        // 通过 NeoForge Capability 获取流体处理器
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, sourceDirection);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, targetPos, targetDirection);
        if (source == null || target == null || target.equals(source)) {
            return;
        }

        int heightDiff = sourcePos.getY() - targetPos.getY();
        int maxSpeed = heightDiff * 50; // 每格高度差 50 mB/tick

        for (int i = 0; i < target.getTanks(); i++) {
            int targetTankCapacity = target.getTankCapacity(i);
            int speed = Math.min(maxSpeed, targetTankCapacity);
            FluidStack fluidInTargetTank = target.getFluidInTank(i);
            FluidStack drain;

            if (fluidInTargetTank.isEmpty()) {
                // 目标槽为空：尝试 drain 任意流体
                drain = source.drain(speed, IFluidHandler.FluidAction.SIMULATE);
            } else {
                // 目标槽有流体：尝试 drain 同种流体补充
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

            // 实际执行
            FluidStack drainFluid = drain.copyWithAmount(filled);
            drainFluid = source.drain(drainFluid, IFluidHandler.FluidAction.EXECUTE);
            target.fill(drainFluid, IFluidHandler.FluidAction.EXECUTE);
        }
    }

    /**
     * 管道终点记录，包含终点位置和出口方向（从该位置指向接收方）。
     *
     * @param pos       终点方块位置
     * @param direction 出口方向（从终点指向接收方的方向）
     */
    public record PipeEnd(BlockPos pos, Direction direction) {
    }
}
