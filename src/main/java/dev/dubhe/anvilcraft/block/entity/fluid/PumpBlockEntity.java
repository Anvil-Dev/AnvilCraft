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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jetbrains.annotations.Nullable;

/**
 * 泵的 BlockEntity。消费 32kW 电力，提供输入端 +10 / 输出端 -10 的等效高度偏移。
 */
@Getter
@Setter
public class PumpBlockEntity extends AbstractPipeBlockEntity implements IPowerConsumer {
    private static final int PUMP_POWER = 32;
    public static final int PUMP_HEADLIFT = 10;

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

    public boolean canPump() {
        return this.working && this.grid != null && this.grid.isWorking();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Working", this.working);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.working = input.getBooleanOr("Working", false);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Working", this.working);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PumpBlockEntity entity) {
        if (level.isClientSide()) return;
        entity.flushState(level, pos);
        BlockState updatedState = level.getBlockState(pos);
        boolean powered = updatedState.getValue(PumpBlock.POWERED);
        boolean overload = updatedState.getValue(PumpBlock.OVERLOAD);
        boolean wasWorking = entity.working;
        entity.working = !powered && !overload;
        if (entity.working != wasWorking) {
            entity.setChanged();
            if (!level.isClientSide()) entity.sendUpdate();
        }
        Orientation orientation = updatedState.getValue(PumpBlock.ORIENTATION);
        Direction sourceDir = orientation.getDirection();
        BlockPos sourcePos = pos.relative(sourceDir);
        if (level.getBlockState(sourcePos).getBlock() instanceof PipeBlock || !entity.canPump()) return;
        Direction targetCurDir = sourceDir.getOpposite();
        ResourceHandler<FluidResource> fluidHandler = level.getCapability(Capabilities.Fluid.BLOCK, sourcePos, targetCurDir);
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
            level, pos, sourceDir, targetCurPos, targetCurDir, effectiveHeight);
    }
}
