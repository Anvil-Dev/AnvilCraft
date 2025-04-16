package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AdvancedRepeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.AdvancedRepeaterMenu;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
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
public class AdvancedRepeaterBlockEntity extends BlockEntity implements MenuProvider {
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

    public AdvancedRepeaterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ADVANCED_REPEATER.get(), pos, blockState);
    }

    private AdvancedRepeaterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AdvancedRepeaterBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new AdvancedRepeaterBlockEntity(type, pos, blockState);
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

    public AdvancedRepeaterBlockEntity readDataNbt(CompoundTag data) {
        this.startMode = data.getByte("StartMode");
        this.outputInvert = data.getBoolean("OutputMode");
        this.waitingTime = data.getInt("WaitingTime");
        this.signalDuration = data.getInt("SignalDuration");
        return this;
    }

    protected void tickTime() {
        switch (this.mode) {
            case WAITING -> {
                if (this.waitingTimeRemaining > 0 && !this.isDeadlock) {
                    this.waitingTimeRemaining--;
                    this.setChanged();
                }
                if (this.waitingTimeRemaining <= 0) {
                    this.startOutputting();
                    this.setChanged();
                }
            }
            case OUTPUTTING -> {
                if (this.signalDurationRemaining > 0 && !this.isDeadlock) {
                    this.signalDurationRemaining--;
                    this.setChanged();
                }
                if (this.signalDurationRemaining <= 0) {
                    this.checkOnSignalEnd();
                    this.setChanged();
                }
            }
        }
    }

    protected void checkIsDeadlock() {
        this.isDeadlock = switch (this.startMode) {
            case 0 -> this.mode == Mode.OUTPUTTING && this.isInputtingSignal;
            case 1 -> false;
            case 2 -> {
                if (this.isInputtingSignal && !this.isDeadlock) {
                    this.mode = Mode.DEFAULT;
                    this.signalDurationRemaining = 0;
                    this.setChanged();
                    yield true;
                } else if (this.isDeadlock && !this.isInputtingSignal) {
                    this.startWaiting();
                    this.setChanged();
                    yield false;
                } else {
                    yield false;
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + this.startMode);
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AdvancedRepeaterBlockEntity repeaterEntity) {
        repeaterEntity.tickTime();
        repeaterEntity.checkIsDeadlock();

        level.setBlock(
            pos,
            state.setValue(AdvancedRepeaterBlock.POWERED, repeaterEntity.outputInvert != repeaterEntity.isOutputting()),
            3
        );
    }

    protected void startWaiting() {
        this.mode = Mode.WAITING;
        this.waitingTimeRemaining = this.waitingTime;
    }

    protected void startOutputting() {
        this.mode = Mode.OUTPUTTING;
        this.signalDurationRemaining = this.signalDuration;
    }

    protected void checkOnSignalEnd() {
        this.mode = Mode.DEFAULT;

        if (this.startMode == 2) {
            this.startWaiting();
        }
    }

    public void start() {
        if (!this.isProcessing()) {
            this.startWaiting();
        }
    }

    public int getOutputSignal() {
        if (this.isOutputting()) {
            if (this.outputInvert) {
                return 0;
            } else {
                return 15;
            }
        } else {
            if (this.outputInvert) {
                return 15;
            } else {
                return 0;
            }
        }
    }

    public void setStartMode(int mode) {
        this.startMode = (byte) Math.clamp(mode, 0, 2);
        if (this.startMode == 2 && this.mode == Mode.DEFAULT) {
            this.startWaiting();
        }
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
        return this.mode == Mode.OUTPUTTING;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.advanced_repeater");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        if (player.level().getBlockEntity(getBlockPos()) instanceof AdvancedRepeaterBlockEntity blockEntity)
            return new AdvancedRepeaterMenu(ModMenuTypes.ADVANCED_REPEATER.get(), containerId, inventory, blockEntity);
        return null;
    }

    public static boolean canStart(@Nullable BlockEntity blockEntity, boolean nowInputting) {
        return blockEntity instanceof AdvancedRepeaterBlockEntity repeater
               && ((repeater.getStartMode() == 0 && !repeater.isInputtingSignal() && nowInputting)
                   || (repeater.getStartMode() == 1 && repeater.isInputtingSignal() && !nowInputting)
                   || (repeater.getStartMode() == 2 && (repeater.isDeadlock || repeater.mode == Mode.DEFAULT)));
    }

    public enum Mode {
        DEFAULT, WAITING, OUTPUTTING
    }
}
