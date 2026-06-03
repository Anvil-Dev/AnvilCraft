package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/// 持有FluidTank的
public interface IFluidHandlerHolder {
    ResourceHandler<FluidResource> getFluidHandler();
}
