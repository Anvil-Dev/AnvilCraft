package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
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
 * 每个管道 BlockEntity 持有一个 {@link #heightBonus} 字段，
 * 代表该段管道的等效高度偏移（泵可增大此值实现扬程）。
 * 流体传输和 PipeEnd 排序均以 {@code pos.getY() + heightBonus} 作为等效高度，
 * 而非真实 Y 坐标。
 */
@Getter
public abstract class AbstractPipeBlockEntity extends BlockEntity {

    /**
     * 本段管道的等效高度偏移。泵等设备可设置为正值以实现扬程。
     * 最终等效高度 = {@code getBlockPos().getY() + heightBonus}。
     */
    protected int heightBonus;

    protected AbstractPipeBlockEntity(BlockEntityType<? extends AbstractPipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 本段管道的等效高度
     *
     * @return 本段管道的等效高度（真实Y + 偏移）
     */
    public int getEffectiveHeight() {
        return this.getBlockPos().getY() + this.heightBonus;
    }

    /**
     * 设置本段管道的等效高度偏移。
     *
     * @param bonus 偏移量（正值提高等效高度，实现扬程）
     */
    public void setHeightBonus(int bonus) {
        if (this.heightBonus != bonus) {
            this.heightBonus = bonus;
            this.setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.sendUpdate();
            }
        }
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

        // 累加当前位置管道的高度偏移
        int bonus = 0;
        if (level.getBlockEntity(blockPos) instanceof AbstractPipeBlockEntity pipeBe) {
            bonus = pipeBe.getHeightBonus();
        }

        if (blockState.getBlock() instanceof PipeNodeBlock) {
            return new PipeEnd(blockPos.relative(direction.getOpposite()), direction, accumulatedHeight + bonus + blockPos.getY());
        }
        if (blockState.getBlock() instanceof PipeStraightBlock) {
            return getPipeStraightEnd(level, blockPos, blockState, direction, accumulatedHeight + bonus);
        }
        if (blockState.getBlock() instanceof PipeCornerBlock) {
            return getPipeCornerEnd(level, blockPos, blockState, direction, accumulatedHeight + bonus);
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
            return new PipeEnd(blockPos, targetDir, accumulatedHeight + blockPos.getY());
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
            return new PipeEnd(blockPos, targetDir, accumulatedHeight + blockPos.getY());
        }
        return getPipeEnd(level, blockPos.relative(targetDir), targetDir.getOpposite(), accumulatedHeight);
    }

    /**
     * 流体传输（带高度差检查）：使用等效高度替代真实 Y 坐标。
     * 仅在源等效高度高于目标等效高度时执行传输。
     */
    public static void moveFluidWithHeightCheck(
        Level level,
        BlockPos sourceCurPos,
        Direction sourceCurDirection,
        BlockPos targetCurPos,
        Direction targetCurDirection
    ) {
        // 计算源的等效高度
        int sourceEffectiveY = sourceCurPos.getY();
        int targetEffectiveY = targetCurPos.getY();
        if (level.getBlockEntity(sourceCurPos) instanceof AbstractPipeBlockEntity sourceBe) {
            sourceEffectiveY = sourceBe.getEffectiveHeight();
        }
        if (level.getBlockEntity(targetCurPos) instanceof AbstractPipeBlockEntity targetBe) {
            targetEffectiveY = targetBe.getEffectiveHeight();
        }

        if (sourceEffectiveY <= targetEffectiveY) {
            return;
        }

        BlockPos sourcePos = sourceCurPos.relative(sourceCurDirection);
        BlockPos targetPos = targetCurPos.relative(targetCurDirection);
        Direction sourceDirection = sourceCurDirection.getOpposite();
        Direction targetDirection = targetCurDirection.getOpposite();
        // 使用等效高度差计算流速
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
