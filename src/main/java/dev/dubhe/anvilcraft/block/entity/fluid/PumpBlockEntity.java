package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import dev.dubhe.anvilcraft.block.state.Orientation;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
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
public class PumpBlockEntity extends AbstractPipeBlockEntity implements IPowerConsumer {
    private static final int PUMP_POWER = 32;   // 32 kW 电力消耗
    public static final int PUMP_HEADLIFT = 10;    // 10 米扬程

    private @Nullable PowerGrid grid;
    private boolean working;

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
     * Per-tick：刷新电力/红石/过载状态，更新 heightBonus，并执行主动流体中转。
     * <ul>
     *   <li>红石信号 / 电网过载 → working=false（阻塞流体，停动画）</li>
     *   <li>正常启用 + 电网供电 → 实际泵送 + heightBonus</li>
     *   <li>启用但电网未供电 → 仅动画运行（活塞运动），不泵送</li>
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

        // working = 泵处于启用状态（控制动画和流体开关）
        boolean wasWorking = entity.working;
        entity.working = !powered && !overload;

        if (entity.working != wasWorking) {
            entity.setChanged();
            if (!level.isClientSide()) {
                entity.sendUpdate();
            }
        }
        Orientation orientation = updatedState.getValue(PumpBlock.ORIENTATION);
        Direction sourceDir = orientation.getDirection();
        BlockPos sourcePos = pos.relative(sourceDir);
        if (level.getBlockState(sourcePos).getBlock() instanceof PipeBlock || !entity.canPump()) {
            return;
        }
        Direction targetCurDir = sourceDir.getOpposite();
        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, sourcePos, targetCurDir);
        if (fluidHandler == null) return;
        PipeEnd pumpEnd = getPipeEnd(level, pos, sourceDir);
        BlockPos targetCurPos = pos;
        int effectiveHeight = 0;
        if (pumpEnd != null) {
            targetCurPos = pumpEnd.pos();
            targetCurDir = pumpEnd.direction();
            effectiveHeight = pumpEnd.effectiveHeight();
        }
        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
            level,
            pos,
            sourceDir,
            targetCurPos,
            targetCurDir,
            effectiveHeight
        );
    }
}
