package dev.dubhe.anvilcraft.api.fluid;

import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 持有FluidTank的
 */
public interface IFluidHandlerHolder {
    IFluidHandler getFluidHandler();
}
