package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/// 持有 FluidTank 的
public interface IFluidResourceHandlerHolder {
    ResourceHandler<FluidResource> getFluidHandler();
}
