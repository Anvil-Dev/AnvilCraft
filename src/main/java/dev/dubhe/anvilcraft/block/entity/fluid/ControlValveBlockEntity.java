package dev.dubhe.anvilcraft.block.entity.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.block.fluid.ControlValveBlock;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

@Getter
public class ControlValveBlockEntity extends BlockEntity implements MenuProvider {
    public static final int FILTER_SLOT_COUNT = 1;
    public static final int MAX_RATE = 2000;

    private final NonNullList<FluidStack> filters = NonNullList.withSize(FILTER_SLOT_COUNT, FluidStack.EMPTY);
    private int maxRate = MAX_RATE;
    private Direction facing = Direction.NORTH;

    public ControlValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void setFacing(Direction facing) {
        this.facing = facing;
        this.setChanged();
    }

    public void setMaxRate(int maxRate) {
        int clamped = Mth.clamp(maxRate, 0, MAX_RATE);
        if (this.maxRate == clamped) return;
        this.maxRate = clamped;
        this.setChanged();
        this.markNetworkDirty();
    }

    public boolean isLocked() {
        BlockState state = getBlockState();
        return state.hasProperty(ControlValveBlock.POWERED) && state.getValue(ControlValveBlock.POWERED);
    }

    public int getEffectiveMaxRate() {
        return this.isLocked() ? 0 : this.maxRate;
    }

    public void setFilter(int index, FluidStack fluid) {
        if (index < 0 || index >= this.filters.size()) return;
        FluidStack normalized = fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1);
        FluidStack old = this.filters.get(index);
        if (old.isEmpty() && normalized.isEmpty()) return;
        if (old.getAmount() == normalized.getAmount() && FluidStack.isSameFluidSameComponents(old, normalized)) return;
        this.filters.set(index, normalized);
        this.setChanged();
        this.markNetworkDirty();
    }

    public FluidStack getFilter(int index) {
        return (index < 0 || index >= this.filters.size()) ? FluidStack.EMPTY : this.filters.get(index);
    }

    /**
     * Check if a FluidStack is allowed through this valve.
     */
    public boolean allows(FluidStack fluid) {
        boolean anySet = false;
        for (FluidStack allowed : this.filters) {
            if (allowed.isEmpty()) continue;
            anySet = true;
            if (FluidStack.isSameFluidSameComponents(allowed, fluid)) return true;
        }
        return !anySet;
    }

    /**
     * Check if a FluidResource is allowed through this valve.
     */
    public boolean allows(FluidResource resource) {
        if (resource.isEmpty()) return false;
        boolean anySet = false;
        for (FluidStack allowed : this.filters) {
            if (allowed.isEmpty()) continue;
            anySet = true;
            if (resource.matches(allowed)) return true;
        }
        return !anySet;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("MaxRate", this.maxRate);
        output.putInt("Facing", this.facing.get3DDataValue());
        this.writeFilters(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.maxRate = Mth.clamp(input.getIntOr("MaxRate", MAX_RATE), 0, MAX_RATE);
        this.facing = Direction.from3DDataValue(input.getIntOr("Facing", Direction.NORTH.get3DDataValue()));
        this.readFilters(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        output.putInt("MaxRate", this.maxRate);
        output.putInt("Facing", this.facing.get3DDataValue());
        this.writeFilters(output);
        return output.buildResult();
    }

    private void writeFilters(ValueOutput output) {
        ValueOutput.TypedOutputList<FilterData> list = output.list("Filters", FilterData.CODEC);
        for (int i = 0; i < this.filters.size(); i++) {
            FluidStack fluid = this.filters.get(i);
            if (fluid.isEmpty()) continue;
            list.add(new FilterData(i, fluid));
        }
    }

    private void readFilters(ValueInput input) {
        this.filters.replaceAll(_ -> FluidStack.EMPTY);
        for (FilterData filter : input.listOrEmpty("Filters", FilterData.CODEC)) {
            if (filter.slot() < 0 || filter.slot() >= this.filters.size()) continue;
            FluidStack fluid = filter.fluid();
            this.filters.set(filter.slot(), fluid.isEmpty() ? FluidStack.EMPTY : fluid.copyWithAmount(1));
        }
    }

    private void markNetworkDirty() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.markDirty(this.level);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sendUpdate() {
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.control_valve");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ControlValveMenu(ModMenuTypes.CONTROL_VALVE.get(), containerId, inventory, this);
    }

    private record FilterData(int slot, FluidStack fluid) {
        private static final Codec<FilterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("Slot").forGetter(FilterData::slot),
            FluidStack.OPTIONAL_CODEC.fieldOf("Fluid").forGetter(FilterData::fluid)
        ).apply(instance, FilterData::new));
    }
}
