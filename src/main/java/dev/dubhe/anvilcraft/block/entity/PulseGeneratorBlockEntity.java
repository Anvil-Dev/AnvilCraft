package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.block.PulseGeneratorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.PulseGeneratorMenu;
import dev.dubhe.anvilcraft.util.Util;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Getter
@Setter
public class PulseGeneratorBlockEntity extends BlockEntity implements MenuProvider, IDiskCloneable {
    protected Mode startMode = Mode.RISING_EDGE;
    protected boolean outputInvert = false;
    protected int waitingTime = 2;
    protected int signalDuration = 2;
    protected State state = State.DEFAULT;

    protected boolean isInputtingSignal = false;
    protected boolean isLocked = false;

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
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag data = this.constructDataNbt();
        BlockItem.setBlockEntityData(stack, this.getType(), data);
        stack.applyComponents(this.collectComponents());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag data = this.constructDataNbt();
        data.putByte("State", this.state.index());
        tag.put("ExtraData", data);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        CompoundTag data = tag.getCompound("ExtraData");
        this.readDataNbt(data);
        // TODO: 删除if-else和else块内的代码
        if (data.contains("State")) {
            this.state = State.fromIndex(data.getByte("State"));
        } else if (data.contains("RemainingWaitingTime") && data.contains("RemainingSignalDuration")) {
            int waitingTimeRemaining = data.getInt("RemainingWaitingTime");
            int signalDurationRemaining = data.getInt("RemainingSignalDuration");
            if (waitingTimeRemaining != 0) {
                this.state = State.WAITING;
                Optional.ofNullable(this.getLevel())
                    .ifPresent(level -> level.scheduleTick(
                        this.getBlockPos(), ModBlocks.PULSE_GENERATOR.get(), waitingTimeRemaining));
            } else if (signalDurationRemaining != 0) {
                this.state = State.OUTPUTTING;
                Optional.ofNullable(this.getLevel())
                    .ifPresent(level -> level.scheduleTick(
                        this.getBlockPos(), ModBlocks.PULSE_GENERATOR.get(), signalDurationRemaining));
            } else {
                this.state = State.DEFAULT;
            }
        }
        if (this.getLevel() == null) return;
        Util.castSafely(this.getBlockState().getBlock(), PulseGeneratorBlock.class)
            .ifPresent(block -> block.update(this.getLevel(), this.getBlockPos(), this::getBlockState));
    }

    public CompoundTag constructDataNbt() {
        CompoundTag data = new CompoundTag();
        data.putByte("StartMode", this.startMode.index());
        data.putBoolean("OutputMode", this.outputInvert);
        data.putBoolean("Inputting", this.isInputtingSignal);
        data.putInt("WaitingTime", this.waitingTime);
        data.putInt("SignalDuration", this.signalDuration);
        return data;
    }

    public PulseGeneratorBlockEntity readDataNbt(CompoundTag data) {
        this.startMode = Mode.fromIndex(data.getByte("StartMode"));
        this.outputInvert = data.getBoolean("OutputMode");
        this.isInputtingSignal = data.getBoolean("Inputting");
        this.waitingTime = data.getInt("WaitingTime");
        this.signalDuration = data.getInt("SignalDuration");
        return this;
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Data", this.constructDataNbt());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        this.readDataNbt(data.getCompound("Data"));
    }

    @ApiStatus.Internal
    public void setState(State state) {
        this.state = state;
    }

    public void setStartMode(int mode) {
        this.startMode = Mode.fromIndex(mode % 3);
        if (this.startMode != Mode.LOOP) {
            this.isLocked = false;
        } else if (!this.isInputtingSignal && this.level != null) {
            Util.castSafely(this.getBlockState().getBlock(), PulseGeneratorBlock.class)
                .ifPresent(block -> block.update(this.level, this.getBlockPos(), this::getBlockState));
        }
        this.setChanged();
    }

    public void setOutputMode(boolean outputInvert) {
        this.outputInvert = outputInvert;
        if (this.level != null) {
            Util.castSafely(this.getBlockState().getBlock(), PulseGeneratorBlock.class)
                .ifPresent(block -> block.update(this.level, this.getBlockPos(), this::getBlockState));
        }
        this.setChanged();
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = Math.clamp(waitingTime, 0, 24000);
        if (this.waitingTime == 0 && this.signalDuration == 0) {
            this.signalDuration = 1;
        }
        this.setChanged();
    }

    public void setSignalDuration(int signalDuration) {
        this.signalDuration = Math.clamp(signalDuration, 0, 24000);
        if (this.signalDuration == 0 && this.waitingTime == 0) {
            this.waitingTime = 1;
        }
        this.setChanged();
    }

    public boolean isProcessing() {
        return this.state != State.DEFAULT;
    }

    public boolean isOutputting() {
        if (this.isLocked) return this.outputInvert;
        return (this.state == State.OUTPUTTING) != this.outputInvert;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.pulse_generator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        if (player.level().getBlockEntity(getBlockPos()) instanceof PulseGeneratorBlockEntity blockEntity) {
            return new PulseGeneratorMenu(ModMenuTypes.PULSE_GENERATOR.get(), containerId, inventory, blockEntity);
        }
        return null;
    }

    public CompoundTag exportMoveData() {
        CompoundTag data = new CompoundTag();
        data.put("Data", this.constructDataNbt());
        data.putBoolean("Inputting", this.isInputtingSignal);
        data.putByte("State", this.state.index());
        return data;
    }

    public void applyMoveData(Level level, BlockPos pos, BlockState state, CompoundTag move) {
        this.readDataNbt(move.getCompound("Data"));
        this.isInputtingSignal = move.getBoolean("Inputting");
        this.state = State.fromIndex(move.getByte("State"));
        switch (this.state) {
            case WAITING -> level.scheduleTick(pos, state.getBlock(), this.getWaitingTime());
            case OUTPUTTING -> level.scheduleTick(pos, state.getBlock(), this.getSignalDuration());
            default -> {
            }
        }
        level.setBlock(pos, state.setValue(PulseGeneratorBlock.POWERED, this.isOutputting()), 3);
        this.isLocked = false;

        Util.<PulseGeneratorBlock>cast(this.getBlockState().getBlock()).update(level, pos, () -> state);

        this.setChanged();
    }

    public enum State {
        DEFAULT, WAITING, OUTPUTTING;

        public byte index() {
            return (byte) this.ordinal();
        }

        public static State fromIndex(int index) {
            return State.values()[index];
        }
    }

    public enum Mode {
        RISING_EDGE, FALLING_EDGE, LOOP;

        public byte index() {
            return (byte) this.ordinal();
        }

        public static Mode fromIndex(int index) {
            return Mode.values()[index];
        }
    }
}
