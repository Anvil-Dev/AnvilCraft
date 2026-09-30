package dev.dubhe.anvilcraft.api.fluidtank;

import dev.dubhe.anvilcraft.api.fluid.GasDisplayFillProvider;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class InfinityFluidTank extends FluidTank implements GasDisplayFillProvider {
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

    /**
     * 创造流体储罐的渲染口径恒为满罐（见 {@code CreativeFluidTankBlockEntityRenderer}），
     * 而其容量报告为 {@link Integer#MAX_VALUE}——若按"存量 / 容量"换算，管道内气体会
     * 透明到看不见。故持有该气体时显示填充率恒为 1。
     */
    @Override
    public float gasDisplayFill(FluidStack gas) {
        return !this.isEmpty() && FluidStack.isSameFluidSameComponents(this.fluid, gas) ? 1.0f : 0.0f;
    }
}
