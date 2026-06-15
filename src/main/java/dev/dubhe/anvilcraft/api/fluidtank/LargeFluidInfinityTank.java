package dev.dubhe.anvilcraft.api.fluidtank;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.function.Predicate;

public class LargeFluidInfinityTank extends FluidTank {
    @Setter
    @Getter
    private boolean isInfinity;

    public LargeFluidInfinityTank(int capacity, Predicate<FluidStack> validator) {
        this(capacity, validator, true);
    }

    public LargeFluidInfinityTank(int capacity, Predicate<FluidStack> validator, boolean isInfinity) {
        super(capacity, validator);
        this.isInfinity = isInfinity;
    }

    public LargeFluidInfinityTank(int capacity) {
        this(capacity, true);
    }

    public LargeFluidInfinityTank(int capacity, boolean isInfinity) {
        super(capacity);
        this.isInfinity = isInfinity;
    }

    public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        super.readFromNBT(lookupProvider, nbt);
        this.isInfinity = nbt.getBoolean("Infinity");
        return this;
    }

    public CompoundTag writeToNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
        super.writeToNBT(lookupProvider, nbt);
        nbt.putBoolean("Infinity", this.isInfinity);
        return nbt;
    }

    @Override
    public FluidTank setCapacity(int capacity) {
        super.setCapacity(capacity);
        if (this.isInfinity && !this.fluid.isEmpty()) this.fluid.setAmount(this.capacity);
        return this;
    }

    @Override
    public int fill(FluidStack resource, IFluidHandler.FluidAction action) {
        if (!this.isInfinity) return super.fill(resource, action);

        if (resource.isEmpty() || !this.isFluidValid(resource)) return 0;

        if (!this.fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(this.fluid, resource)) return 0;

        if (!action.simulate() && this.fluid.isEmpty()) {
            this.fluid = resource.copyWithAmount(this.capacity);
            this.onContentsChanged();
        }
        return resource.getAmount();

    }

    @Override
    public FluidStack drain(int maxDrain, IFluidHandler.FluidAction action) {
        if (!this.isInfinity) return super.drain(maxDrain, action);

        return this.isEmpty() ? FluidStack.EMPTY : this.fluid.copyWithAmount(maxDrain);
    }

    @Override
    public int getSpace() {
        return isInfinity ? Integer.MAX_VALUE : super.getSpace();
    }
}
