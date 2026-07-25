package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

final class SingleFluidTankHandler implements ResourceHandler<FluidResource>, ValueIOSerializable {
    private final int baseCapacity;
    private final int infinityThreshold;
    private final Runnable changeListener;
    private final TankJournal snapshotJournal = new TankJournal();
    private FluidStack fluid = FluidStack.EMPTY;
    private boolean enhanced;
    private boolean infinite;

    SingleFluidTankHandler(int baseCapacity, int infinityThreshold, Runnable changeListener) {
        this.baseCapacity = baseCapacity;
        this.infinityThreshold = infinityThreshold;
        this.changeListener = changeListener;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, 1);
        return FluidResource.of(this.fluid);
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, 1);
        return this.fluid.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, 1);
        return this.enhanced ? this.infinityThreshold : this.baseCapacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, 1);
        return !resource.isEmpty() && (this.fluid.isEmpty() || FluidResource.of(this.fluid).equals(resource));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, 1);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!this.isValid(index, resource)) return 0;
        if (this.infinite) return amount;

        int stored = this.fluid.getAmount();
        int capacity = this.enhanced ? this.infinityThreshold : this.baseCapacity;
        int space = capacity - stored;
        if (space <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);
        if (this.enhanced && amount >= space) {
            this.fluid = resource.toStack(this.infinityThreshold);
            this.infinite = true;
            return amount;
        }

        int inserted = Math.min(amount, space);
        this.fluid = resource.toStack(stored + inserted);
        return inserted;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, 1);
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        if (!FluidResource.of(this.fluid).equals(resource)) return 0;
        if (this.infinite) return amount;

        int extracted = Math.min(amount, this.fluid.getAmount());
        if (extracted <= 0) return 0;
        this.snapshotJournal.updateSnapshots(transaction);
        int remaining = this.fluid.getAmount() - extracted;
        this.fluid = remaining == 0 ? FluidStack.EMPTY : resource.toStack(remaining);
        return extracted;
    }

    void setEnhanced(boolean enhanced) {
        if (this.enhanced == enhanced) return;
        this.enhanced = enhanced;
        // 缩容时只调整容量上限，不截断已存流体，避免修改容量导致存量凭空减少
        this.infinite = enhanced && this.fluid.getAmount() >= this.infinityThreshold;
        this.changeListener.run();
    }

    boolean isInfinite() {
        return this.infinite;
    }

    boolean isEnhanced() {
        return this.enhanced;
    }

    FluidStack getFluid() {
        return this.fluid.copy();
    }

    int getFluidAmount() {
        return this.fluid.getAmount();
    }

    int getCapacity() {
        return this.enhanced ? this.infinityThreshold : this.baseCapacity;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, this.fluid);
        output.putBoolean("Enhanced", this.enhanced);
        output.putBoolean("Infinite", this.infinite);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.enhanced = input.getBooleanOr("Enhanced", false);
        this.fluid = input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        int capacity = this.enhanced ? this.infinityThreshold : this.baseCapacity;
        if (this.fluid.getAmount() > capacity) this.fluid.setAmount(capacity);
        this.infinite = this.enhanced
            && input.getBooleanOr("Infinite", false)
            && this.fluid.getAmount() == this.infinityThreshold;
        this.changeListener.run();
    }

    private record TankState(FluidStack fluid, boolean enhanced, boolean infinite) {
    }

    private class TankJournal extends SnapshotJournal<TankState> {
        @Override
        protected TankState createSnapshot() {
            return new TankState(
                SingleFluidTankHandler.this.fluid.copy(),
                SingleFluidTankHandler.this.enhanced,
                SingleFluidTankHandler.this.infinite
            );
        }

        @Override
        protected void revertToSnapshot(TankState snapshot) {
            SingleFluidTankHandler.this.fluid = snapshot.fluid().copy();
            SingleFluidTankHandler.this.enhanced = snapshot.enhanced();
            SingleFluidTankHandler.this.infinite = snapshot.infinite();
        }

        @Override
        protected void onRootCommit(TankState originalState) {
            SingleFluidTankHandler.this.changeListener.run();
        }
    }
}
