package dev.dubhe.anvilcraft.api.fluidtank;

import dev.dubhe.anvilcraft.mixin.accessor.StacksResourceHandlerAccessor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

@Getter
@Setter
public class InfinityFluidTank extends FluidStacksResourceHandler {
    private boolean infinity;

    public InfinityFluidTank(int capacity) {
        this(1, capacity, false);
    }

    public InfinityFluidTank(int capacity, boolean infinity) {
        this(1, capacity, infinity);
    }

    public InfinityFluidTank(int size, int capacity, boolean infinity) {
        super(size, capacity);
        this.infinity = infinity;
    }

    public InfinityFluidTank(NonNullList<FluidStack> stacks, int capacity, boolean infinity) {
        super(stacks, capacity);
        this.infinity = infinity;
    }

    @Override
    protected int getCapacity(int index, FluidResource resource) {
        if (this.isInfinity()) return Integer.MAX_VALUE;
        return super.getCapacity(index, resource);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        for (int i = 0; i < this.size(); i++) {
            FluidResource resource = this.getResource(i);
            if (this.infinity && !resource.isEmpty()) {
                this.stacks.set(i, this.getStackFrom(resource, capacity));
            }
        }
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        if (!this.infinity) return super.extract(resource, amount, transaction);

        for (int i = 0; i < this.size(); i++) {
            int extracted = this.extract(i, resource, amount, transaction);
            if (extracted == amount) return amount;
        }
        return 0;
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!this.infinity) return super.extract(index, resource, amount, transaction);

        return this.getResource(index).isEmpty() ? 0 : amount;
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        if (!this.infinity) return super.insert(resource, amount, transaction);

        for (int i = 0; i < this.size(); i++) {
            int inserted = this.insert(i, resource, amount, transaction);
            if (inserted == amount) return amount;
        }
        return 0;
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        if (!this.infinity) return super.insert(index, resource, amount, transaction);

        if (resource.isEmpty() || !this.isValid(index, resource)) return 0;

        FluidResource resourceIn = this.getResource(0);
        if (!resourceIn.isEmpty() && !resourceIn.equals(resource)) return 0;

        if (resourceIn.isEmpty()) {
            ((StacksResourceHandlerAccessor) this).getSnapshotJournals().get(index).updateSnapshots(transaction);
            this.stacks.set(index, this.getStackFrom(resource, this.getCapacity(index, resource)));
        }
        return amount;
    }

    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        output.putBoolean("Infinity", this.infinity);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        this.infinity = input.getBooleanOr("Infinity", false);
    }
}
