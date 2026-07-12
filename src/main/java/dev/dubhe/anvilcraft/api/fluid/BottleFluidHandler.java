package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * Fluid handler for honey bottles. Each bottle transfers 250 mB.
 */
public class BottleFluidHandler implements IFluidHandlerItem {
    private static final int FLUID_PER_BOTTLE = 250;

    protected ItemStack container;

    public BottleFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (container.is(Items.HONEY_BOTTLE)) {
            return new FluidStack(ModFluids.HONEY, FLUID_PER_BOTTLE);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return FLUID_PER_BOTTLE;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid() instanceof HoneyFluid;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < FLUID_PER_BOTTLE) return 0;
        if (!container.is(Items.GLASS_BOTTLE)) return 0;
        ItemStack filled = getFilledBottle(resource);
        if (filled.isEmpty()) return 0;

        if (action.execute()) {
            container = filled;
        }
        return FLUID_PER_BOTTLE;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain < FLUID_PER_BOTTLE) return FluidStack.EMPTY;
        FluidStack result = getFluidInTank(0);
        if (result.isEmpty()) return FluidStack.EMPTY;

        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < FLUID_PER_BOTTLE) return FluidStack.EMPTY;
        FluidStack result = getFluidInTank(0);
        if (result.isEmpty()) return FluidStack.EMPTY;
        if (!result.is(resource.getFluid())) return FluidStack.EMPTY;

        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }

    private static ItemStack getFilledBottle(FluidStack resource) {
        if (resource.getFluid() instanceof HoneyFluid) {
            return new ItemStack(Items.HONEY_BOTTLE);
        }
        return ItemStack.EMPTY;
    }
}
