package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.block.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AdvancedComparatorMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

@Getter
@Setter
public class AdvancedComparatorBlockEntity extends BlockEntity implements MenuProvider, IDiskCloneable {
    protected Mode compareMode = Mode.HYSTERESIS;
    private State state = State.OUTPUT_LOW;
    protected boolean outputInvert = false;
    protected boolean redstoneControl = false;
    protected int highLimit = 10;
    protected int lowLimit = 5;
    protected int inputtingSignal;

    public AdvancedComparatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ADVANCED_COMPARATOR.get(), pos, blockState);
    }

    private AdvancedComparatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AdvancedComparatorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new AdvancedComparatorBlockEntity(type, pos, blockState);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag data = this.constructDataNbt();
        data.putInt("InputSignal", this.inputtingSignal);
        return data;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag data = this.constructDataNbt();
        data.putInt("InputSignal", this.inputtingSignal);
        output.store("ExtraData", CompoundTag.CODEC, data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag data = input.read("ExtraData", CompoundTag.CODEC).orElse(new CompoundTag());
        this.readDataNbt(data);
        if ((this.compareMode == Mode.HYSTERESIS && this.inputtingSignal >= this.highLimit)
            || (this.compareMode == Mode.WINDOW && this.inputtingSignal <= this.highLimit)) {
            this.state = State.OUTPUT_HIGH;
        } else this.state = State.OUTPUT_LOW;
        Optional.ofNullable(this.getLevel())
            .ifPresent(level1 ->
                level1.scheduleTick(this.getBlockPos(), ModBlocks.ADVANCED_COMPARATOR.get(), 1));
    }

    public CompoundTag constructDataNbt() {
        CompoundTag data = new CompoundTag();
        data.putByte("CompareMode", this.compareMode.index());
        data.putBoolean("OutputMode", this.outputInvert);
        data.putBoolean("RedstoneControl", this.redstoneControl);
        data.putInt("HighLimit", this.highLimit);
        data.putInt("LowLimit", this.lowLimit);
        return data;
    }

    public AdvancedComparatorBlockEntity readDataNbt(CompoundTag data) {
        this.compareMode = Mode.fromIndex(data.getByteOr("CompareMode", (byte) 0));
        this.outputInvert = data.getBooleanOr("OutputMode", false);
        this.redstoneControl = data.getBooleanOr("RedstoneControl", false);
        this.highLimit = data.getIntOr("HighLimit", 0);
        this.lowLimit = data.getIntOr("LowLimit", 0);
        this.inputtingSignal = data.getIntOr("InputSignal", 0);
        return this;
    }

    @Override
    public void storeDiskData(ValueOutput output) {
        output.store("Data", CompoundTag.CODEC, this.constructDataNbt());
    }

    @Override
    public void applyDiskData(ValueInput input) {
        this.readDataNbt(input.read("Data", CompoundTag.CODEC).orElse(new CompoundTag()));
        if (this.getLevel() == null) return;
        Util.castSafely(this.getBlockState().getBlock(), AdvancedComparatorBlock.class)
            .ifPresent(block -> block.update(this.getLevel(), this.getBlockPos(), this.getBlockState()));
    }

    public boolean isOutputting() {
        return this.state == AdvancedComparatorBlockEntity.State.OUTPUT_HIGH != this.outputInvert;
    }

    public void updateInputtingSignal(Level level, BlockPos pos, BlockState state) {
        int signal = AdvancedComparatorBlock.getInputSignal(level, pos, state);
        this.inputtingSignal = Mth.clamp(signal, 0, 15);
        if (this.isRedstoneControl()) {
            this.highLimit = AdvancedComparatorBlock.getAlternateSignal(level, pos, state, true);
            this.lowLimit = AdvancedComparatorBlock.getAlternateSignal(level, pos, state, false);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level == null) return;
        this.updateInputtingSignal(this.level, this.getBlockPos(), this.getBlockState());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.advanced_comparator");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        if (player.level().getBlockEntity(this.getBlockPos()) instanceof AdvancedComparatorBlockEntity blockEntity) {
            return new AdvancedComparatorMenu(ModMenuTypes.ADVANCED_COMPARATOR.get(), containerId, inventory, blockEntity);
        }
        return null;
    }

    public CompoundTag exportMoveData() {
        return this.constructDataNbt();
    }

    public void applyMoveData(Level level, BlockPos pos, BlockState state, CompoundTag nbt) {
        this.readDataNbt(nbt);
        ((AdvancedComparatorBlock) state.getBlock()).update(level, pos, state);
        this.setChanged();
    }

    public enum State {
        OUTPUT_LOW, OUTPUT_HIGH
    }

    public enum Mode implements StringRepresentable {
        HYSTERESIS, WINDOW;

        public byte index() {
            return (byte) this.ordinal();
        }

        public static Mode fromIndex(int index) {
            return values()[index];
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase();
        }
    }
}
