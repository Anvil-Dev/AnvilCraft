package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * 蜂蜜瓶流体处理器包装器
 * 使蜂蜜瓶可以和储罐进行流体交互（参考水瓶行为，每瓶 250mB）
 */
public class HoneyBottleWrapper implements IFluidHandlerItem {
    private static final int HONEY_PER_BOTTLE = 250;

    protected ItemStack container;

    public HoneyBottleWrapper(ItemStack container) {
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
            return new FluidStack(ModFluids.HONEY, HONEY_PER_BOTTLE);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return HONEY_PER_BOTTLE;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return stack.getFluid() instanceof HoneyFluid;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < HONEY_PER_BOTTLE) return 0;
        if (!container.is(Items.GLASS_BOTTLE)) return 0;
        if (!(resource.getFluid() instanceof HoneyFluid)) return 0;

        if (action.execute()) {
            container = new ItemStack(Items.HONEY_BOTTLE);
        }
        return HONEY_PER_BOTTLE;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain < HONEY_PER_BOTTLE) return FluidStack.EMPTY;
        if (!container.is(Items.HONEY_BOTTLE)) return FluidStack.EMPTY;

        FluidStack result = new FluidStack(ModFluids.HONEY, HONEY_PER_BOTTLE);
        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.getAmount() < HONEY_PER_BOTTLE) return FluidStack.EMPTY;
        if (!container.is(Items.HONEY_BOTTLE)) return FluidStack.EMPTY;
        if (!(resource.getFluid() instanceof HoneyFluid)) return FluidStack.EMPTY;

        FluidStack result = new FluidStack(ModFluids.HONEY, HONEY_PER_BOTTLE);
        if (action.execute()) {
            container = new ItemStack(Items.GLASS_BOTTLE);
        }
        return result;
    }
}
