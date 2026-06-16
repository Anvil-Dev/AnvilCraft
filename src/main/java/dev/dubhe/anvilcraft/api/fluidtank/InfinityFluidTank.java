package dev.dubhe.anvilcraft.api.fluidtank;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class InfinityFluidTank extends FluidTank {
    public InfinityFluidTank() {
        super(Integer.MAX_VALUE);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return resource.getAmount();
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        if (!this.isEmpty()) {
            return this.fluid.copyWithAmount(maxDrain);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        if (!this.isEmpty() && resource.is(this.fluid.getFluid())) {
            return resource.copy();
        }
        return FluidStack.EMPTY;
    }
}
