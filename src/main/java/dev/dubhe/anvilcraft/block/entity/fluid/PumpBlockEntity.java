package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * 泵的 BlockEntity。消费 32kW 电力，提供输入端 +10 / 输出端 -10 的等效高度偏移。
 *
 * <p>工作状态判定：
 * <ul>
 *   <li>有红石信号 → 关闭（heightBonus=0，阻塞流体）</li>
 *   <li>电网过载 → 关闭（OVERLOAD=true，阻塞流体）</li>
 *   <li>正常供电 → 工作（输入端 +10，输出端 -10）</li>
 * </ul>
 *
 * <p>方向映射：{@link dev.dubhe.anvilcraft.block.state.Orientation#getDirection()} 返回输出方向。
 * 输入端为该方向的反方向。泵的输出端高度降低，输入端高度抬升。
 */
@Getter
@Setter
public class PumpBlockEntity extends AbstractPipeBlockEntity implements IPowerConsumer, IFluidHandlerHolder {

    private static final int PUMP_POWER = 32;   // 32 kW 电力消耗
    public static final int PUMP_HEADLIFT = 10;    // 10 米扬程

    private @Nullable PowerGrid grid;
    private boolean working;

    /**
     * 上 tick 实际传输的流体量（mB），用于客户端活塞动画速度
     */
    @Getter
    private int lastTransferAmount;

    /**
     * 重入防护：防止 IFluidHandler relay 时无限递归
     */
    private boolean transferring;

    public PumpBlockEntity(BlockEntityType<? extends PumpBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static PumpBlockEntity create(BlockEntityType<PumpBlockEntity> type, BlockPos pos, BlockState state) {
        return new PumpBlockEntity(type, pos, state);
    }

    @Override
    public int getInputPower() {
        return PUMP_POWER;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    // ---- IFluidHandlerHolder ----

    @Override
    public IFluidHandler getFluidHandler() {
        return new PumpFluidHandler();
    }

    /**
     * 获取相邻方块在指定侧面的 IFluidHandler。
     *
     * @param side 泵朝向该邻居的方向
     */
    @Nullable
    private IFluidHandler getNeighborFluidHandler(Direction side) {
        if (level == null) return null;
        BlockPos neighborPos = getBlockPos().relative(side);
        return level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, side.getOpposite());
    }

    /**
     * 泵是否能实际泵送流体（启用 + 电网有电）
     */
    public boolean canPump() {
        return working && grid != null && grid.isWorking();
    }

    /**
     * 泵的无缓存流体处理器。
     * <ul>
     *   <li>能泵送时：drain 从输入端邻居抽取，fill 向输出端邻居注入</li>
     *   <li>关闭/过载/无电：容量为 0，阻塞所有流体</li>
     * </ul>
     */
    private class PumpFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            if (!canPump()) return 0;
            return PUMP_HEADLIFT * 50; // 500 mB/tick = 10m × 50 mB/tick/m
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return canPump();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canPump() || transferring) return 0;
            BlockState state = getBlockState();
            if (!(state.getBlock() instanceof PumpBlock)) return 0;
            Direction outputDir = state.getValue(PumpBlock.ORIENTATION).getDirection();
            IFluidHandler output = getNeighborFluidHandler(outputDir);
            if (output == null) return 0;

            transferring = true;
            try {
                return output.fill(resource, action);
            } finally {
                transferring = false;
            }
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!canPump() || transferring) return FluidStack.EMPTY;
            BlockState state = getBlockState();
            if (!(state.getBlock() instanceof PumpBlock)) return FluidStack.EMPTY;
            Direction inputDir = state.getValue(PumpBlock.ORIENTATION).getDirection().getOpposite();
            IFluidHandler input = getNeighborFluidHandler(inputDir);
            if (input == null) return FluidStack.EMPTY;

            transferring = true;
            try {
                return input.drain(resource, action);
            } finally {
                transferring = false;
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!canPump() || transferring) return FluidStack.EMPTY;
            BlockState state = getBlockState();
            if (!(state.getBlock() instanceof PumpBlock)) return FluidStack.EMPTY;
            Direction inputDir = state.getValue(PumpBlock.ORIENTATION).getDirection().getOpposite();
            IFluidHandler input = getNeighborFluidHandler(inputDir);
            if (input == null) return FluidStack.EMPTY;

            transferring = true;
            try {
                return input.drain(maxDrain, action);
            } finally {
                transferring = false;
            }
        }
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Working", working);
        tag.putInt("LastTransferAmount", lastTransferAmount);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        working = tag.getBoolean("Working");
        lastTransferAmount = tag.getInt("LastTransferAmount");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Working", working);
        tag.putInt("LastTransferAmount", lastTransferAmount);
        return tag;
    }

    // ---- Tick ----

    /**
     * Per-tick：刷新电力/红石/过载状态，更新 heightBonus，并执行主动流体中转。
     * <ul>
     *   <li>红石信号 / 电网过载 → working=false（阻塞流体，停动画）</li>
     *   <li>正常启用 + 电网供电 → 实际泵送 + heightBonus</li>
     *   <li>启用但电网未供电 → 仅动画运行（活塞运动），不泵送</li>
     * </ul>
     */
    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity entity) {
        if (level.isClientSide()) {
            boolean powered = state.getValue(PumpBlock.POWERED);
            boolean overload = state.getValue(PumpBlock.OVERLOAD);
            entity.working = !powered && !overload;
            return;
        }

        // 刷新电网过载状态到 blockstate
        entity.flushState(level, pos);
        // flushState 通过 setBlockAndUpdate 修改了 blockstate，需重读
        BlockState updatedState = level.getBlockState(pos);

        boolean powered = updatedState.getValue(PumpBlock.POWERED);
        boolean overload = updatedState.getValue(PumpBlock.OVERLOAD);

        // working = 泵处于启用状态（控制动画和流体开关）
        boolean wasWorking = entity.working;
        entity.working = !powered && !overload;

        if (entity.working != wasWorking) {
            entity.setChanged();
            if (!level.isClientSide()) entity.sendUpdate();
        }

        // 电网是否能提供足够功率
        boolean gridPowered = entity.grid != null && entity.grid.isWorking();

        // 实际泵送需要启用状态 + 电网供电
        boolean canPump = entity.working && gridPowered;

        // 主动流体中转：从输入端抽取流体，注入输出端
        int transferred = 0;
        if (canPump && !entity.transferring) {
            Orientation orientation = updatedState.getValue(PumpBlock.ORIENTATION);
            Direction outputDir = orientation.getDirection();
            Direction inputDir = outputDir.getOpposite();

            IFluidHandler inputHandler = entity.getNeighborFluidHandler(inputDir);
            IFluidHandler outputHandler = entity.getNeighborFluidHandler(outputDir);

            if (inputHandler != null && outputHandler != null) {
                int maxTransfer = PUMP_HEADLIFT * 50; // 500 mB/tick
                entity.transferring = true;
                try {
                    FluidStack drained = inputHandler.drain(maxTransfer, IFluidHandler.FluidAction.SIMULATE);
                    if (!drained.isEmpty()) {
                        int filled = outputHandler.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            FluidStack toMove = drained.copyWithAmount(filled);
                            inputHandler.drain(toMove, IFluidHandler.FluidAction.EXECUTE);
                            outputHandler.fill(toMove, IFluidHandler.FluidAction.EXECUTE);
                            transferred = filled;
                        }
                    }
                } finally {
                    entity.transferring = false;
                }
            }
        }

        // 同步传输量到客户端（用于活塞动画速度）
        if (entity.lastTransferAmount != transferred) {
            entity.lastTransferAmount = transferred;
            entity.setChanged();
            if (!level.isClientSide()) entity.sendUpdate();
        }
    }
}
