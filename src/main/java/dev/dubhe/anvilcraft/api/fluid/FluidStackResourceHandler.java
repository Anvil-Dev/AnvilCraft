package dev.dubhe.anvilcraft.api.fluid;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

public class FluidStackResourceHandler implements ResourceHandler<FluidResource>, ValueIOSerializable {
    @Getter
    private FluidStack stack = FluidStack.EMPTY;
    @Setter
    private int capacity;

    private final StackJournal snapshotJournal = new StackJournal();

    public FluidStackResourceHandler() {
        this(FluidType.BUCKET_VOLUME);
    }

    public FluidStackResourceHandler(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public FluidResource getResource(int index) {
        return FluidResource.of(this.stack);
    }

    @Override
    public long getAmountAsLong(int index) {
        return this.stack.getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        return this.capacity;
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        return this.stack.isEmpty() || resource.matches(this.stack);
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        int currentAmount = this.getAmountAsInt(index);

        if (currentAmount == 0 || this.isValid(index, resource)) {
            int inserted = Math.min(amount, this.getCapacityAsInt(index, resource) - currentAmount);

            if (inserted > 0) {
                this.snapshotJournal.updateSnapshots(transaction);
                this.stack = resource.toStack(currentAmount + inserted);
                return inserted;
            }
        }

        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        Objects.checkIndex(index, this.size());
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);

        if (this.isValid(index, resource)) {
            int currentAmount = this.getAmountAsInt(index);
            int extracted = Math.min(amount, currentAmount);

            if (extracted > 0) {
                this.snapshotJournal.updateSnapshots(transaction);
                this.stack = resource.toStack(currentAmount - extracted);
                return extracted;
            }
        }

        return 0;
    }

    public void set(FluidResource resource, int amount) {
        TransferPreconditions.checkNonNegative(amount);
        if (resource.isEmpty() && amount > 0) {
            throw new IllegalArgumentException("Resource is empty but the amount is positive: " + amount);
        }

        FluidStack original = this.stack;
        this.stack = resource.toStack(amount);
        this.onContentChanged(original);
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("Fluid", FluidStack.OPTIONAL_CODEC, this.stack);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.stack = input.read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        this.onContentChanged(this.stack);
    }

    public boolean isFull() {
        return this.stack.amount() == this.capacity;
    }

    public float getFill() {
        return (float) this.stack.amount() / this.capacity;
    }

    protected void onContentChanged(FluidStack original) {
    }

    private class StackJournal extends SnapshotJournal<FluidStack> {
        private StackJournal() {
        }

        @Override
        protected FluidStack createSnapshot() {
            return FluidStackResourceHandler.this.stack.copy();
        }

        @Override
        protected void revertToSnapshot(FluidStack snapshot) {
            FluidStackResourceHandler.this.stack = snapshot;
        }

        @Override
        protected void onRootCommit(FluidStack originalState) {
            FluidStackResourceHandler.this.onContentChanged(originalState);
        }
    }
}
