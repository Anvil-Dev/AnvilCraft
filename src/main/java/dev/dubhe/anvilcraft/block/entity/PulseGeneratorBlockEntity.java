package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@Setter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PulseGeneratorBlockEntity extends BlockEntity implements MenuProvider {
    protected byte startMode = 0;
    protected boolean outputInvert = false;
    protected int waitingTime = 2;
    protected int signalDuration = 2;

    protected boolean isInputtingSignal = false;
    protected boolean isDeadlock = false;

    @Setter(AccessLevel.NONE)
    private Mode mode = Mode.DEFAULT;
    @Setter(AccessLevel.NONE)
    private int waitingTimeRemaining;
    @Setter(AccessLevel.NONE)
    private int signalDurationRemaining;

    public PulseGeneratorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.PULSE_GENERATOR.get(), pos, blockState);
    }

    private PulseGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PulseGeneratorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new PulseGeneratorBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag data = this.constructDataNbt();
        data.putInt("RemainingWaitingTime", this.waitingTimeRemaining);
        data.putInt("RemainingSignalDuration", this.signalDurationRemaining);
        tag.put("ExtraData", data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag data = tag.getCompound("ExtraData");
        this.readDataNbt(data);
        this.waitingTimeRemaining = data.getInt("RemainingWaitingTime");
        this.signalDurationRemaining = data.getInt("RemainingSignalDuration");
        if (this.waitingTimeRemaining != 0) this.mode = Mode.WAITING;
        else if (this.signalDurationRemaining != 0) this.mode = Mode.OUTPUTTING;
        else this.mode = Mode.DEFAULT;
    }

    public CompoundTag constructDataNbt() {
        CompoundTag data = new CompoundTag();
        data.putByte("StartMode", this.startMode);
        data.putBoolean("OutputMode", this.outputInvert);
        data.putInt("WaitingTime", this.waitingTime);
        data.putInt("SignalDuration", this.signalDuration);
        return data;
    }

    public PulseGeneratorBlockEntity readDataNbt(CompoundTag data) {
        this.startMode = data.getByte("StartMode");
        this.outputInvert = data.getBoolean("OutputMode");
        this.waitingTime = data.getInt("WaitingTime");
        this.signalDuration = data.getInt("SignalDuration");
        return this;
    }

    protected void tickTime(Level level, BlockPos pos, BlockState state) {
        switch (this.mode) {
            case WAITING -> {
                if (this.waitingTimeRemaining > 0 && !this.isDeadlock) {
                    this.waitingTimeRemaining--;
                    this.setChanged();
                }
                if (this.waitingTimeRemaining <= 0) {
                    this.startOutputting(level, pos, state);
                    this.setChanged();
                }
            }
            case OUTPUTTING -> {
                if (this.signalDurationRemaining > 0 && !this.isDeadlock) {
                    this.signalDurationRemaining--;
                    this.setChanged();
                }
                if (this.signalDurationRemaining <= 0) {
                    this.checkOnSignalEnd(level, pos, state);
                    this.setChanged();
                }
            }
        }
    }

    protected void checkIsDeadlock(Level level, BlockPos pos, BlockState state) {
        this.isDeadlock = switch (this.startMode) {
            case 0, 1 -> false;
            case 2 -> {
                if (this.isInputtingSignal && !this.isDeadlock) {
                    this.mode = Mode.DEFAULT;
                    this.signalDurationRemaining = 0;
                    this.setChanged();
                    yield true;
                } else if (this.isDeadlock && !this.isInputtingSignal) {
                    this.startWaiting(level, pos, state);
                    this.setChanged();
                    yield false;
                } else {
                    yield false;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + this.startMode);
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseGeneratorBlockEntity generatorEntity) {
        if (level.isClientSide) return;
        generatorEntity.tickTime(level, pos, state);
        generatorEntity.checkIsDeadlock(level, pos, state);

        generatorEntity.updateBlockAndNeighbours(level, pos, state);
    }

    protected void updateBlockAndNeighbours(Level level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) instanceof PulseGeneratorBlockEntity generatorEntity) {
            Direction direction = state.getValue(PulseGeneratorBlock.FACING).getOpposite();
            BlockPos neighbourPos = pos.relative(direction);
            level.setBlockAndUpdate(pos, state.setValue(PulseGeneratorBlock.POWERED, generatorEntity.isOutputting()));
            level.neighborChanged(neighbourPos, state.getBlock(), pos);
            level.updateNeighborsAtExceptFromFacing(neighbourPos, state.getBlock(), direction);
        }
    }

    protected void startWaiting(Level level, BlockPos pos, BlockState state) {
        this.mode = Mode.WAITING;
        if (this.waitingTime != 0) {
            this.waitingTimeRemaining = this.waitingTime;
        } else {
            this.startOutputting(level, pos, state);
        }
    }

    protected void startOutputting(Level level, BlockPos pos, BlockState state) {
        this.mode = Mode.OUTPUTTING;
        this.signalDurationRemaining = this.signalDuration;
        this.updateBlockAndNeighbours(level, pos, state);
    }

    protected void checkOnSignalEnd(Level level, BlockPos pos, BlockState state) {
        this.mode = Mode.DEFAULT;
        this.updateBlockAndNeighbours(level, pos, state);

        if (this.startMode == 2) {
            this.startWaiting(level, pos, state);
        }
    }

    public void start(Level level, BlockPos pos, BlockState state) {
        if (!this.isProcessing()) {
            this.startWaiting(level, pos, state);
        }
    }

    public void setStartMode(int mode) {
        this.startMode = (byte) Math.clamp(mode, 0, 2);
        this.setChanged();
    }

    public void setSignalDuration(int signalDuration) {
        this.signalDuration = Math.clamp(signalDuration, 1, 24000);
        this.setChanged();
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = Math.clamp(waitingTime, 0, 24000);
        this.setChanged();
    }

    public boolean isProcessing() {
        return this.mode != Mode.DEFAULT;
    }

    public boolean isOutputting() {
        return this.mode == Mode.OUTPUTTING ^ this.outputInvert;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.pulse_generator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        if (player.level().getBlockEntity(getBlockPos()) instanceof PulseGeneratorBlockEntity blockEntity)
            return new PulseGeneratorMenu(ModMenuTypes.PULSE_GENERATOR.get(), containerId, inventory, blockEntity);
        return null;
    }

    public static boolean canStart(@Nullable BlockEntity blockEntity, boolean nowInputting) {
        return blockEntity instanceof PulseGeneratorBlockEntity repeater
               && ((repeater.getStartMode() == 0 && !repeater.isInputtingSignal() && nowInputting)
                   || (repeater.getStartMode() == 1 && repeater.isInputtingSignal() && !nowInputting)
                   || (repeater.getStartMode() == 2 && (repeater.isDeadlock || repeater.mode == Mode.DEFAULT)));
    }

    public enum Mode {
        DEFAULT, WAITING, OUTPUTTING
    }
}
