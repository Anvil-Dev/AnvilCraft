package dev.dubhe.anvilcraft.block.entity.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeCornerBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeStraightBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * 管道 BlockEntity 抽象基类，提供 PipeEnd 寻路、流体传输和方块更新通知。
 * 子类：PipeBlockEntity（直管/弯管流体 tick）、PipeNodeBlockEntity（节点流体存储和分发）。
 *
 * <p>26.1 版本使用 NeoForge 新的 ResourceHandler<FluidResource> API 替代旧的 IFluidHandler。
 */
@Getter
public abstract class AbstractPipeBlockEntity extends BlockEntity {
    private static final String TAG_POWERED = "Powered";
    private static final String TAG_VALVES = "Valves";

    private static final Codec<Direction> DIRECTION_CODEC = Codec.INT.xmap(
        Direction::from3DDataValue,
        Direction::get3DDataValue
    );

    private final Map<Direction, Direction> baseFlow = new EnumMap<>(Direction.class);
    private boolean powered = false;

    protected AbstractPipeBlockEntity(BlockEntityType<? extends AbstractPipeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public boolean hasValveOn(Direction face) {
        return this.baseFlow.containsKey(face);
    }

    public boolean isEmpty() {
        return this.baseFlow.isEmpty();
    }

    public void setValve(Direction face, Direction flowOut) {
        this.baseFlow.put(face, flowOut);
        this.setChanged();
    }

    public void removeValve(Direction face) {
        this.baseFlow.remove(face);
        this.setChanged();
    }

    @Nullable
    public Direction getBaseFlow(Direction face) {
        return this.baseFlow.get(face);
    }

    @Nullable
    public Direction effectiveFlow(Direction face) {
        Direction base = this.baseFlow.get(face);
        if (base == null) return null;
        return this.powered ? base.getOpposite() : base;
    }

    public Map<Direction, Direction> effectiveFlows() {
        Map<Direction, Direction> result = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Direction> entry : this.baseFlow.entrySet()) {
            result.put(entry.getKey(), this.powered ? entry.getValue().getOpposite() : entry.getValue());
        }
        return result;
    }

    public Map<Direction, Direction> baseFlowCopy() {
        return new EnumMap<>(this.baseFlow);
    }

    public void restore(Map<Direction, Direction> saved, boolean powered) {
        this.baseFlow.clear();
        this.baseFlow.putAll(saved);
        this.powered = powered;
        this.setChanged();
    }

    public boolean setPowered(boolean powered) {
        if (this.powered == powered) return false;
        this.powered = powered;
        this.setChanged();
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean(AbstractPipeBlockEntity.TAG_POWERED, this.powered);
        this.writeValves(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.powered = input.getBooleanOr(AbstractPipeBlockEntity.TAG_POWERED, false);
        this.readValves(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        output.putBoolean(AbstractPipeBlockEntity.TAG_POWERED, this.powered);
        this.writeValves(output);
        return output.buildResult();
    }

    private void writeValves(ValueOutput output) {
        ValueOutput.TypedOutputList<ValveData> list = output.list(AbstractPipeBlockEntity.TAG_VALVES, ValveData.CODEC);
        for (Map.Entry<Direction, Direction> entry : this.baseFlow.entrySet()) {
            list.add(new ValveData(entry.getKey(), entry.getValue()));
        }
    }

    private void readValves(ValueInput input) {
        this.baseFlow.clear();
        for (ValveData valve : input.listOrEmpty(AbstractPipeBlockEntity.TAG_VALVES, ValveData.CODEC)) {
            this.baseFlow.put(valve.face(), valve.flow());
        }
    }

    public void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
    }

    protected void sendNeighbourUpdate() {
        if (this.level == null) return;
        this.level.updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
    }

    public static boolean canFlowThroughCheckValve(Level level, BlockPos pipePos, Direction face, Direction flowDirection) {
        if (!level.isLoaded(pipePos)) return false;
        if (level.getBlockEntity(pipePos) instanceof AbstractPipeBlockEntity pipe) {
            Direction allowed = pipe.effectiveFlow(face);
            return allowed == null || allowed == flowDirection;
        }
        return true;
    }

    /**
     * 从指定位置出发，沿管道递归追踪 PipeEnd。
     */
    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction, int accumulatedHeight) {
        return AbstractPipeBlockEntity.getPipeEnd(level, blockPos, direction, accumulatedHeight, true);
    }

    public static @Nullable PipeEnd getPipeEnd(
        Level level, BlockPos blockPos, Direction direction, int accumulatedHeight, boolean checkValves
    ) {
        if (!level.isLoaded(blockPos)) return null;
        BlockState blockState = level.getBlockState(blockPos);
        if (checkValves
            && blockState.getBlock() instanceof PipeBlock
            && !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, blockPos, direction, direction.getOpposite())) {
            return null;
        }
        if (blockState.getBlock() instanceof PipeNodeBlock) {
            return new PipeEnd(blockPos.relative(direction.getOpposite()), direction, accumulatedHeight);
        }
        if (blockState.getBlock() instanceof PipeStraightBlock) {
            return AbstractPipeBlockEntity.getPipeStraightEnd(level, blockPos, blockState, direction, accumulatedHeight, checkValves);
        }
        if (blockState.getBlock() instanceof PipeCornerBlock) {
            return AbstractPipeBlockEntity.getPipeCornerEnd(level, blockPos, blockState, direction, accumulatedHeight, checkValves);
        }
        if (blockState.getBlock() instanceof PumpBlock) {
            Direction pumpOutputDir = blockState.getValue(PumpBlock.ORIENTATION).getDirection();
            if (direction == pumpOutputDir && level.getBlockEntity(blockPos) instanceof PumpBlockEntity pumpBe && pumpBe.canPump()) {
                return AbstractPipeBlockEntity.getPumpPipeEnd(level, blockPos, direction, accumulatedHeight, checkValves);
            }
            return null;
        }
        return null;
    }

    public static @Nullable PipeEnd getPipeEnd(Level level, BlockPos blockPos, Direction direction) {
        return AbstractPipeBlockEntity.getPipeEnd(level, blockPos, direction, 0);
    }

    public static @Nullable PipeEnd getPipeStraightEnd(
        Level level, BlockPos blockPos, BlockState blockState, Direction direction, int accumulatedHeight, boolean checkValves
    ) {
        Direction.Axis axis = blockState.getValue(PipeStraightBlock.AXIS);
        if (!direction.getAxis().equals(axis)) return null;
        Direction startDir = PipeBlock.getDirectionFromAxis(axis, Direction.AxisDirection.NEGATIVE);
        boolean hasNext;
        if (direction.equals(startDir)) hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_END);
        else hasNext = !blockState.getValue(PipeStraightBlock.HAS_END_START);
        Direction targetDir = direction.getOpposite();
        if (checkValves && !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, blockPos, targetDir, targetDir)) return null;
        if (!hasNext) {
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, direction, accumulatedHeight, checkValves);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return AbstractPipeBlockEntity.getPipeEnd(level, blockPos.relative(targetDir), direction, accumulatedHeight, checkValves);
    }

    public static @Nullable PipeEnd getPipeCornerEnd(
        Level level, BlockPos blockPos, BlockState blockState, Direction direction, int accumulatedHeight, boolean checkValves
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
        if (checkValves && !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, blockPos, targetDir, targetDir)) return null;
        if (!hasNext) {
            BlockPos neighborPos = blockPos.relative(targetDir);
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                return AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, targetDir.getOpposite(), accumulatedHeight, checkValves);
            }
            return new PipeEnd(blockPos, targetDir, accumulatedHeight);
        }
        return AbstractPipeBlockEntity.getPipeEnd(
            level,
            blockPos.relative(targetDir),
            targetDir.getOpposite(),
            accumulatedHeight,
            checkValves
        );
    }

    private static @Nullable PipeEnd getPumpPipeEnd(
        Level level, BlockPos pumpPos, Direction direction, int accumulatedHeight, boolean checkValves
    ) {
        BlockPos nextPos = pumpPos.relative(direction.getOpposite());
        if (!level.isLoaded(nextPos)) return null;
        BlockState nextState = level.getBlockState(nextPos);
        if (nextState.getBlock() instanceof PipeNodeBlock
            || nextState.getBlock() instanceof PipeStraightBlock
            || nextState.getBlock() instanceof PipeCornerBlock
            || nextState.getBlock() instanceof PumpBlock) {
            return AbstractPipeBlockEntity.getPipeEnd(
                level,
                nextPos,
                direction,
                accumulatedHeight + PumpBlockEntity.PUMP_HEADLIFT,
                checkValves
            );
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
        if (!AbstractPipeBlockEntity.canFlowThroughCheckValve(level, sourceCurPos, sourceCurDirection, sourceCurDirection.getOpposite())
            || !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, targetCurPos, targetCurDirection, targetCurDirection)
            || !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, sourcePos, sourceDirection, sourceDirection)
            || !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, targetPos, targetDirection, targetDirection.getOpposite())) {
            return;
        }
        AbstractPipeBlockEntity.moveFluid(
            level,
            sourcePos,
            sourceDirection,
            targetPos,
            targetDirection,
            sourceEffectiveY - targetEffectiveY
        );
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
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
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
        if (sourceCurDirection != null
            && !AbstractPipeBlockEntity.canFlowThroughCheckValve(
                level,
                sourceCurPos,
                sourceCurDirection,
                sourceCurDirection.getOpposite()
            )) {
            return;
        }
        if (targetCurDirection != null
            && !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, targetCurPos, targetCurDirection, targetCurDirection)) {
            return;
        }
        if (sourceSide != null && !AbstractPipeBlockEntity.canFlowThroughCheckValve(level, sourcePos, sourceSide, sourceSide)) {
            return;
        }
        if (targetSide != null && !AbstractPipeBlockEntity.canFlowThroughCheckValve(
            level,
            targetPos,
            targetSide,
            targetSide.getOpposite()
        )) {
            return;
        }
        AbstractPipeBlockEntity.moveFluid(
            level,
            sourcePos,
            sourceSide,
            targetPos,
            targetSide,
            sourceEffectiveHeight - targetEffectiveHeight
        );
    }

    /**
     * 使用 NeoForge 26.1 ResourceHandler<FluidResource> API 进行流体传输。
     * 替代旧的 IFluidHandler.drain()/fill() API。
     */
    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        @Nullable Direction sourceDirection,
        BlockPos targetPos,
        @Nullable Direction targetDirection,
        int heightDiff
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
                int inserted = target.insert(resourceToDrain, actualDrained, tx);
                if (inserted <= 0) continue;
                if (inserted < actualDrained) {
                    int returned = source.insert(resourceToDrain, actualDrained - inserted, tx);
                    if (returned < actualDrained - inserted) continue;
                }
                tx.commit();
            }
        }
        if (heightDiff != 0) {
            TriggerUtil.connectFluidContainers(level, sourcePos);
            TriggerUtil.connectFluidContainers(level, targetPos);
        }
    }

    public static void moveFluid(
        Level level,
        BlockPos sourcePos,
        @Nullable Direction sourceDirection,
        BlockPos targetPos,
        @Nullable Direction targetDirection
    ) {
        int heightDiff = sourcePos.getY() - targetPos.getY();
        AbstractPipeBlockEntity.moveFluid(level, sourcePos, sourceDirection, targetPos, targetDirection, heightDiff);
    }

    /**
     * 管道终点记录。
     */
    public record PipeEnd(BlockPos pos, Direction direction, int effectiveHeight) {}

    private record ValveData(Direction face, Direction flow) {
        private static final Codec<ValveData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AbstractPipeBlockEntity.DIRECTION_CODEC.fieldOf("Face").forGetter(ValveData::face),
            AbstractPipeBlockEntity.DIRECTION_CODEC.fieldOf("Flow").forGetter(ValveData::flow)
        ).apply(instance, ValveData::new));
    }
}
