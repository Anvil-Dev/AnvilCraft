package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * 泵的 BlockEntity。消费 32kW 电力，为流体网络提供输出侧 +10 / 输入侧 -10 的等效高度势场。
 *
 * <p>重构后泵<b>不再自行搬运流体</b>：它只作为网络中的一条"有向势能边"，实际流体
 * 分配由 {@link dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork} 统一执行。
 * 本 tick 仅刷新电力/红石/过载状态并据此决定 {@link #canPump()}。
 *
 * <p>工作状态判定：
 * <ul>
 *   <li>有红石信号 → 关闭（阻断网络连接）</li>
 *   <li>电网过载 → 关闭（阻断网络连接）</li>
 *   <li>正常供电 → 工作（提供势场）</li>
 * </ul>
 *
 * <p>方向映射：{@link dev.dubhe.anvilcraft.block.state.Orientation#getDirection()} 返回输出方向，
 * 该侧等效高度 +10；反向（输入侧）-10。
 */
@Getter
@Setter
public class PumpBlockEntity extends AbstractPipeBlockEntity implements IPowerConsumer {
    private static final int PUMP_POWER = 32;   // 32 kW 电力消耗
    public static final int PUMP_HEADLIFT = 10;    // 10 米扬程（单侧势场偏移）

    private @Nullable PowerGrid grid;
    private boolean working;
    /** 上一 tick 的 canPump() 结果，用于检测供电变化以令网络缓存失效 */
    private boolean lastCanPump;

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

    /**
     * 泵是否能实际泵送流体（启用 + 电网有电）
     */
    public boolean canPump() {
        return this.working && this.grid != null && this.grid.isWorking();
    }

    // ---- NBT ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Working", working);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        working = tag.getBoolean("Working");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Working", working);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- Tick ----

    /**
     * Per-tick：刷新电力/红石/过载状态，据此更新 {@link #working}（决定泵是否为网络提供连接与势场）。
     * 不再自行搬运流体——流体分配由 {@link dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork} 统一执行。
     * <ul>
     *   <li>红石信号 / 电网过载 → working=false（阻断连接，停动画）</li>
     *   <li>正常启用 + 电网供电 → working=true（提供势场）</li>
     * </ul>
     */
    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity entity) {
        if (level.isClientSide()) {
            return;
        }

        // 刷新电网过载状态到 blockstate
        entity.flushState(level, pos);
        // flushState 通过 setBlockAndUpdate 修改了 blockstate，需重读
        BlockState updatedState = level.getBlockState(pos);

        boolean powered = updatedState.getValue(PumpBlock.POWERED);
        boolean overload = updatedState.getValue(PumpBlock.OVERLOAD);

        // working = 泵处于启用状态（控制动画和势场开关）
        boolean wasWorking = entity.working;
        entity.working = !powered && !overload;

        if (entity.working != wasWorking) {
            entity.setChanged();
            entity.sendUpdate();
        }
        // canPump() 还取决于电网供电(grid.isWorking())，它可能在 working 不变时改变，一旦 canPump 结果变化就令网络缓存失效重扫，避免断电后泵仍继续抽送。
        boolean canPumpNow = entity.canPump();
        if (canPumpNow != entity.lastCanPump) {
            entity.lastCanPump = canPumpNow;
            FluidNetworkManager.INSTANCE.markDirty(level);
        }
    }
}
