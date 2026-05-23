package dev.dubhe.anvilcraft.api.fluid;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

public class CapacityModifiableFluidHandler extends FluidStacksResourceHandler {
    public CapacityModifiableFluidHandler(int size, int capacity) {
        super(size, capacity);
    }

    public CapacityModifiableFluidHandler(NonNullList<FluidStack> stacks, int capacity) {
        super(stacks, capacity);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
